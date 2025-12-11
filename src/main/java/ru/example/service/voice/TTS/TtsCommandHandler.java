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
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

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
            log.info("🔍 WAV: {}Hz {}ch {}bit", sourceFormat.getSampleRate(),
                    sourceFormat.getChannels(), sourceFormat.getSampleSizeInBits());

            // ✅ РУЧНАЯ КОНВЕРТАЦИЯ float32 → int16 → upsample → stereo
            byte[] pcm48k = manualConvertToDiscordPcm(readAllBytes(ais));

            int packetCount = pcm48k.length / 3840;
            int alignedLength = packetCount * 3840;

            log.info("✅ Ручная конвертация: {} байт ({} пакетов)",
                    alignedLength, packetCount);

            Files.write(Paths.get("/app/manual_pcm.raw"),
                    Arrays.copyOf(pcm48k, alignedLength));

            return Arrays.copyOf(pcm48k, alignedLength);

        } catch (Exception e) {
            log.error("❌ Конвертация", e);
            throw e;
        }
    }

    private byte[] manualConvertToDiscordPcm(byte[] float32Mono16k) {
        int sampleCount = float32Mono16k.length / 4;
        int targetSamples = (int)(sampleCount * 3.0); // 16k → 48k
        byte[] pcm48k = new byte[targetSamples * 4];
        int outIdx = 0;

        for (int out = 0; out < targetSamples && outIdx < pcm48k.length; out++) {
            float inPos = out / 3.0f;
            int inIdx = (int)inPos;
            float frac = inPos - inIdx;

            float sample1 = inIdx < sampleCount ?
                    ByteBuffer.wrap(float32Mono16k, inIdx*4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat() : 0;
            float sample2 = (inIdx+1) < sampleCount ?
                    ByteBuffer.wrap(float32Mono16k, (inIdx+1)*4, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat() : 0;
            float interpSample = sample1 * (1-frac) + sample2 * frac;

            // ✅ BigEndian: high byte ПЕРВЫЙ, диапазон ±32767
            short s16 = (short)(Math.max(-1f, Math.min(1f, interpSample)) * 32767);

            // Stereo L (high→low) → R (high→low)
            pcm48k[outIdx++] = (byte)((s16 >> 8) & 0xFF);  // L high (BigEndian)
            pcm48k[outIdx++] = (byte)(s16 & 0xFF);         // L low
            pcm48k[outIdx++] = (byte)((s16 >> 8) & 0xFF);  // R high
            pcm48k[outIdx++] = (byte)(s16 & 0xFF);         // R low
        }

        int alignedLength = (outIdx / 3840) * 3840;
        log.info("✅ BigEndian PCM: {} байт ({} пакетов)", alignedLength, alignedLength / 3840);
        return Arrays.copyOf(pcm48k, alignedLength);
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
