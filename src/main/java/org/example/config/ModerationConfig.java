package org.example.config;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class ModerationConfig {
    private List<String> bannedWords = new ArrayList<>();
    private List<String> allowedDomains = new ArrayList<>();
    private int maxWarnings = 3;
    private int muteDurationMinutes = 60;
    private int autoMuteThreshold = 3; // Количество нарушений для авто-мута
    private String logChannelId;
    private String muteRoleId;
    private boolean deleteMessages = true;
    private int capsLockThreshold = 70; // % заглавных букв
    private int mentionLimit = 5; // Максимум упоминаний
    private int messageLengthLimit = 1000; // Максимальная длина сообщения
    private int spamMessageCount = 5; // Сообщений для спама
    private long spamTimeWindow = 5000; // 5 секунд для спама

}