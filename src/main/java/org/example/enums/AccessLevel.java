package org.example.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AccessLevel {
    MUTE("Опущенный", -1),
    NEWBIE("Новичок", 0),
    REGULAR("Обычный пользователь", 1), 
    EXPERT("Эксперт", 2),
    MODERATOR("Модератор", 3),
    ADMIN("Администратор", 4);

    private final String displayName;
    private final int level;

    public boolean canAccess(AccessLevel required) {
        return this.level >= required.level;
    }
}