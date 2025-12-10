package ru.example.service.voice.TTS;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TtsCommandHandler {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String ttsUrl = "http://tts-service:5000/synthesize";
    private static final Logger log = LogManager.getLogger(TtsCommandHandler.class);

    public void execute(SlashCommandInteractionEvent event, InteractionHook hook) {
        Guild guild = event.getGuild();
        if (!guild.getAudioManager().isConnected()) {
            event.getHook().sendMessage("❌ Бот не в голосовом канале! `/join_voice`").queue();
            return;
        }

        String text = event.getOption("text").getAsString();
        log.info("🗣️ TTS: {}", text);

        CompletableFuture.runAsync(() -> {
            try {
                // 1. TTS → Discord PCM напрямую
                byte[] discordPcm = generateDiscordPcmFromTts(text);

                log.info("✅ Готово PCM: {} байт ({} пакетов)",
                        discordPcm.length, discordPcm.length / 3840);

                // 2. Discord воспроизведение
                playPcmInDiscord(guild, discordPcm);

                // Результат
                hook.editOriginalEmbeds(
                        new EmbedBuilder()
                                .setTitle("🗣️ TTS выполнен")
                                .setDescription("**" + text + "**")
                                .setColor(Color.GREEN)
                                .build()
                ).queue();

            } catch (Exception e) {
                log.error("❌ TTS failed", e);
                event.getHook().editOriginal("❌ **TTS:** " + e.getMessage()).queue();
            }
        });
    }

    private byte[] generateDiscordPcmFromTts(String text) throws Exception {
        byte[] wavData = generateWavFromTts(text);
        return convertWavToDiscordPcm(wavData);
    }

    private byte[] generateWavFromTts(String text) {
        String jsonRequest = String.format("{\"text\": \"%s\", \"speaker_id\": 0}",
                text.replace("\"", "\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                ttsUrl, HttpMethod.POST, request, byte[].class
        );

        log.info("✅ TTS WAV: {} байт", response.getBody().length);
        return response.getBody();
    }

    private void playPcmInDiscord(Guild guild, byte[] audioData) {
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(new FixedPcmHandler(audioData));
    }

    // ✅ ИСПРАВЛЕННЫЙ: Прямой PCM handler БЕЗ JavaSound конвертации
    private static class FixedPcmHandler implements AudioSendHandler {
        private final byte[] pcmData;
        private int position = 0;
        private final int PACKET_SIZE = 3840; // 20ms = 48kHz * 16bit * 2ch * 0.02s

        public FixedPcmHandler(byte[] pcmData) {
            this.pcmData = pcmData;
            log.info("🎵 PCM Handler: {} байт, {} пакетов",
                    pcmData.length, pcmData.length / PACKET_SIZE);
        }

        @Override
        public boolean canProvide() {
            return position < pcmData.length;
        }

        @Override
        public ByteBuffer provide20MsAudio() {
            if (position >= pcmData.length) return null;

            int remaining = pcmData.length - position;
            int bytesToCopy = Math.min(PACKET_SIZE, remaining);

            byte[] packet = new byte[PACKET_SIZE];
            System.arraycopy(pcmData, position, packet, 0, bytesToCopy);

            // Остаток нулями (нормально для конца аудио)
            if (bytesToCopy < PACKET_SIZE) {
                Arrays.fill(packet, bytesToCopy, PACKET_SIZE, (byte) 0);
            }

            position += PACKET_SIZE;
            return ByteBuffer.wrap(packet);
        }

        @Override
        public boolean isOpus() {
            return false; // ЧИСТЫЙ PCM
        }
    }

    private byte[] convertWavToDiscordPcm(byte[] wavData) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(wavData);
             AudioInputStream ais = AudioSystem.getAudioInputStream(bais)) {

            AudioFormat sourceFormat = ais.getFormat();
            log.info("🔍 WAV: {}Hz {}ch {}bit",
                    sourceFormat.getSampleRate(),
                    sourceFormat.getChannels(),
                    sourceFormat.getSampleSizeInBits());

            // ✅ ПРЯМАЯ конвертация в Discord PCM БЕЗ промежуточных шагов!
            AudioFormat discordFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,  // S16LE
                    48000.0f,       // 48kHz
                    16,             // 16bit
                    2,              // Stereo
                    4,              // 4 bytes per frame
                    48000.0f,       // frame rate
                    false           // little-endian
            );

            AudioInputStream discordStream = AudioSystem.getAudioInputStream(
                    discordFormat, ais
            );

            byte[] pcmData = readAllBytes(discordStream);

            // ✅ Выравниваем под пакеты Discord
            int packetCount = pcmData.length / 3840;
            int alignedLength = packetCount * 3840;

            log.info("✅ Конвертация завершена: {} -> {} байт ({} пакетов)",
                    pcmData.length, alignedLength, packetCount);

            Files.write(Paths.get("/app/discord_pcm.raw"),
                    Arrays.copyOf(pcmData, alignedLength));

            return Arrays.copyOf(pcmData, alignedLength);

        } catch (Exception e) {
            log.error("❌ Конвертация сломалась", e);
            throw e;
        }
    }

    private byte[] readAllBytes(AudioInputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}
