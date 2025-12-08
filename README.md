# 🤖 Discord Bot - бот для discord сервера 

Бот администратор с текстовыми функциями и возможностью управлением голосом 

## 🌟 Особенности
- Локальное распознание речи с помощью модели Vosk
- Подробное логирование всех запросов

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

## 📝 Документации и ресурсы
- [Сайт с моделями Vosk](https://alphacephei.com/vosk/models)
- [Документация для работы с библиотекой discordа](https://discord.com/developers)
- [Как починить Discord](https://howdyho.net/windows-software/discord-fix-snova-rabotayushij-diskord-vojs-zvonki), либо с [официального сайта](https://github.com/Flowseal/zapret-discord-youtube), лично у меня работает при запуске general (ALT2).bat, либо general (ALT8).bat
- [Как подключить бота к своему discord серверу(гилдии)](https://www.youtube.com/watch?v=a5Stb2vf6oI)

## 🛠️ Технологический стек программы
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Log4j](https://img.shields.io/badge/Log4j-1F1F1F?style=for-the-badge&logo=apache&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)  
