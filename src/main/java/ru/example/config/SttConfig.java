package ru.example.config;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

import java.io.File;
import java.io.IOException;

@Configuration
public class SttConfig {

    private final Logger log = LogManager.getLogger(SttConfig.class);

    @Value("${ru.example.stt.model-path:/app/models/vosk-model-ru-0.10}")
    private String configModelPath;

    @Bean
    public Model model() throws IOException {
        // ✅ ПРИОРИТЕТ 1: ENV переменная из терминала
        String envPath = System.getenv("RU_EXAMPLE_STT_MODEL_PATH");

        // ✅ ПРИОРИТЕТ 2: config из application.yml
        String finalPath = (envPath != null && !envPath.trim().isEmpty())
                ? envPath.trim()
                : configModelPath;

        log.info("🌍 ENV RU_EXAMPLE_STT_MODEL_PATH='{}'", envPath);
        log.info("⚙️  CONFIG ru.example.stt.model-path='{}'", configModelPath);
        log.info("🎯 FINAL PATH: {}", finalPath);

        File modelDir = new File(finalPath);
        log.info("📁 Exists: {}", modelDir.exists());
        log.info("📁 Is directory: {}", modelDir.isDirectory());

        if (!modelDir.exists()) {
            throw new IllegalStateException("❌ Model not found: " + finalPath);
        }

        log.info("✅ Creating Vosk Model...");
        Model model = new Model(finalPath);
        log.info("✅ Vosk Model {} LOADED SUCCESSFULLY!", finalPath);
        return model;
    }

    @PostConstruct
    public void logEnvDebug() {
        String envPath = System.getenv("RU_EXAMPLE_STT_MODEL_PATH");
        log.info("🔍 DEBUG: ENV RU_EXAMPLE_STT_MODEL_PATH='{}'", envPath);
    }
}
