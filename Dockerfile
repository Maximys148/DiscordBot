FROM azul/zulu-openjdk:17-jre

# Установка UTF-8 локалей и необходимых пакетов
RUN apt-get update && apt-get install -y \
    locales \
    && sed -i -e 's/# ru_RU.UTF-8 UTF-8/ru_RU.UTF-8 UTF-8/' /etc/locale.gen \
    && locale-gen \
    && rm -rf /var/lib/apt/lists/*

# Установка переменных окружения для русского языка с UTF-8
ENV LANG=ru_RU.UTF-8 \
    LANGUAGE=ru_RU:ru \
    LC_ALL=ru_RU.UTF-8

# ... остальная часть вашего Dockerfile без изменений ...
COPY models/vosk-model-small-ru-0.22 /app/models/vosk-model-small-ru-0.22
COPY models/vosk-model-ru-0.10 /app/models/vosk-model-ru-0.10

RUN ls -la /app/models/ && \
    ls -la /app/models/vosk-model-small-ru-0.22/ || echo "small missing"

COPY target/discord-bot-1.0.0.jar /app.jar
WORKDIR /app
CMD ["java", "-jar", "/app.jar"]
