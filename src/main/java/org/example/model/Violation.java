package org.example.model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.enums.ViolationType;

@AllArgsConstructor
@Getter
public class Violation {
    private ViolationType type; // Тип нарушения
    private String content; // Содержимое сообщение
    private long timestamp; // Время нарушения
    private String messageId;
    private String moderatorId; // Для ручных нарушений
}

