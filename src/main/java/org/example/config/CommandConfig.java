package org.example.config;

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
                Commands.slash("menu", "Главное меню бота"),
                Commands.slash("rules", "Показать правила сервера"),
                Commands.slash("join_voice", "Подключить бота к голосовому каналу"),
                Commands.slash("test_vosk", "Протестировать работу Vosk"),

                // Информационные команды
                Commands.slash("profile", "Посмотреть свой профиль"),
                Commands.slash("server_jinfo", "Информация о сервере"),
                Commands.slash("userinfo", "Информация о пользователе")
                        .addOption(OptionType.USER, "user", "Пользователь", false),

                // Утилиты
                Commands.slash("settings", "Настройки бота"),
                Commands.slash("support", "Техническая поддержка"),
                Commands.slash("feedback", "Оставить отзыв")
                        .addOption(OptionType.STRING, "message", "Ваш отзыв", true),

                // Модерация
                Commands.slash("clear", "Очистить сообщения")
                        .addOption(OptionType.INTEGER, "amount", "Количество сообщений", true)
        );
    }

    @Bean
    public List<SlashCommandData> adminCommands() {
        return List.of(
                Commands.slash("admin", "Административные команды")
                        .addSubcommands(new SubcommandData("ban", "Забанить пользователя")
                                .addOption(OptionType.USER, "user", "Пользователь", true)
                                .addOption(OptionType.STRING, "reason", "Причина", false))
        );
    }
}