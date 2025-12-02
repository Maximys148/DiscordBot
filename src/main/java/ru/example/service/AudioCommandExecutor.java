package ru.example.service;

import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.model.CommandInfo;
import ru.example.service.AudioCommandHandlers.MuteCommandHandler;
import ru.example.service.TextCommandServices.UnmuteCommandHandler;

/**
 * Перенаправляет аудио команду в нужный сервис обработки
 */
@Service
public class AudioCommandExecutor {
    private final MuteCommandHandler muteService;
    private final UnmuteCommandHandler unmuteService;
    private final Logger log = LogManager.getLogger(AudioCommandExecutor.class);

    // Добавьте другие сервисы

    public AudioCommandExecutor(MuteCommandHandler muteService, UnmuteCommandHandler unmuteService) {
        this.muteService = muteService;
        this.unmuteService = unmuteService;
    }

    public void executeCommand(CommandInfo commandInfo, User issuer) {
        switch (commandInfo.getCommandId()) {
            case MUTE_USER:
                muteService.execute(commandInfo, issuer);
                break;
            case UNMUTE_USER:
                // unmuteService.execute(commandInfo, issuer);
            default:
                log.warn("Неизвестная команда: {}", commandInfo.getCommandId());
        }
    }
}
