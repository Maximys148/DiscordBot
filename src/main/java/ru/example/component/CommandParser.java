// CommandParser.java
package ru.example.component;

import org.springframework.stereotype.Service;
import ru.example.enums.VoiceCommandType;
import ru.example.model.CommandInfo;

@Service
public class CommandParser {

    /**
     * Разбирает текст, выделяет параметры для команды по её ID.
     * Пример упрощён, надо расширять под реальные параметры команд.
     *
     * @param text текст с командой
     * @param commandId идентификатор команды
     * @return заполненный объект CommandInfo с параметрами
     */
    public CommandInfo parse(String text, VoiceCommandType commandId) {
        CommandInfo info = new CommandInfo();
        info.setCommandId(commandId);

        String lowerText = text.toLowerCase();

        switch (commandId) {
            case BAN_USER:
                info.setTargetUser(extractUserFromText(lowerText, new String[]{"забанить", "бан"}));
                break;
            case MUTE_USER:
                info.setTargetUser(extractUserFromText(lowerText, new String[]{"за муть", "замуть", "отключить микрофон", "выключи микрофон", "выключи звук"}));
                break;
            case UNMUTE_USER:
                info.setTargetUser(extractUserFromText(lowerText, new String[]{"заглушить", "замутить", "отключить микрофон", "включить микрофон", "выключить звук"}));
                break;
            case DECREASE_VOLUME:
            case INCREASE_VOLUME:
                info.setTargetUser(extractUserFromText(lowerText, new String[]{"уменьшить громкость", "увеличить громкость"}));
                info.setVolumeChange(extractVolumeValue(lowerText));
                break;
        }

        return info;
    }

    // Примерный метод для выделения имени пользователя из текста (с выбором самого длинного слова после ключевого слова)
    private String extractUserFromText(String text, String[] keywords) {
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx != -1) {
                String afterKeyword = text.substring(idx + keyword.length()).trim();
                String[] words = afterKeyword.split("\\s+");
                if (words.length > 0) {
                    // Найдём самое длинное слово в массиве слов после ключевого слова
                    String longestWord = "";
                    for (String word : words) {
                        if (word.length() > longestWord.length()) {
                            longestWord = word;
                        }
                    }
                    return longestWord;
                }
            }
        }
        return null;
    }


    // Метод для выделения значения громкости (например "на 10", "на десять" — здесь простая заглушка)
    private int extractVolumeValue(String text) {
        // Простейший поиск цифр в тексте
        String[] words = text.split("\\s+");
        for (String w : words) {
            try {
                return Integer.parseInt(w);
            } catch (NumberFormatException ignored) {}
        }
        return 0; // значение по умолчанию
    }


}
