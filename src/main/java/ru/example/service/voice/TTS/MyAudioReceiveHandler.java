package ru.example.service.voice.TTS;

import lombok.Setter;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Принимает аудио данные из голосового канала и отправляет их в STT(Speech-To-Text) сервис
 */
@Service
public class MyAudioReceiveHandler implements AudioReceiveHandler {

    private final SttService sttService;
    private final ConcurrentHashMap<Long, AudioBuffer> userBuffers;
    private final ScheduledExecutorService scheduler;
    private final Logger log = LogManager.getLogger(MyAudioReceiveHandler.class);

    // Порог тишины в миллисекундах
    private static final int SILENCE_TIMEOUT_MS = 1500;
    // Минимальная длина аудио для обработки (в байтах)
    private static final int MIN_AUDIO_LENGTH = 3200; // ~100ms при 16kHz, 16-bit mono

    public MyAudioReceiveHandler(SttService sttService) {
        this.sttService = sttService;
        this.userBuffers = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }

    @Override
    public boolean canReceiveUser() {
        return true;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        User user = userAudio.getUser();
        byte[] audioData = userAudio.getAudioData(1.0);

        // Игнорируем пустые данные
        if (audioData.length == 0) {
            return;
        }

        Long userId = user.getIdLong();
        AudioBuffer buffer = userBuffers.computeIfAbsent(userId,
                k -> new AudioBuffer());

        boolean hasVoice = isVoicePresent(audioData);

        synchronized (buffer) {
            if (hasVoice) {
                // Добавляем аудио в буфер и сбрасываем таймер тишины
                buffer.addAudio(audioData);
                buffer.setHasVoice(true);
                buffer.resetSilenceTimer();

                // Отменяем предыдущую запланированную отправку
                if (buffer.scheduledTask != null) {
                    buffer.scheduledTask.cancel(false);
                }
            } else {
                // Тишина обнаружена
                if (buffer.hasVoice()) {
                    // Если до этого был голос, планируем отправку после таймаута тишины
                    if (buffer.scheduledTask == null || buffer.scheduledTask.isCancelled()) {
                        buffer.scheduledTask = scheduler.schedule(() -> {
                            processBufferedAudio(user, buffer);
                        }, SILENCE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    private boolean isVoicePresent(byte[] audioData) {
        long sum = 0;
        for (byte b : audioData) {
            sum += Math.abs(b);
        }
        double avg = sum / (double) audioData.length;
        double threshold = 5;
        return avg > threshold;
    }

    private void processBufferedAudio(User user, AudioBuffer buffer) {
        synchronized (buffer) {
            byte[] audioData = buffer.getAndClear();
            buffer.setHasVoice(false);
            buffer.scheduledTask = null;

            if (audioData.length >= MIN_AUDIO_LENGTH) {
                processAudio(user, audioData);
            } else {
                log.debug("Аудио слишком короткое для обработки: {} байт", audioData.length);
            }
        }
    }

    private void processAudio(User user, byte[] audioData) {
        sttService.recognizeDiscordAudio(audioData, user)
                .thenAccept(transcript -> {
                    if (transcript != null && !transcript.trim().isEmpty()) {

                        // Очищаем буфер пользователя после успешной обработки
                        userBuffers.remove(user.getIdLong());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Ошибка обработки аудио для пользователя {}: {}",
                            user.getGlobalName(), throwable.getMessage());
                    return null;
                });
    }

    // Добавляем метод для принудительной отправки всех буферов
    public void flushAllBuffers() {
        userBuffers.forEach((userId, buffer) -> {
            synchronized (buffer) {
                byte[] audioData = buffer.getAndClear();
                if (audioData.length >= MIN_AUDIO_LENGTH) {
                    // Можно создать временного пользователя или обработать анонимно
                    log.debug("Принудительно отправлен буфер пользователя {}: {} байт",
                            userId, audioData.length);
                }
            }
        });
        userBuffers.clear();
    }

    private class AudioBuffer {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private long lastVoiceTimestamp = System.currentTimeMillis();
        @Setter
        private boolean hasVoice = false;
        private java.util.concurrent.ScheduledFuture<?> scheduledTask;

        public void addAudio(byte[] audioData) {
            try {
                buffer.write(audioData);
            } catch (Exception e) {
                log.error("Ошибка записи в буфер: {}", e.getMessage());
            }
        }

        public byte[] getAndClear() {
            byte[] data = buffer.toByteArray();
            buffer.reset();
            return data;
        }

        public void resetSilenceTimer() {
            lastVoiceTimestamp = System.currentTimeMillis();
        }

        public boolean silenceExceeded(int timeoutMs) {
            return System.currentTimeMillis() - lastVoiceTimestamp > timeoutMs;
        }

        public boolean hasVoice() {
            return hasVoice;
        }

    }

    // Очистка ресурсов
    public void cleanup() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        userBuffers.clear();
    }
}