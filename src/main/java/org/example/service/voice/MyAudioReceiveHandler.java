package org.example.service.voice;

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

@Service
public class MyAudioReceiveHandler implements AudioReceiveHandler {

    private final VoskService voskService;
    private final ConcurrentHashMap<Long, AudioBuffer> userBuffers;
    private final ScheduledExecutorService scheduler;
    private final Logger log = LogManager.getLogger(MyAudioReceiveHandler.class);

    private static final int BUFFER_TIME_MS = 2000;

    public MyAudioReceiveHandler(VoskService voskService) {
        this.voskService = voskService;
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
        if (audioData == null || audioData.length == 0) {
            return;
        }

        Long userId = user.getIdLong();
        AudioBuffer buffer = userBuffers.computeIfAbsent(userId,
                k -> new AudioBuffer());
        buffer.addAudio(audioData);

        // Если буфер еще не запланирован на отправку
        if (!buffer.isScheduled()) {
            buffer.setScheduled(true);

            scheduler.schedule(() -> {
                byte[] accumulatedAudio = buffer.getAndClear();
                if (accumulatedAudio.length > 19200) { // Минимум 0.5 секунды аудио
                    processAudio(user, accumulatedAudio);
                }
                buffer.setScheduled(false);
            }, BUFFER_TIME_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void processAudio(User user, byte[] audioData) {
        voskService.recognizeDiscordAudio(audioData, user)
                .thenAccept(transcript -> {
                    if (transcript != null && !transcript.trim().isEmpty()) {
                        log.info("💬 " + user.getGlobalName() + ": " + transcript);
                    }
                });
    }

    private static class AudioBuffer {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean scheduled = false;

        public synchronized void addAudio(byte[] audioData) {
            try {
                buffer.write(audioData);
            } catch (Exception e) {
                // ignore
            }
        }

        public synchronized byte[] getAndClear() {
            byte[] data = buffer.toByteArray();
            buffer.reset();
            return data;
        }

        public synchronized boolean isScheduled() {
            return scheduled;
        }

        public synchronized void setScheduled(boolean scheduled) {
            this.scheduled = scheduled;
        }
    }
}