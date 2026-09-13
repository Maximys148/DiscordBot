package ru.example.service.voice.TTS;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Service
public class TtsCommandHandler {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LogManager.getLogger(TtsCommandHandler.class);

    @Value("${app.tts.fast.url:http://tts-service:5000/synthesize}")
    private String ttsFastUrl;

    @Value("${app.tts.natural.url:http://vits-service:5001/synthesize}")
    private String ttsNaturalUrl;

    @Value("${app.tts.default-mode:fast}")
    private String defaultMode;

    public void execute(SlashCommandInteractionEvent event, InteractionHook hook) {
        Guild guild = event.getGuild();
        if (!guild.getAudioManager().isConnected()) {
            event.reply("❌ Бот не в голосовом канале! `/join_voice`").setEphemeral(true).queue();
            return;
        }

        String text = event.getOption("text").getAsString();
        log.info("🗣️ TTS: {}", text);

        event.deferReply().queue();  // 1. Отложить ответ

        CompletableFuture.runAsync(() -> {
            try {
                String model = event.getOption("model") != null ?
                        event.getOption("model").getAsString() : defaultMode;

                String ttsUrl = "silero".equals(model) || "VITS".equals(model) ? ttsFastUrl : ttsNaturalUrl;
                log.info("🎤 TTS режим: '{}' → {}", model, ttsUrl);

                byte[] discordPcm = generateDiscordPcmFromTts(text, ttsUrl);
                playPcmInDiscord(guild, discordPcm);

                // ✅ Теперь event.getHook() работает!
                event.getHook().editOriginalEmbeds(  // ← Было event.getHook()
                        new EmbedBuilder()
                                .setTitle("🗣️ TTS выполнен")
                                .setDescription("**" + text + "**")
                                .addField("🎵 Модель", model.toUpperCase(), true)
                                .setColor(Color.GREEN)
                                .build()
                ).queue();

            } catch (Exception e) {
                log.error("❌ TTS failed", e);
                event.getHook().editOriginalEmbeds(  // ← Работает после deferReply()
                        new EmbedBuilder()
                                .setTitle("❌ TTS ошибка")
                                .setDescription(e.getMessage())
                                .setColor(Color.RED)
                                .build()
                ).queue();
            }
        });
    }


    /** ✅ Генерация PCM с динамическим URL модели */
    private byte[] generateDiscordPcmFromTts(String text, String ttsUrl) {
        String jsonRequest = String.format("{\"text\": \"%s\", \"speaker\": \"xenia\"}",  // xenia по умолчанию
                text.replace("\"", "\\\""));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                ttsUrl, HttpMethod.POST, request, byte[].class
        );

        byte[] pcm = response.getBody();
        log.info("✅ TTS PCM: {} байт ({} пакетов)", pcm.length, pcm.length / 3840);

        return alignToPackets(pcm);
    }

    private byte[] alignToPackets(byte[] pcm) {
        int packetCount = pcm.length / 3840;
        int alignedLength = packetCount * 3840;
        log.info("📦 Выровнено: {} байт ({} пакетов)", alignedLength, packetCount);
        return Arrays.copyOf(pcm, alignedLength);
    }

    private void playPcmInDiscord(Guild guild, byte[] audioData) {
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(new FixedPcmHandler(audioData));
    }

    // ✅ PCM handler без изменений
    private static class FixedPcmHandler implements AudioSendHandler {
        private final byte[] pcmData;
        private int position = 0;
        private final int PACKET_SIZE = 3840;

        public FixedPcmHandler(byte[] pcmData) {
            this.pcmData = pcmData;
            log.info("🎵 PCM Handler: {} байт, {} пакетов", pcmData.length, pcmData.length / PACKET_SIZE);
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

            if (bytesToCopy < PACKET_SIZE) {
                Arrays.fill(packet, bytesToCopy, PACKET_SIZE, (byte) 0);
            }

            position += PACKET_SIZE;
            return ByteBuffer.wrap(packet);
        }

        @Override
        public boolean isOpus() {
            return false;
        }
    }
}
