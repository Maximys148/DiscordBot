package ru.example.service.CommandsExecutor;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.service.TextCommandServices.*;
import ru.example.service.voice.TTS.TtsCommandHandler;

@Service
public class TextCommandExecutor extends ListenerAdapter {

    private final HelpHandler helpHandler;
    private final ProfileHandler profileHandler;
    private final VoiceHandler voiceHandler;
    private final MuteCommandHandler muteCommandHandler;
    private final UnmuteCommandHandler unmuteCommandHandler;
    private final TtsCommandHandler ttsCommandHandler;
    private final Logger log = LogManager.getLogger(TextCommandExecutor.class);

    public TextCommandExecutor(HelpHandler helpHandler,
                               ProfileHandler profileHandler,
                               VoiceHandler voiceHandler,
                               MuteCommandHandler muteCommandHandler,
                               UnmuteCommandHandler unmuteCommandHandler,
                               TtsCommandHandler ttsCommandHandler) {
        this.helpHandler = helpHandler;
        this.profileHandler = profileHandler;
        this.voiceHandler = voiceHandler;
        this.muteCommandHandler = muteCommandHandler;
        this.unmuteCommandHandler = unmuteCommandHandler;
        this.ttsCommandHandler = ttsCommandHandler;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue(hook -> {
            try {
                switch (event.getName()) {
                    case "help":
                        helpHandler.execute(event);
                        break;
                    case "rules":
                        helpHandler.executeRules(event);
                        break;
                    case "profile":
                        profileHandler.execute(event);
                        break;
                    case "join_voice":
                        voiceHandler.execute(event);
                        break;
                    case "test_tts":
                        ttsCommandHandler.execute(event, hook);  // TTS сам сделает deferReply
                        break;
                    case "mute_user":
                        muteCommandHandler.execute(event);
                        break;
                    case "unmute_user":
                        unmuteCommandHandler.execute(event);
                        break;
                    default:
                        event.reply("Неизвестная команда").setEphemeral(true).queue();
                        break;
                }
            } catch (Exception e) {
                log.error("Ошибка выполнения команды {}: {}", event.getName(), e.getMessage());
                event.reply("Ошибка выполнения команды").setEphemeral(true).queue();
            }
        });
    }
}