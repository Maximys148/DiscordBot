package ru.example.service.CommandsExecutor;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.example.service.TextCommandServices.*;

@Service
public class TextCommandExecutor extends ListenerAdapter {

    private final HelpHandler helpHandler;
    private final ProfileService profileService;
    private final VoiceService voiceService;
    private final MuteCommandHandler muteCommandHandler;
    private final UnmuteCommandHandler unmuteCommandHandler;
    private final Logger log = LogManager.getLogger(TextCommandExecutor.class);

    public TextCommandExecutor(HelpHandler helpHandler,
                               ProfileService profileService,
                               VoiceService voiceService, @Qualifier("textMuteCommandHandler") MuteCommandHandler muteCommandHandler, UnmuteCommandHandler unmuteCommandHandler) {
        this.helpHandler = helpHandler;
        this.profileService = profileService;
        this.voiceService = voiceService;
        this.muteCommandHandler = muteCommandHandler;
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
                    case "mute_user":
                        muteCommandHandler.execute(event);
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