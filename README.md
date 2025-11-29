# 🤖 Discord Bot - бот для discord сервера 

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Log4j](https://img.shields.io/badge/Log4j-1F1F1F?style=for-the-badge&logo=apache&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Бот администратор с текстовыми функциями и возможностью управлением голосом 

## 🌟 Особенности
- Локальное распознание речи с помощью модели Vosk
- Подробное логирование всех запросов

!!! ВАЖНО
Добавить файл application.yml в src/main/resources/ в нём ты указываешь ip и порты.
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

## 📝 Документации
- [Документация для работы с библиотекой discordа](https://discord.com/developers)
- [Как подключить бота к своему discord серверу(гилдии)](https://www.youtube.com/watch?v=a5Stb2vf6oI)
