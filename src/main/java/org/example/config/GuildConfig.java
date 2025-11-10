package org.example.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GuildConfig {
    @Value("${guild.id}")
    private String guildId;

    @Bean
    public Guild guild(JDA jda) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException("Гильдия с ID " + guildId + " не найдена");
        }
        return guild;
    }
}