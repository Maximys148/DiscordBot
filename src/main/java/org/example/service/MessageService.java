package org.example.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MessageService extends ListenerAdapter {

    private final CommandFormatter commandFormatter;
    private final Logger log = LogManager.getLogger(MessageService.class);

    public MessageService(CommandFormatter commandFormatter) {
        this.commandFormatter = commandFormatter;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw().toLowerCase();
        long guildId = event.getGuild().getIdLong();

        switch (message) {
            case "!команды":
            case "!commands":
                sendCommandList(event, guildId);
                break;

            case "!правила":
            case "!rules":
                sendRulesInfo(event, guildId);
                break;

            case "!помощь":
            case "!help":
                sendHelpInfo(event, guildId);
                break;

            case "!меню":
            case "!menu":
                sendMainMenu(event, guildId);
                break;
        }
    }

    private void sendCommandList(MessageReceivedEvent event, long guildId) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("**🔗 Кликабельные команды:**\n" +
                        commandFormatter.createCommandLink("help", "📋 Помощь", guildId) + " - Список команд\n" +
                        commandFormatter.createCommandLink("rules", "📜 Правила", guildId) + " - Правила сервера\n" +
                        commandFormatter.createCommandLink("menu", "📋 Меню", guildId) + " - Главное меню\n" +
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId) + " - Ваш профиль\n" +
                        commandFormatter.createCommandLink("serverinfo", "🏰 Сервер", guildId) + " - Инфо о сервере\n\n" +
                        "*Просто нажмите на команду для использования!*")
                .build();

        event.getChannel().sendMessage(message).queue();
    }

    private void sendRulesInfo(MessageReceivedEvent event, long guildId) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("**📜 Правила сервера**\n\n" +
                        "1. Уважайте других участников\n" +
                        "2. Не спамьте\n" +
                        "3. Соблюдайте тематику каналов\n\n" +
                        "**Быстрые команды:**\n" +
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId) + "\n" +
                        commandFormatter.createCommandLink("serverinfo", "🏰 Сервер", guildId) + "\n" +
                        commandFormatter.createCommandLink("help", "❓ Помощь", guildId))
                .build();

        event.getChannel().sendMessage(message).queue();
    }

    private void sendHelpInfo(MessageReceivedEvent event, long guildId) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("**❓ Нужна помощь?**\n\n" +
                        "Используйте " + commandFormatter.createCommandLink("menu", "главное меню", guildId) +
                        " для навигации или " + commandFormatter.createCommandLink("support", "техподдержку", guildId) +
                        " для помощи\n\n" +
                        "**Основные команды:**\n" +
                        commandFormatter.createCommandLink("rules", "📜 Правила", guildId) + "\n" +
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId) + "\n" +
                        commandFormatter.createCommandLink("serverinfo", "🏰 Сервер", guildId))
                .build();

        event.getChannel().sendMessage(message).queue();
    }

    private void sendMainMenu(MessageReceivedEvent event, long guildId) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("**📋 Главное меню**\n\n" +
                        commandFormatter.createCommandLink("help", "❓ Помощь", guildId) + " - Все команды\n" +
                        commandFormatter.createCommandLink("rules", "📜 Правила", guildId) + " - Правила сервера\n" +
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId) + " - Ваш профиль\n" +
                        commandFormatter.createCommandLink("serverinfo", "🏰 Сервер", guildId) + " - Инфо о сервере\n" +
                        commandFormatter.createCommandLink("settings", "⚙️ Настройки", guildId) + " - Настройки бота\n" +
                        commandFormatter.createCommandLink("support", "🆘 Поддержка", guildId) + " - Техподдержка")
                .build();

        event.getChannel().sendMessage(message).queue();
    }
}