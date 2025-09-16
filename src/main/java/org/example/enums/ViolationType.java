package org.example.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ViolationType {
    BANNED_WORD("Запрещённое слово"),
    SPAM("Спам"),
    FLOOD("Флуд"),
    CAPS_LOCK("Капслок"),
    LINKS("Ссылки"),
    MENTION_SPAM("Спам упоминаниями"),
    ADVERTISEMENT("Реклама"),
    NSFW("NSFW контент"),
    TOXICITY("Токсичность");

    private final String description;
}