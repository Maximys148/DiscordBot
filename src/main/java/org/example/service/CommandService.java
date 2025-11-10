package org.example.service;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.enums.AccessLevel;
import org.example.service.voice.VoiceConnectionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.example.constant.MessageConstant.HELP_MESSAGE;
import static org.example.constant.MessageConstant.RULES_MESSAGE;


@Service
public class CommandService extends ListenerAdapter {

    private final CommandFormatter commandFormatter;
    private final VoiceConnectionService voiceConnectionService;
    private final Guild guild;
    private final Logger log = LogManager.getLogger(CommandService.class);

    public CommandService(CommandFormatter commandFormatter, VoiceConnectionService voiceConnectionService, Guild guild) {
        this.commandFormatter = commandFormatter;
        this.voiceConnectionService = voiceConnectionService;
        this.guild = guild;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            try {
                long guildId = event.getGuild().getIdLong();

                switch (event.getName()) {
                    case "help":
                        handleHelpCommand(event);
                        break;
                    case "rules":
                        handleRulesCommand(event);
                        break;
                    case "profile":
                        handleProfileCommand(event);
                        break;
                    case "server_info":
                        handleServerInfoCommand(event);
                        break;
                    case "menu":
                        handleMenuCommand(event, guildId);
                        break;
                    case "support":
                        handleSupportCommand(event);
                        break;
                    case "settings":
                        handleSettingsCommand(event);
                        break;
                    case "join_voice":
                        handleVoiceCommand(event);
                        break;
                    case "test_vosk":
                        handleTestVoskCommand(event);
                        break;
                }
            } catch (Exception e) {
                log.error("Ошибка обработки команды: {}", e.getMessage());
                event.getHook().sendMessage("Ошибка обработки команды").queue();
            }
        }, error -> {
            log.error("Ошибка deferReply: {}", error.getMessage());
        });
    }

    private void handleHelpCommand(SlashCommandInteractionEvent event) {
        String username = event.getUser().getAsMention();
        AccessLevel userLevel = getUserAccessLevel(event.getUser());

        String message = HELP_MESSAGE.formatted(
                username,
                userLevel.getDisplayName(),
                getAvailableCommandsForLevel(userLevel)
        );

        event.getHook().sendMessage(message).queue();
    }

    private AccessLevel getUserAccessLevel(User user) {
        Member member = guild.getMember(user);
        if (member == null) return AccessLevel.NEWBIE;

        if (member.getRoles().stream().anyMatch(role -> role.getName().equals("Администратор"))) {
            return AccessLevel.ADMIN;
        } else if (member.getRoles().stream().anyMatch(role -> role.getName().equals("Модератор"))) {
            return AccessLevel.MODERATOR;
        } else {
            OffsetDateTime thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC);
            OffsetDateTime sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC);

            if (member.getTimeJoined().isBefore(thirtyDaysAgo)) {
                return AccessLevel.EXPERT; // На сервере больше 30 дней
            } else if (member.getTimeJoined().isBefore(sevenDaysAgo)) {
                return AccessLevel.REGULAR; // На сервере больше 7 дней
            } else {
                return AccessLevel.NEWBIE;
            }
        }
    }

    private String getAvailableCommandsForLevel(AccessLevel level) {
        StringBuilder commands = new StringBuilder();

        // Базовые команды для всех
        commands.append("** Основные команды:**\n");
        commands.append(String.format("`/%s` - Правила сервера\n", "rules"));
        commands.append(String.format("`/%s` - Ваш профиль\n", "profile"));
        commands.append(String.format("`/%s` - Информация о сервере\n", "serverinfo"));
        commands.append(String.format("`/%s` - Главное меню\n\n", "menu"));

        // Команды для REGULAR и выше
        if (level.canAccess(AccessLevel.REGULAR)) {
            commands.append(String.format("`/%s` - Настройки бота\n", "settings"));
            commands.append(String.format("`/%s` - Техническая поддержка\n\n", "support"));
        }

        // Команды для EXPERT и выше
        if (level.canAccess(AccessLevel.EXPERT)) {
            commands.append(String.format("`/%s` - Информация о пользователе\n", "userinfo"));
            commands.append(String.format("`/%s` - Оставить отзыв\n\n", "feedback"));
        }

        // Команды для MODERATOR и выше
        if (level.canAccess(AccessLevel.MODERATOR)) {
            commands.append(String.format("`/%s` - Очистка сообщений\n", "clear"));
        }

        // Команды для ADMIN
        if (level.canAccess(AccessLevel.ADMIN)) {
            commands.append("`/admin` - Админ панель\n");
        }

        return commands.toString();
    }

    private void handleRulesCommand(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage(RULES_MESSAGE).queue();
    }

    private void handleProfileCommand(SlashCommandInteractionEvent event) {
        String user = event.getUser().getAsMention();
        String joinDate = event.getMember().getTimeJoined().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        ** Профиль пользователя**
                        
                        **Никнейм:** %s
                        **На сервере с:** %s
                        **Роль:** %s
                        """.formatted(
                        user,
                        joinDate,
                        getUserAccessLevel(event.getUser()).getDisplayName()
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private void handleVoiceCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        // Находим голосовой канал по имени
        VoiceChannel voiceChannel = guild.getVoiceChannelsByName("Лобби", true).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Голосовой канал не найден"));

        // Подключаемся к голосовому каналу
        voiceConnectionService.connectToVoiceChannel();
        log.info("Подключение к голосовому каналу: {}", voiceChannel.getName());
        log.info("Готово. Ожидание данных...");
    }

    private void handleServerInfoCommand(SlashCommandInteractionEvent event) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        ** ИНФОРМАЦИЯ О СЕРВЕРЕ**
                        
                        **Название:** %s
                        **Участников:** %d
                        **Каналов:** %d
                        **Создан:** %s
                        """.formatted(
                        event.getGuild().getName(),
                        event.getGuild().getMemberCount(),
                        event.getGuild().getChannels().size(),
                        event.getGuild().getTimeCreated().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private void handleMenuCommand(SlashCommandInteractionEvent event, long guildId) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        ** ГЛАВНОЕ МЕНЮ**
                        
                        **Информация:**
                        %s - Правила сервера
                        %s - Информация о сервере
                        %s - Ваш профиль
                        
                        **Утилиты:**
                        %s - Настройки бота
                        %s - Техническая поддержка
                        %s - Очистка сообщений
                        
                        **Помощь:**
                        %s - Все команды
                        
                        *Выберите нужный раздел!*
                        """.formatted(
                        commandFormatter.createCommandLink("rules", "📜 Правила", guildId),
                        commandFormatter.createCommandLink("serverinfo", "🏰 Сервер", guildId),
                        commandFormatter.createCommandLink("profile", "👤 Профиль", guildId),
                        commandFormatter.createCommandLink("settings", "⚙️ Настройки", guildId),
                        commandFormatter.createCommandLink("support", "🆘 Поддержка", guildId),
                        commandFormatter.createCommandLink("clear", "🗑️ Очистка", guildId),
                        commandFormatter.createCommandLink("help", "❓ Помощь", guildId)
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private void handleUserInfoCommand(SlashCommandInteractionEvent event) {
        OptionMapping userOption = event.getOption("user");
        // Реализация информации о пользователе
        event.reply("Информация о пользователе...").queue();
    }

    private void handleClearCommand(SlashCommandInteractionEvent event) {
        OptionMapping amountOption = event.getOption("amount");
        // Реализация очистки сообщений
        event.reply("Очистка сообщений...").queue();
    }

    private void handleSupportCommand(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        ** ТЕХНИЧЕСКАЯ ПОДДЕРЖКА**
                        
                        Если у вас возникли проблемы:
                        1. Опишите вашу проблему
                        2. Укажите какие команды не работают
                        3. Приложите скриншоты если нужно
                        
                        **Контакты:**
                        • Создатель: <@ID_АДМИНА>
                        • Email: support@example.com
                        
                        **Полезные ссылки:**
                        %s - Частые вопросы
                        %s - Сообщить о ошибке
                        %s - Главное меню
                        """.formatted(
                        commandFormatter.createCommandLink("faq", "❓ FAQ", guildId),
                        commandFormatter.createCommandLink("report", "🐞 Report", guildId),
                        commandFormatter.createCommandLink("menu", "📋 Меню", guildId)
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private void handleSettingsCommand(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        **⚙️ НАСТРОЙКИ БОТА**
                        
                        **Настройки уведомлений:**
                        %s - Уведомления о сообщениях
                        %s - Уведомления о голосовых
                        %s - Email уведомления
                        
                        **Настройки профиля:**
                        %s - Приватность
                        %s - Язык интерфейса
                        
                        %s - Главное меню
                        """.formatted(
                        commandFormatter.createCommandLink("notify_messages", "💬 Сообщения", guildId),
                        commandFormatter.createCommandLink("notify_voice", "🎤 Голосовые", guildId),
                        commandFormatter.createCommandLink("notify_email", "📧 Email", guildId),
                        commandFormatter.createCommandLink("privacy", "🔒 Приватность", guildId),
                        commandFormatter.createCommandLink("language", "🌐 Язык", guildId),
                        commandFormatter.createCommandLink("menu", "📋 Меню", guildId)
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private void handleTestVoskCommand(SlashCommandInteractionEvent event) {
        try {
            event.deferReply().queue();


            event.getHook().sendMessage("✅ Тесты Vosk запущены! Проверьте логи в консоли.").queue();

        } catch (Exception e) {
            event.getHook().sendMessage("❌ Ошибка теста Vosk: " + e.getMessage()).queue();
        }
    }
}