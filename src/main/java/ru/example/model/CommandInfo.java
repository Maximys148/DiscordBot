// CommandInfo.java
package ru.example.model;

import lombok.Data;
import ru.example.enums.VoiceCommandType;

/**
 * Информация о команде к боту полученная из аудио
 */
@Data
public class CommandInfo {

    private VoiceCommandType commandId;
    private String targetUser;
    private int volumeChange;
}
