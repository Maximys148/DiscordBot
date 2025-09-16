package org.example.service;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ViolationTrackerService {

    // Map<userId, количество нарушений>
    private final Map<String, Integer> userViolations = new ConcurrentHashMap<>();
    private final Set<String> mutedUsers =  new HashSet<>();

    
    // Время жизни нарушений (24 часа)
    private static final long VIOLATION_EXPIRE_TIME = 24 * 60 * 60 * 1000; 
    private final Map<String, Long> violationTimestamps = new ConcurrentHashMap<>();

    private final Logger log = LogManager.getLogger(ViolationTrackerService.class);

    public ViolationTrackerService() {
    }

    public void addViolation(String userId) {
        int currentViolations = userViolations.getOrDefault(userId, 0);
        userViolations.put(userId, currentViolations + 1);
        violationTimestamps.put(userId, System.currentTimeMillis());
        
        log.info("Пользователь " + userId + " имеет " + (currentViolations + 1) + " нарушений");
    }

    public Integer getViolationCount(String userId) {
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
            List<Role> rolesMute = event.getGuild().getRolesByName("Опущенный", true);
            Role role = rolesMute.get(0);

            // Выдаем роль мута
            event.getGuild().addRoleToMember(member, role).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage(" Ошибка при выдаче мута: ");
        }
    }

    public void unmuteUser(String userId) {
        mutedUsers.remove(userId);
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
}