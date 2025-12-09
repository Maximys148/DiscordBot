package ru.example.service.voice.STT;

import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;
import ru.example.filter.AudioFilter;
import ru.example.service.voice.OpusToPcmDecoder;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Преобразует аудио данные в текст
 */
@Service
public class SttService {

    private final Model model;
    private final OpusToPcmDecoder opusToPcmDecoder;
    private final AudioFilter audioFilter;
    private final Logger log = LogManager.getLogger(SttService.class);

    public SttService(OpusToPcmDecoder opusToPcmDecoder, Model model, AudioFilter audioFilter) {
        this.opusToPcmDecoder = opusToPcmDecoder;
        this.model = model;
        this.audioFilter = audioFilter;
    }

    public CompletableFuture<String> recognizeDiscordAudio(byte[] discordAudio, User user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Конвертация Discord → Vosk для пользователя {}", user);

                byte[] voskAudio = opusToPcmDecoder.resample48kTo16k(discordAudio);

                if (voskAudio == null || voskAudio.length == 0) {
                    log.error("Молчание");
                    return "";
                }

                //log.info("✅ Конвертация успешна: {} → {} байт", discordAudio.length, voskAudio.length);

                return recognizeAudio(voskAudio, user);

            } catch (Exception e) {
                log.error("❌ Ошибка обработки аудио", e);
                return "";
            }
        });
    }

    private String recognizeAudio(byte[] audioData, User user) {
        if (audioData == null) {
            log.warn(" Аудио сообщение пустое");
            return "";
        }

        try (Recognizer recognizer = new Recognizer(model, 16000.0f)) {
            recognizer.setWords(true);
            recognizer.setPartialWords(true);

            //log.info("🔍 Начало распознавания: {} байт ({}ms)",audioData.length, (audioData.length * 1000) / 32000);

            // Передаем ВСЮ фразу целиком в Vosk
            boolean accepted = recognizer.acceptWaveForm(audioData, audioData.length);

            String resultJson = recognizer.getResult();
            //log.info("📄 Сырой JSON от Vosk: {}", resultJson);

            // Извлекаем текст из JSON ДО исправления кодировки
            String rawText = extractTextFromJson(resultJson);

            if (rawText.isEmpty()) {
                log.warn("Текст пустой");
                return "";
            }

            // log.info("🔤 Извлеченный текст до фикса: '{}'", rawText);
            audioFilter.checkAudio(rawText, user);
            log.info("{} - {}", user.getGlobalName(), rawText);
            return rawText;

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

    @PreDestroy
    public void cleanup() {
        log.info("🧹 Очистка ресурсов VoskService");
        if (model != null) {
            model.close();
        }
    }
}