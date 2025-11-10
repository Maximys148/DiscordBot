package org.example.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JDAConfig {

    @Value("${discord.bot.token}")
    private String token;

    @Value("${discord.bot.activity}")
    private String activityText;

    @Value("${discord.bot.activity-type:WATCHING}")
    private String activityType;

    private final Logger log = LogManager.getLogger(JDAConfig.class);

    @Bean
    public Activity botActivity() {
        return switch (activityType.toUpperCase()) {
            case "PLAYING" -> Activity.playing(activityText);
            case "LISTENING" -> Activity.listening(activityText);
            case "COMPETING" -> Activity.competing(activityText);
            default -> Activity.watching(activityText);
        };
    }

    @Bean
    public JDA jda(Activity botActivity) {
        try {
            JDA jda = JDABuilder.createDefault(token)
                    .setActivity(botActivity)
                    .build();

            jda.awaitReady();
            log.info("JDA успешно инициализирован и готов");
            return jda;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Инициализация JDA прервана", e);
        }
    }

}