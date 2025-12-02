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
                Commands.slash("join_voice", "Подключить бота к голосовому каналу"),
                Commands.slash("unmute_user", "Включить пользователю микрофон").addOption(OptionType.USER, "user", "Пользователь"),

                // Информационные команды
                Commands.slash("profile", "Посмотреть свой профиль"),
                Commands.slash("server_info", "Информация о сервере"),
                Commands.slash("admin", "Административные команды")
                        .addSubcommands(new SubcommandData("ban", "Забанить пользователя")
                                .addOption(OptionType.USER, "user", "Пользователь", true)
                                .addOption(OptionType.STRING, "reason", "Причина", false))
        );
    }
}