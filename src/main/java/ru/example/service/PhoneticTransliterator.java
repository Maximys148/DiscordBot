package ru.example.service;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

@Service
public final class PhoneticTransliterator {

    private static final DoubleMetaphone dm = new DoubleMetaphone();
    private static final Map<String, String> dictionary = new HashMap<>();

    static {
        // Заполните словарь исключений
        //dictionary.put(dm.doubleMetaphone("vector"), "вектор");
        //dictionary.put(dm.doubleMetaphone("maximus"), "максимус");
        // Добавляйте другие правила по необходимости
    }

    private PhoneticTransliterator() {
        // Приватный конструктор для утилитного класса
    }

    public static String transliterate(String input) {
        String inputCode = dm.doubleMetaphone(input.toLowerCase());
        if (dictionary.containsKey(inputCode)) {
            return dictionary.get(inputCode);
        }
        return simpleTransliterate(input);
    }

    private static String simpleTransliterate(String input) {
        // Здесь вызывайте ICU4J transliterator или другую логику базовой трансляции
        return input; // замените на вызов ICU4J, например
    }
}
