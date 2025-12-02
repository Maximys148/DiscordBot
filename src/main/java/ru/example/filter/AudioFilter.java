// AudioFilter.java
package ru.example.filter;

import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.example.component.CommandParser;
import ru.example.enums.VoiceCommandType;
import ru.example.model.CommandInfo;
import ru.example.service.AudioCommandExecutor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Фильтрует аудиоДанные на наличие обращений(команд) к боту
 */
@Service
public class AudioFilter {

    private final String botName;
    private final CommandParser parser;
    private final AudioCommandExecutor executor;
    private final Map<List<String>, VoiceCommandType> audioCommands;
    private final Logger log = LogManager.getLogger(AudioFilter.class);

    public AudioFilter(String botName,
                       CommandParser parser, AudioCommandExecutor executor,
                       @Qualifier("audioCommands") Map<List<String>, VoiceCommandType> audioCommands) {
        this.botName = botName;
        this.parser = parser;
        this.executor = executor;
        this.audioCommands = audioCommands;
    }

    public void checkAudio(String text, User user) {
        if (!text.toLowerCase().contains(botName.toLowerCase())) {
            // Обращение к боту отсутствует
            return;
        }

        log.info("Обрабатываю обращение - {}", text);

        Optional<VoiceCommandType> commandIdOpt = findCommandInText(text);
        if (commandIdOpt.isEmpty()) {
            log.info("В обращении - {}, команда не найдена", text);
            return;
        }

        VoiceCommandType commandId = commandIdOpt.get();
        log.info("В обращении - {}, найдена команда {}", text, commandId);
        CommandInfo commandInfo = parser.parse(text, commandId);
        executor.executeCommand(commandInfo, user);
    }

    private Optional<VoiceCommandType> findCommandInText(String text) {
        String normalizedText = text.toLowerCase();

        return audioCommands.entrySet().stream()
                .filter(entry -> entry.getKey().stream().anyMatch(normalizedText::contains))
                .map(Map.Entry::getValue)
                .findFirst();
    }

}
