package ru.example.config;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommandConfig {

    @Bean
    public List<SlashCommandData> globalCommands() {
        return List.of(
            // Основные команды
            Commands.slash("help", "Показать список всех команд"),
            Commands.slash("rules", "Показать правила сервера"),
            Commands.slash("profile", "Посмотреть свой профиль"),
            Commands.slash("join_voice", "Подключить бота к голосовому каналу"),
            Commands.slash("test_tts", "Тестирование озвучки голосом")
                    .addOption(OptionType.STRING, "text", "Текст для озвучки", true)
                    .addOption(OptionType.STRING, "model", "tts_models/ru/css10/vits | tts_models/en/ljspeech/vits", false),
            Commands.slash("mute_user", "Выключить микрофон у пользователя")
                .addOption(OptionType.USER, "user", "Пользователь"),
            Commands.slash("unmute_user", "Включить пользователю микрофон")
                .addOption(OptionType.USER, "user", "Пользователь")
        );
    }
}