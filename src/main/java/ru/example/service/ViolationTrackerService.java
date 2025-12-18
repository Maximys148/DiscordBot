package ru.example.service;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ViolationTrackerService {

    // Map<userId, количество нарушений>
    private final Map<String, Integer> userViolations = new ConcurrentHashMap<>();
    private final Map<String, List<Role>> userRoles = new ConcurrentHashMap<>();
    private final Set<String> mutedUsers = new HashSet<>();
    private final Guild guild;

    // Время жизни нарушений (24 часа)
    private static final long VIOLATION_EXPIRE_TIME = 24 * 60 * 60 * 1000;
    private final Map<String, Long> violationTimestamps = new ConcurrentHashMap<>();

    private final Logger log = LogManager.getLogger(ViolationTrackerService.class);

    public ViolationTrackerService(Guild guild) {
        this.guild = guild;
    }

    public void addViolation(Member member) {
        String id = member.getId();
        int currentViolations = userViolations.getOrDefault(id, 0);
        userViolations.put(id, currentViolations + 1);
        violationTimestamps.put(id, System.currentTimeMillis());

        log.info("Пользователь {} имеет {} нарушений",
                member.getUser().getGlobalName(),
                currentViolations + 1);
    }

    public Integer getViolationCount(String userId) {
        cleanupExpiredViolations(userId);
        return userViolations.getOrDefault(userId, 0);
    }

    public void resetViolations(String userId) {
        userViolations.remove(userId);
        violationTimestamps.remove(userId);
    }

    public boolean isUserMuted(String userId) {
        return mutedUsers.contains(userId);
    }

    public void muteUser(MessageReceivedEvent event, Member member) {
        try {
            if (member.isOwner()) {
                event.getChannel().sendMessage("❌ Нельзя замутить владельца сервера!").queue();
                return;
            }

            if (member.hasPermission(Permission.ADMINISTRATOR)) {
                event.getChannel().sendMessage("❌ Нельзя замутить администратора!").queue();
                return;
            }

            Guild guild = event.getGuild();
            List<Role> muteRoles = guild.getRolesByName("Опущенный", true);

            if (muteRoles.isEmpty()) {
                event.getChannel().sendMessage("❌ Роль 'Опущенный' не найдена! Создайте её вручную.").queue();
                return;
            }

            Role muteRole = muteRoles.get(0);

            // Проверяем, есть ли уже эта роль у пользователя
            if (member.getRoles().contains(muteRole)) {
                event.getChannel().sendMessage("✅ Пользователь уже имеет роль 'Опущенный'").queue();
                return;
            }

            // Сохраняем текущие роли пользователя (исключая @everyone)
            List<Role> currentRoles = member.getRoles().stream()
                    .filter(role -> !role.isPublicRole())
                    .collect(Collectors.toList());

            userRoles.put(member.getId(), currentRoles);

            // ПРОСТО ВЫДАЕМ РОЛЬ - ничего больше не меняем!
            guild.modifyMemberRoles(member, Collections.singletonList(muteRole)).
                    queue(
                    success -> {
                        mutedUsers.add(member.getId());
                        event.getChannel().sendMessage("🔇 " + member.getAsMention() + " получил роль 'Опущенный'!").queue();
                        log.info("Пользователь {} получил роль 'Опущенный'", member.getUser().getGlobalName());
                    },
                    error -> {
                        event.getChannel().sendMessage("❌ Ошибка при выдаче роли: " + error.getMessage()).queue();
                        log.error("Ошибка выдачи роли пользователю {}", member.getId(), error);
                    }
            );

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Ошибка: " + e.getMessage()).queue();
            log.error("Ошибка в muteUser", e);
        }
    }

    public void unmuteUser(MessageReceivedEvent event, Member member) {
        try {
            Guild guild = event.getGuild();
            List<Role> muteRoles = guild.getRolesByName("Опущенный", true);

            if (!muteRoles.isEmpty()) {
                Role muteRole = muteRoles.get(0);

                // Убираем роль мута
                guild.removeRoleFromMember(member, muteRole).queue(
                        success -> {
                            // Возвращаем сохранённые роли после успешного снятия мута
                            restoreUserRoles(member);
                        },
                        error -> {
                            event.getChannel().sendMessage("❌ Ошибка при снятии роли: " + error.getMessage()).queue();
                        }
                );
            } else {
                // Если роли нет, просто восстанавливаем права
                restoreUserRoles(member);
            }

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Ошибка при снятии мута: " + e.getMessage()).queue();
            log.error("Ошибка размута пользователя {}", member.getId(), e);
        }
    }

    private void restoreUserRoles(Member member) {
        // Возвращаем сохранённые роли
        List<Role> savedRoles = userRoles.getOrDefault(member.getId(), new ArrayList<>());
        if (!savedRoles.isEmpty()) {
            for (Role role : savedRoles) {
                guild.addRoleToMember(member, role).queue();
            }
        }

        // Очищаем данные
        mutedUsers.remove(member.getId());
        userRoles.remove(member.getId());
        resetViolations(member.getId());

        guild.getTextChannels().get(0).sendMessage(
                "✅ " + member.getAsMention() + " снят с мута! Роли возвращены."
        ).queue();

        log.info("Пользователь {} размучен", member.getUser().getGlobalName());
    }

    private void cleanupExpiredViolations(String userId) {
        Long timestamp = violationTimestamps.get(userId);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) > VIOLATION_EXPIRE_TIME) {
            resetViolations(userId);
        }
    }

    public Map<String, Integer> getAllViolations() {
        return new HashMap<>(userViolations);
    }

    // Метод для проверки существования роли
    public boolean checkMuteRoleExists(Guild guild) {
        return !guild.getRolesByName("Опущенный", true).isEmpty();
    }

    // Метод для получения роли
    public Role getMuteRole(Guild guild) {
        List<Role> roles = guild.getRolesByName("Опущенный", true);
        return roles.isEmpty() ? null : roles.get(0);
    }
}