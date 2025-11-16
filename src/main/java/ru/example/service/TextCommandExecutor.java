package ru.example.service;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.service.TextCommandServices.HelpService;
import ru.example.service.TextCommandServices.ProfileService;
import ru.example.service.TextCommandServices.VoiceService;

@Service
public class TextCommandExecutor extends ListenerAdapter {

    private final HelpService helpService;
    private final ProfileService profileService;
    private final VoiceService voiceService;
    private final Logger log = LogManager.getLogger(TextCommandExecutor.class);

    public TextCommandExecutor(HelpService helpService,
                               ProfileService profileService,
                               VoiceService voiceService) {
        this.helpService = helpService;
        this.profileService = profileService;
        this.voiceService = voiceService;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            try {
                switch (event.getName()) {
                    case "help":
                        helpService.execute(event);
                        break;
                    case "rules":
                        helpService.executeRules(event);
                        break;
                    case "profile":
                        profileService.execute(event);
                        break;
                    case "join_voice":
                        voiceService.execute(event);
                        break;
                    default:
                        event.getHook().sendMessage("Неизвестная команда").queue();
                        break;
                }
            } catch (Exception e) {
                log.error("Ошибка выполнения команды {}: {}", event.getName(), e.getMessage());
                event.getHook().sendMessage("Ошибка выполнения команды").queue();
            }
        }, error -> {
            log.error("Ошибка deferReply: {}", error.getMessage());
        });
    }
}