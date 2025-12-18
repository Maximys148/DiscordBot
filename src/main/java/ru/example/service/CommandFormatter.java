package ru.example.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CommandFormatter {

    private final JDA jda;
    private final Map<Long, Map<String, Long>> commandCache = new ConcurrentHashMap<>();
    private final Logger log = LogManager.getLogger(CommandFormatter.class);

    public CommandFormatter(@Lazy JDA jda) {
        this.jda = jda;
    }


    public String createCommandLink(String commandName, String displayText, long guildId) {
        Long commandId = getCommandId(commandName, guildId);
        if (commandId != null && commandId > 0) {
            return "[%s](</%s:%d>)".formatted(displayText, commandName, commandId);
        }
        return "`/" + commandName + "`";
    }

    private Long getCommandId(String commandName, long guildId) {
        Map<String, Long> guildCache = commandCache.computeIfAbsent(guildId, k -> new HashMap<>());
        
        if (guildCache.containsKey(commandName)) {
            return guildCache.get(commandName);
        }

        Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.retrieveCommands().queue(commands -> {
                commands.forEach(cmd -> {
                    if (cmd.getName().equals(commandName)) {
                        guildCache.put(commandName, cmd.getIdLong());
                    }
                });
            });
        }

        return guildCache.get(commandName);
    }

    public void refreshCache(long guildId) {
        commandCache.remove(guildId);
    }

    public void refreshAllCaches() {
        commandCache.clear();
    }
}