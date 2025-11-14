package ru.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

import java.io.IOException;

@Configuration
public class SttConfig {
    @Value("${vosk.modelPath}")
    String modelPath;
    @Bean
    public Model model() throws IOException {

        return new Model(modelPath);
    }
}
