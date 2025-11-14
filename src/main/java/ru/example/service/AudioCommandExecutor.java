package ru.example.service;

import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.model.CommandInfo;
import ru.example.service.AudioCommandHandlers.MuteCommandHandler;

/**
 * Перенаправляет аудио команду в нужный сервис обработки
 */
@Service
public class AudioCommandExecutor {

    private final MuteCommandHandler muteService;
    private final Logger log = LogManager.getLogger(AudioCommandExecutor.class);

    // Добавьте другие сервисы

    public AudioCommandExecutor(MuteCommandHandler muteService) {
        this.muteService = muteService;
    }

    public void executeCommand(CommandInfo commandInfo, User issuer) {
        switch (commandInfo.getCommandId()) {
            case MUTE_USER:
                muteService.execute(commandInfo, issuer);
                break;
            default:
                log.warn("Неизвестная команда: {}", commandInfo.getCommandId());
        }
    }
}
