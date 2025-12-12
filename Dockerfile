FROM azul/zulu-openjdk:17-jre

# ✅ UTF-8 Кодировка (только нужные строки)
ENV DEBIAN_FRONTEND=noninteractive
ENV LANGUAGE=ru_RU.UTF-8
ENV LANG=ru_RU.UTF-8
ENV LC_ALL=ru_RU.UTF-8

RUN apt-get update && \
    apt-get install -y language-pack-ru && \
    locale-gen ru_RU.UTF-8 && \
    dpkg-reconfigure locales -f noninteractive && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# ✅ КОПИРУЕМ ВСЕ 4 модели!
COPY models/vosk-model-small-ru-0.22 /app/models/vosk-model-small-ru-0.22
COPY models/vosk-model-ru-0.10 /app/models/vosk-model-ru-0.10

# ✅ ЛИСТИНГ для проверки
RUN ls -la /app/models/ && \
    ls -la /app/models/vosk-model-small-ru-0.22/ || echo "small missing"

COPY target/discord-bot-1.0.0.jar /app.jar
WORKDIR /app
CMD ["java", "-jar", "/app.jar"]
