package ru.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.example.enums.ViolationType;

/**
 * Нарушение сообщения по правилам сервера
 */
@AllArgsConstructor
@Getter
public class Violation {
    private ViolationType type; // Тип нарушения
    private String content; // Содержимое сообщение
    private long timestamp; // Время нарушения
    private String messageId;
    private String moderatorId; // Для ручных нарушений
}

