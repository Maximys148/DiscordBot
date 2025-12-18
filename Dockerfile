FROM azul/zulu-openjdk:17-jre

# ✅ UTF-8 + Python + Silero
ENV DEBIAN_FRONTEND=noninteractive
ENV LANGUAGE=ru_RU.UTF-8
ENV LANG=ru_RU.UTF-8
ENV LC_ALL=ru_RU.UTF-8

RUN apt-get update && \
    apt-get install -y language-pack-ru python3 python3-pip ffmpeg && \
    locale-gen ru_RU.UTF-8 && \
    dpkg-reconfigure locales -f noninteractive && \
    rm -rf /var/lib/apt/lists/*

# ✅ Silero TTS v5 + зависимости (CPU)
RUN pip3 install --no-cache-dir torch torchaudio --index-url https://download.pytorch.org/whl/cpu && \
    pip3 install --no-cache-dir flask numpy omegaconf silero

# ✅ КОПИРУЕМ Vosk модели
COPY models/vosk-model-small-ru-0.22 /app/models/vosk-model-small-ru-0.22
COPY models/vosk-model-ru-0.10 /app/models/vosk-model-ru-0.10

# ✅ Silero TTS сервер
COPY tts_service/tts_server.py /app/tts_server.py
EXPOSE 5000

# ✅ Java бот
COPY target/discord-bot-1.0.0.jar /app.jar
WORKDIR /app

# ✅ Запуск БОТА (TTS сервер уже работает на 5000)
CMD ["java", "-jar", "/app.jar"]
