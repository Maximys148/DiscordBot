package ru.example.service.voice.TTS;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.example.service.voice.OpusToPcmDecoder;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

@Service
public class TtsCommandHandler {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final OpusToPcmDecoder pcmDecoder;
    private final String ttsUrl = "http://tts-service:5002/api/tts";
    private static final Logger log = LogManager.getLogger(TtsCommandHandler.class);
    
    public TtsCommandHandler(OpusToPcmDecoder pcmDecoder) {
        this.pcmDecoder = pcmDecoder;
    }
    
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        
        // Проверка: бот в голосовом канале?
        Member bot = event.getGuild().getSelfMember();
        if (!bot.getVoiceState().inAudioChannel()) {
            event.getHook().sendMessage("❌ **Бот не в голосовом канале!**\nИспользуйте `/join_voice`").queue();
            return;
        }
        
        String text = event.getOption("text").getAsString();
        String model = event.getOption("model") != null ? 
                      event.getOption("model").getAsString() : 
                      "tts_models/ru/css10/vits";
        
        try {
            // 1. TTS → WAV (от Coqui)
            byte[] wav48k = generateWavFromTts(text, model);
            
            // 2. Используем ВАШ декодер: 48kHz → 16kHz PCM (для унификации)
            byte[] pcm16k = pcmDecoder.resample48kTo16k(wav48k);
            
            // 3. PCM → Opus для Discord
            byte[] opusData = pcm16kToOpus(pcm16k);
            
            // 4. Воспроизведение
            playOpusInDiscord(event.getGuild(), opusData);
            
            event.getHook().sendMessageEmbeds(
                new EmbedBuilder()
                    .setTitle("🔊 **Mozilla TTS активирован**")
                    .setDescription("**`" + text + "`**")
                    .addField("Модель", model, true)
                    .addField("Формат", "48kHz→16kHz→Opus", true)
                    .setColor(Color.GREEN)
                    .build()
            ).queue();
            
        } catch (Exception e) {
            log.error("TTS ошибка", e);
            event.getHook().sendMessage("❌ **Ошибка:** " + e.getMessage()).queue();
        }
    }
    
    private byte[] generateWavFromTts(String text, String model) throws IOException {
        String url = ttsUrl + "?text=" + URLEncoder.encode(text, "UTF-8")
                   + "&model_name=" + model + "&format=wav";
        
        log.info("TTS запрос: {}", url);
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        return response.getBody();
    }
    
    private byte[] pcm16kToOpus(byte[] pcm16k) throws IOException, InterruptedException {
        // ffmpeg: PCM 16kHz mono → Opus 48kHz stereo (Discord формат)
        ProcessBuilder pb = new ProcessBuilder(
            "ffmpeg", "-y",
            "-f", "s16le",           // PCM signed 16-bit little endian
            "-ar", "16000",          // 16kHz
            "-ac", "1",              // mono
            "-i", "-", 
            "-ar", "48000",          // Discord: 48kHz
            "-ac", "2",              // stereo
            "-c:a", "libopus",
            "-b:a", "64k",
            "-f", "opus", "-"
        );
        
        Process process = pb.redirectErrorStream(true).start();
        try (OutputStream os = process.getOutputStream()) {
            os.write(pcm16k);
        }
        
        byte[] opusData = process.getInputStream().readAllBytes();
        process.waitFor(10, TimeUnit.SECONDS);
        
        log.info("PCM16k({}) → Opus({})", pcm16k.length, opusData.length);
        return opusData;
    }
    
    private void playOpusInDiscord(Guild guild, byte[] opusData) {
        AudioManager audioManager = guild.getAudioManager();
        audioManager.setSendingHandler(new OpusPacketSendHandler(opusData));
    }
}
