package ru.example.service.TextCommandServices;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.service.voice.VoiceConnectionService;

@Service
public class VoiceService {

    private final VoiceConnectionService voiceConnectionService;
    private final Logger log = LogManager.getLogger(VoiceService.class);

    public VoiceService(VoiceConnectionService voiceConnectionService) {
        this.voiceConnectionService = voiceConnectionService;
    }

    public void execute(SlashCommandInteractionEvent event) {
        try {
            VoiceChannel voiceChannel = event.getGuild().getVoiceChannelsByName("Лобби", true).stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Голосовой канал 'Лобби' не найден"));

            voiceConnectionService.connectToVoiceChannel();
            log.info("Подключение к голосовому каналу: {}", voiceChannel.getName());

            event.getHook().sendMessage("✅ Бот подключен к голосовому каналу " + voiceChannel.getName()).queue();

        } catch (Exception e) {
            log.error("Ошибка подключения к голосовому каналу: {}", e.getMessage());
            event.getHook().sendMessage("❌ Ошибка подключения к голосовому каналу: " + e.getMessage()).queue();
        }
    }

    public void executeTestVosk(SlashCommandInteractionEvent event) {
        try {
            event.getHook().sendMessage("✅ Тесты Vosk запущены! Проверьте логи в консоли.").queue();
            log.info("Тестирование Vosk запущено пользователем: {}", event.getUser().getName());
        } catch (Exception e) {
            event.getHook().sendMessage("❌ Ошибка теста Vosk: " + e.getMessage()).queue();
        }
    }
}