/*
package ru.example.service;

import com.ibm.icu.text.Transliterator;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


@Service
public class VoskDictionaryService {

    private final Guild guild;
    private final Transliterator transliterator;
    private final Logger log = LogManager.getLogger(VoskDictionaryService.class);

    private static final String DICTIONARY_FILE_PATH = "vosk_custom_dict.txt";
    @Value("${vosk.modelPath}")
    String MODEL_PATH;

    private Model voskModel;
    // Getter для recognizer, чтобы использовать его в распознавании речи
    @Getter
    private Recognizer recognizer;

    public VoskDictionaryService(Guild guild) {
        this.guild = guild;
        this.transliterator = Transliterator.getInstance("Latin-Cyrillic");
    }

    @PostConstruct
    public void init() {
        buildAndSaveDictionary();
        loadModelWithCustomDictionary();
    }

    private void buildAndSaveDictionary() {
        List<Member> members = guild.getMembers();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DICTIONARY_FILE_PATH))) {
            for (Member member : members) {
                String nickname = member.getEffectiveName();
                String nicknameCyr = transliterator.transliterate(nickname).toLowerCase();
                writer.write(nicknameCyr);
                writer.newLine();
                log.info("[translate:Добавлено слово в словарь] {}", nicknameCyr);
            }
            log.info("[translate:Пользовательский словарь успешно сохранён в файл] {}", DICTIONARY_FILE_PATH);
        } catch (IOException e) {
            log.error("[translate:Ошибка записи файла словаря] {}", e.getMessage());
        }
    }

    private void loadModelWithCustomDictionary() {
        try {
            voskModel = new Model(MODEL_PATH);
            recognizer = new Recognizer(voskModel, 16000.0f, DICTIONARY_FILE_PATH);
            log.info("[translate:Модель Vosk успешно загружена с пользовательским словарём]");
        } catch (IOException e) {
            log.error("[translate:Ошибка загрузки модели Vosk]: {}", e.getMessage());
        }
    }
}
*/
