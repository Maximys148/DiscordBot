package ru.example.service.TextCommandServices;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;
import ru.example.service.CommandFormatter;

@Service
public class MenuService {

    private final CommandFormatter commandFormatter;

    public MenuService(CommandFormatter commandFormatter) {
        this.commandFormatter = commandFormatter;
    }

    public void execute(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        **📋 ГЛАВНОЕ МЕНЮ**
                        
                        **📜 Информация:**
                        %s - Правила сервера
                        %s - Информация о сервере
                        %s - Ваш профиль
                        
                        **⚙️ Утилиты:**
                        %s - Настройки бота
                        %s - Техническая поддержка
                        %s - Очистка сообщений
                        
                        **❓ Помощь:**
                        %s - Все команды
                        
                        *Выберите нужный раздел!*
                        """.formatted(
                        commandFormatter.createCommandLink("rules", "📜 Правила", guildId),
                        commandFormatter.createCommandLink("server_info", "🏰 Сервер", guildId),
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId),
                        commandFormatter.createCommandLink("settings", "⚙️ Настройки", guildId),
                        commandFormatter.createCommandLink("support", "🆘 Поддержка", guildId),
                        commandFormatter.createCommandLink("clear", "🗑️ Очистка", guildId),
                        commandFormatter.createCommandLink("help", "❓ Помощь", guildId)
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }
}