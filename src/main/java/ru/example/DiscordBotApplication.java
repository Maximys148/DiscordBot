package ru.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "ru.example")
@EnableConfigurationProperties
public class DiscordBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscordBotApplication.class, args);
    }
}