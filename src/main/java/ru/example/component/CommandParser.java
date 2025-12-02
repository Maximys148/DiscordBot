// CommandParser.java
package ru.example.component;

import org.springframework.stereotype.Service;
import ru.example.enums.VoiceCommandType;
import ru.example.model.CommandInfo;

import java.util.Arrays;

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
                info.setTargetUser(extractTargetUser(lowerText, new String[]{"забанить", "бан"}));
                break;
            case MUTE_USER:
                info.setTargetUser(extractTargetUser(lowerText, new String[]{"за муть", "замуть", "отключи микрофон", "выключи микрофон", "выключи звук"}));
                break;
            case UNMUTE_USER:
                info.setTargetUser(extractTargetUser(lowerText, new String[]{"раз муть", "размуть", "включи микрофон", "включи микрофон", "выключи звук"}));
                break;
            case DECREASE_VOLUME:
            case INCREASE_VOLUME:
                info.setTargetUser(extractTargetUser(lowerText, new String[]{"уменьшить громкость", "увеличить громкость"}));
                info.setVolumeChange(extractVolumeValue(lowerText));
                break;
        }

        return info;
    }

    private String extractTargetUser(String text, String[] keywords) {
        String[] prepositions = {"для", "от", "на", "с", "у", "про", "после", "перед", "к", "в", "на", "сделай", "сделайте", "используй", "используйте"}; // добавьте по необходимости
        String lowerText = text.toLowerCase();

        for (String keyword : keywords) {
            int idx = lowerText.indexOf(keyword);
            if (idx != -1) {
                // Вырезаем часть после ключевого слова
                String afterKeyword = lowerText.substring(idx + keyword.length()).trim();

                // Разбиваем на слова
                String[] words = afterKeyword.split("\\s+");

                // Ищем предлог или начало никнейма
                int startIdx = 0;
                for (int i = 0; i < words.length; i++) {
                    String w = words[i];
                    if (Arrays.asList(prepositions).contains(w) || w.matches("[.,:;!?]")) {
                        startIdx = i + 1; // следующего слова после предлога - предполагаемый Никнейм
                        break;
                    }
                }

                // Собираем никнейм из слов после предлога
                StringBuilder nicknameBuilder = new StringBuilder();
                for (int i = startIdx; i < words.length; i++) {
                    String w = words[i];
                    // Можно добавить условия для определения конца никнейма, например: если нашли слово-параметр
                    // или слово, начинающееся с '@' или содержащего спецсимвол
                    // Для простоты — собираем все оставшиеся слова
                    if (!nicknameBuilder.isEmpty()) {
                        nicknameBuilder.append(" ");
                    }
                    nicknameBuilder.append(words[i]);
                }

                String nickname = nicknameBuilder.toString().trim();
                if (!nickname.isEmpty()) {
                    return nickname;
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
