package org.example.enums;

public enum AccessLevel {
    NEWBIE("Новичок", 0),
    REGULAR("Обычный пользователь", 1), 
    EXPERT("Эксперт", 2),
    MODERATOR("Модератор", 3),
    ADMIN("Администратор", 4);

    private final String displayName;
    private final int level;

    AccessLevel(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public boolean canAccess(AccessLevel required) { return this.level >= required.level; }
}