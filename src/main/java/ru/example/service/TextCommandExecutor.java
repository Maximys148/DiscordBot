package ru.example.service;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.service.TextCommandServices.HelpHandler;
import ru.example.service.TextCommandServices.ProfileService;
import ru.example.service.TextCommandServices.UnmuteCommandHandler;
import ru.example.service.TextCommandServices.VoiceService;

@Service
public class TextCommandExecutor extends ListenerAdapter {

    private final HelpHandler helpHandler;
    private final ProfileService profileService;
    private final VoiceService voiceService;
    private final UnmuteCommandHandler unmuteCommandHandler;
    private final Logger log = LogManager.getLogger(TextCommandExecutor.class);

    public TextCommandExecutor(HelpHandler helpHandler,
                               ProfileService profileService,
                               VoiceService voiceService, UnmuteCommandHandler unmuteCommandHandler) {
        this.helpHandler = helpHandler;
        this.profileService = profileService;
        this.voiceService = voiceService;
        this.unmuteCommandHandler = unmuteCommandHandler;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            try {
                switch (event.getName()) {
                    case "help":
                        helpHandler.execute(event);
                        break;
                    case "rules":
                        helpHandler.executeRules(event);
                        break;
                    case "profile":
                        profileService.execute(event);
                        break;
                    case "join_voice":
                        voiceService.execute(event);
                        break;
                    case "unmute_user":
                        unmuteCommandHandler.execute(event);
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