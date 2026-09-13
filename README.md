# 🤖 Discord Bot - бот для discord сервера 

Бот администратор с текстовыми и голосовыми функциями 

## 🌟 Особенности
- Локальное распознание речи с помощью модели Vosk
- Локальное генерация речи с помощью интеграции локального сервера с моделью Silero
- Подробное логирование всех запросов
- Как и указывалось выше программа работает полностью локально без сторонних API (не учитывая самого Discord)

## 💬 Команды бота

| Название | Описание | Голосовое управление |
|----------|--------|:-------------:|
| `help` | Показать список всех команд |❌ |
| `rules` | Показать правила сервера | ❌ |
| `profile` | Посмотреть свой профиль | ❌ |
| `join_voice` | Подключить бота к голосовому каналу | ❌ |
| `mute_user` | Отключить пользователю микрофон | ✅ |
| `unmute_user` | Включить пользователю микрофон | ✅ |

!!! ВАЖНО
- Добавить файл application.yml в src/main/resources/ в нём указываешь id сервера и токен бота.
- Id сервера можно найти кликнув правой кнопкой мыши по значку вашего сервера(в левой части приложения указаны сервера в которых ты находишься) и выбрать копировать ID сервера
- Токен бота ищите в https://discord.com/developers/applications выбираете application вашего бота, в разделе bot выбираете token и копируйте его
- Какую модель Vosk выбрать? Все модели храняться по пути models/, далее выбираете модель vosk-model-small-ru-0.22 или vosk-model-ru-0.10 . С vosk-model-small-ru-0.22(40 Mb) программа запускается за 3-7 секунд в зависимости от системы, что подходит для тестирования, с vosk-model-ru-0.10(1.5 Gb) программа запускается в районе 50 секунд, также она лучше распазнаёт речь. Есть и другие модели, но этих двух должно хватить.
Ниже приведу пример файла
```
guild:
  id: "вставляешь id сервера(гильдии)"
discord:
  bot:
    token: ${DISCORD_BOT_TOKEN: вставляешь токен бота}
    prefix: "!"
    activity: " за сервером"
    activity-type: WATCHING
vosk:
  modelPath:
    "models/vosk-model-small-ru-0.22" // выбираешь нужную модель
transliterator:
  CYRILLIC_TO_LATIN: "Latin-Cyrillic"
logging:
  level:
    org.example.service.voice: TRACE
    org.example.service.voice.org.example.service.voice.STT.VoskService: TRACE
    org.example.service.voice.AudioProcessingService: DEBUG
    org.example.service.voice.AudioReceiveHandler: DEBUG
    org.example.service.ru.example.service.CommandService: INFO

    org.vosk: DEBUG
    com.alphacephei: DEBUG

    net.dv8tion.jda: WARN
    net.dv8tion.jda.api.audio: INFO
    net.dv8tion.jda.internal.audio: WARN

    org.springframework: WARN
    org.springframework.boot: WARN

    com.sun.jna: WARN
    org.apache: WARN

  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```
- Добавить файл MessageConstant.java в src/main/java/ru/example/constant/ в нём ты указываешь текст помощи(команда /help), текст правил и запрещённые слова.
Ниже приведу пример файла
```
package ru.example.constant;

import java.util.List;

public class MessageConstant {
    public static final String HELP_MESSAGE = """
        Приветствую, **%s**! 👋
        
        **ДОСТУПНЫЕ КОМАНДЫ** (ваш уровень: %s)
        
        %s
        
        *Используйте слеш-команды (/команда) для взаимодействия с ботом*
        """;
    public static final String RULES_MESSAGE = """
        **Правила сервера **
        
        1.1  Общаться уважительно.
       
        """;
    public static final List<String> BAN_WORD = List.of("Перечисление всяких плохих слов");
```
## Установка и запуск
# 1. Клонирование и настройка
- git clone <https://github.com/Maximys148/DiscordBot>
- cd DiscordBot

# 2. Создание необходимых директорий
mkdir -p tts_service vits_service debug

# 3. Настройка переменных окружения (Windows PowerShell)
- "RU_EXAMPLE_STT_MODEL_PATH=/app/models/vosk-model-small-ru-0.22" | Out-File -Encoding ascii .env // тут выбираешь модель из папки models в корне проекта
- docker-compose config просмотр настроек

# 4. Запуск всех сервисов
docker-compose up -d

## 📝 Документации и ресурсы
- [Сайт с моделями Vosk](https://alphacephei.com/vosk/models)
- [Документация для работы с библиотекой discordа](https://discord.com/developers)
- [Как починить Discord](https://howdyho.net/windows-software/discord-fix-snova-rabotayushij-diskord-vojs-zvonki), либо с [официального сайта](https://github.com/Flowseal/zapret-discord-youtube)
- [Как подключить бота к своему discord серверу(гилдии)](https://www.youtube.com/watch?v=a5Stb2vf6oI)

## 🛠️ Технологический стек программы
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Web](https://img.shields.io/badge/Spring_Web-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Log4j](https://img.shields.io/badge/Log4j-1F1F1F?style=for-the-badge&logo=apache&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)  
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
