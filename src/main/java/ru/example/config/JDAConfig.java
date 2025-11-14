package ru.example.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
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
            return jda;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Инициализация JDA прервана", e);
        }
    }

}