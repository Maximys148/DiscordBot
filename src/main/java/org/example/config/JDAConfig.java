package org.example.config;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.CommandService;
import org.example.service.MessageService;
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
    public JDA jda(MessageService messageService, Activity botActivity, CommandService commandService) {
        return JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MEMBERS
                )
                .enableCache(CacheFlag.VOICE_STATE)
                .setActivity(botActivity)
                .addEventListeners(messageService, commandService)
                .build();
    }
}