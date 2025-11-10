package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

import java.io.IOException;

@Configuration
public class SttConfig {

    @Bean
    public Model model() throws IOException {
        String modelPath = "models/vosk-model-small-ru-0.22";
        return new Model(modelPath);
    }
}
