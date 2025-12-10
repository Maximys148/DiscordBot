package ru.example.service.voice.TTS;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ru.example.service.voice.OpusToPcmDecoder;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Service
public class TtsCommandHandler {
    private final RestTemplate restTemplate = new RestTemplate();
    private final OpusToPcmDecoder pcmDecoder;
    private final String ttsUrl = "http://tts-service:5002/api/tts";  // ✅ localhost для теста
    private static final Logger log = LogManager.getLogger(TtsCommandHandler.class);

    public TtsCommandHandler(OpusToPcmDecoder pcmDecoder) {
        this.pcmDecoder = pcmDecoder;
    }

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
                // 1. TTS → WAV (POST multipart)
                byte[] wavData = generateWavFromTts(text);
                byte[] discordPcm = convertToDiscordPcm(wavData);
                // После FFmpeg в convertToDiscordPcm():

                // Путь внутри Docker
                Path path = Paths.get("/app/debug_pcm.raw");
                Files.write(path, discordPcm);
                log.info("DEBUG PCM saved to {}", path.toAbsolutePath());

                // 2. Discord
                playPcmInDiscord(guild, discordPcm);

                // ✅ editOriginal (НЕ sendMessage!)
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

    // ✅ POST multipart (НЕ GET!)
    private byte[] generateWavFromTts(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("text", text);
        body.add("format", "wav");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response = restTemplate.postForEntity(ttsUrl, request, byte[].class);

        log.info("✅ TTS OK: {} байт", response.getBody().length);
        return response.getBody();
    }

    private void playPcmInDiscord(Guild guild, byte[] pcm) {
        AudioManager audioManager = guild.getAudioManager();

        // 🔍 ЛОГ 1: Бот в канале?
        log.info("🔊 AudioManager: connected={}, channel={}",
                audioManager.isConnected(),
                audioManager.getConnectedChannel() != null ?
                        audioManager.getConnectedChannel().getName() : "null");

        if (!audioManager.isConnected()) {
            log.error("❌ Бот НЕ в голосовом канале!");
            return;
        }

        audioManager.setSendingHandler(new AudioSendHandler() {
            private final ByteArrayInputStream stream = new ByteArrayInputStream(pcm);
            private int packetsSent = 0;

            @Override
            public boolean canProvide() {
                return stream.available() > 0;
            }

            @Override
            public ByteBuffer provide20MsAudio() {
                byte[] buffer = new byte[3840]; // 20ms 48kHz stereo 16bit
                int read;
                try {
                    read = stream.read(buffer);
                } catch (IOException e) {
                    return null;
                }

                if (read <= 0) {
                    log.info("📦 Пакеты отправлено: {}", packetsSent);
                    return null; // конец потока
                }

                packetsSent++;
                if (packetsSent % 50 == 0) {  // 🔍 ЛОГ 2: пакеты идут?
                    log.info("📦 Отправлено пакетов: {}", packetsSent);
                }

                if (read < buffer.length) {
                    Arrays.fill(buffer, read, buffer.length, (byte) 0);
                }
                return ByteBuffer.wrap(buffer);
            }

            @Override
            public boolean isOpus() {
                return false;  // 🔍 ЛОГ 3: PCM режим!
            }
        });

        log.info("✅ PCM отправлен: {} байт (~{} сек)", pcm.length, pcm.length / 384000.0);
    }


    private byte[] convertToDiscordPcm(byte[] wavData) throws Exception {
        // Сохраняем временный WAV
        Path tempWav = Files.createTempFile("tts_", ".wav");
        Files.write(tempWav, wavData);

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", tempWav.toString(),
                "-ar", "48000", "-ac", "1", "-f", "s16le", "-acodec", "pcm_s16le",
                "pipe:1"
        );


        Process process = pb.start();
        byte[] discordPcm = process.getInputStream().readAllBytes();
        process.waitFor();
        // В convertToDiscordPcm, после FFmpeg:
        Files.write(Paths.get("/tmp/debug_pcm.raw"), discordPcm);
        log.info("✅ DEBUG: /tmp/debug_pcm.raw сохранён (открой в Audacity как raw PCM 48kHz stereo 16bit little-endian)");

        Files.delete(tempWav);

        log.info("✅ FFmpeg PCM: {} байт", discordPcm.length);
        return discordPcm;
    }

}
