package ru.example.service.TextCommandServices;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.enums.AccessLevel;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static ru.example.constant.MessageConstant.HELP_MESSAGE;
import static ru.example.constant.MessageConstant.RULES_MESSAGE;

@Service
public class HelpService {

    private final Guild guild;
    private final Logger log = LogManager.getLogger(HelpService.class);

    public HelpService(Guild guild) {
        this.guild = guild;
    }

    public void execute(SlashCommandInteractionEvent event) {
        String username = event.getUser().getAsMention();
        AccessLevel userLevel = getUserAccessLevel(event.getUser());

        String message = HELP_MESSAGE.formatted(
                username,
                userLevel.getDisplayName(),
                getAvailableCommandsForLevel(userLevel)
        );

        event.getHook().sendMessage(message).queue();
    }

    public void executeRules(SlashCommandInteractionEvent event) {
        event.getHook().sendMessage(RULES_MESSAGE).queue();
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
                return AccessLevel.EXPERT;
            } else if (member.getTimeJoined().isBefore(sevenDaysAgo)) {
                return AccessLevel.REGULAR;
            } else {
                return AccessLevel.NEWBIE;
            }
        }
    }

    private String getAvailableCommandsForLevel(AccessLevel level) {
        StringBuilder commands = new StringBuilder();

        commands.append("** Основные команды:**\n");
        commands.append(String.format("`/%s` - Правила сервера\n", "rules"));
        commands.append(String.format("`/%s` - Ваш профиль\n", "profile"));
        commands.append(String.format("`/%s` - Информация о сервере\n", "serverinfo"));
        commands.append(String.format("`/%s` - Главное меню\n\n", "menu"));

        if (level.canAccess(AccessLevel.REGULAR)) {
            commands.append(String.format("`/%s` - Настройки бота\n", "settings"));
            commands.append(String.format("`/%s` - Техническая поддержка\n\n", "support"));
        }

        if (level.canAccess(AccessLevel.EXPERT)) {
            commands.append(String.format("`/%s` - Информация о пользователе\n", "userinfo"));
            commands.append(String.format("`/%s` - Оставить отзыв\n\n", "feedback"));
        }

        if (level.canAccess(AccessLevel.MODERATOR)) {
            commands.append(String.format("`/%s` - Очистка сообщений\n", "clear"));
        }

        if (level.canAccess(AccessLevel.ADMIN)) {
            commands.append("`/admin` - Админ панель\n");
        }

        return commands.toString();
    }
}