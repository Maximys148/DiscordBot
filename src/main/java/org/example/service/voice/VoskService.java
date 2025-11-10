package org.example.service.voice;

import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class VoskService {

    private final Model model;
    private final OpusToPcmDecoder opusToPcmDecoder;
    private final Logger log = LogManager.getLogger(VoskService.class);


    public VoskService(OpusToPcmDecoder opusToPcmDecoder, Model model) {
        this.opusToPcmDecoder = opusToPcmDecoder;
        this.model = model;
    }

    public CompletableFuture<String> recognizeDiscordAudio(byte[] discordAudio, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔧 Конвертация Discord → Vosk для пользователя {}", user);

                byte[] voskAudio = opusToPcmDecoder.resample48kTo16k(discordAudio);

                if (voskAudio == null || voskAudio.length == 0) {
                    log.error("❌ Конвертация не удалась");
                    return "";
                }

                log.info("✅ Конвертация успешна: {} → {} байт",
                        discordAudio.length, voskAudio.length);

                return recognizeAudio(voskAudio);

            } catch (Exception e) {
                log.error("❌ Ошибка обработки аудио", e);
                return "";
            }
        });
    }

    private String recognizeAudio(byte[] audioData) {
        if (audioData == null || audioData.length < 3200) {
            log.warn("⚠️ Слишком мало данных для распознавания: {} байт",
                    audioData != null ? audioData.length : 0);
            return "";
        }

        try (Recognizer recognizer = new Recognizer(model, 16000.0f)) {
            recognizer.setWords(true);
            recognizer.setPartialWords(true);

            log.info("🔍 Начало распознавания: {} байт ({}ms)",
                    audioData.length, (audioData.length * 1000) / 32000);

            // Передаем ВСЮ фразу целиком в Vosk
            boolean accepted = recognizer.acceptWaveForm(audioData, audioData.length);

            String resultJson = recognizer.getResult();
            log.info("📄 Сырой JSON от Vosk: {}", resultJson);

            // Извлекаем текст из JSON ДО исправления кодировки
            String rawText = extractTextFromJson(resultJson);

            if (rawText == null || rawText.isEmpty()) {
                log.warn("📭 Текст не распознан");
                return "";
            }

            log.info("🔤 Извлеченный текст до фикса: '{}'", rawText);

            // Исправляем кодировку только текста
            String fixedText = fixTextEncoding(rawText);

            log.info("✅ Исправленный текст: '{}'", fixedText);
            return fixedText;

        } catch (Exception e) {
            log.error("❌ Ошибка распознавания фразы", e);
            return "";
        }
    }

    /**
     * Извлекает текст из JSON ответа Vosk
     */
    private String extractTextFromJson(String resultJson) {
        if (resultJson == null || resultJson.isEmpty()) {
            return "";
        }

        try {
            // Простой парсинг JSON через поиск поля "text"
            int textStart = resultJson.indexOf("\"text\"");
            if (textStart == -1) {
                log.warn("📭 Поле 'text' не найдено в JSON: {}", resultJson);
                return "";
            }

            // Находим начало значения
            int valueStart = resultJson.indexOf(":", textStart);
            if (valueStart == -1) {
                return "";
            }

            // Находим первую кавычку после двоеточия
            int quoteStart = resultJson.indexOf("\"", valueStart);
            if (quoteStart == -1) {
                return "";
            }

            // Находим закрывающую кавычку
            int quoteEnd = resultJson.indexOf("\"", quoteStart + 1);
            if (quoteEnd == -1) {
                return "";
            }

            // Извлекаем текст между кавычками
            String text = resultJson.substring(quoteStart + 1, quoteEnd);
            log.debug("📖 Извлечен текст из JSON: '{}'", text);

            return text;

        } catch (Exception e) {
            log.error("❌ Ошибка извлечения текста из JSON: {}", resultJson, e);
            return "";
        }
    }

    /**
     * Исправляет кодировку текста
     */
    private String fixTextEncoding(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        try {
            log.debug("🔧 Исправление кодировки: '{}'", text);

            // Vosk возвращает UTF-8 байты, но они интерпретируются как Windows-1251
            byte[] bytes = text.getBytes("Windows-1251");
            String fixed = new String(bytes, StandardCharsets.UTF_8);

            log.info("✅ Кодировка исправлена: '{}' -> '{}'", text, fixed);
            return fixed;

        } catch (Exception e) {
            log.warn("⚠️ Не удалось исправить кодировку для: '{}'", text);
            return text;
        }
    }



    @PreDestroy
    public void cleanup() {
        log.info("🧹 Очистка ресурсов VoskService");
        if (model != null) {
            model.close();
        }
    }
}