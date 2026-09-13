package ru.example.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoiceCommandType {
        BAN_USER,
        MUTE_USER,
        DECREASE_VOLUME,
        INCREASE_VOLUME,
        UNMUTE_USER,
        JOIN_VOICE_CHANNEL,
        RESTART_BOT,
        SHOW_COMMANDS;
}
