package org.example.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CommandRegistration {

    private final JDA jda;
    private final List<SlashCommandData> globalCommands;
    private final List<SlashCommandData> adminCommands;
    private final String guildId;

    private final Logger log = LogManager.getLogger(CommandRegistration.class);

    public CommandRegistration(JDA jda,
                               @Qualifier("globalCommands") List<SlashCommandData> globalCommands,
                               @Qualifier("adminCommands") List<SlashCommandData> adminCommands,
                               @Value("${guild.id}") String guildId) {
        this.jda = jda;
        this.globalCommands = globalCommands;
        this.adminCommands = adminCommands;
        this.guildId = guildId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        try {
            jda.awaitReady();

            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                // Объединяем все команды в один список
                List<SlashCommandData> allCommands = new ArrayList<>();
                allCommands.addAll(globalCommands);
                allCommands.addAll(adminCommands);

                // Регистрируем все команды одним вызовом
                guild.updateCommands()
                        .addCommands(allCommands)
                        .queue(success -> {
                            log.info("Все команды зарегистрированы для гильдии {}", guild.getName());
                            log.info("Зарегистрировано команд: {}", allCommands.size());
                        }, error -> {
                            log.error("Ошибка регистрации команд: {}", error.getMessage());
                        });
            } else {
                log.warn("Гильдия с ID {} не найдена", guildId);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Ожидание готовности JDA прервано");
        }
    }
}