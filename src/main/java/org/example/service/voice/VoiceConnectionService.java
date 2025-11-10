package org.example.service.voice;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class VoiceConnectionService {

    private final MyAudioReceiveHandler myAudioReceiveHandler;
    private final Map<String, String> guildListeningChannels = new ConcurrentHashMap<>();
    private final Logger log = LogManager.getLogger(VoiceConnectionService.class);

    public VoiceConnectionService(MyAudioReceiveHandler myAudioReceiveHandler) {
        this.myAudioReceiveHandler = myAudioReceiveHandler;
    }

    public void connectToVoiceChannel(Guild guild) {
        try {
            AudioManager audioManager = guild.getAudioManager();

            // Бот должен отправлять аудио, чтобы получать его
            audioManager.setSendingHandler(new AudioSendHandler() {
                private final byte[] silence = new byte[3840]; // 20ms тишины

                @Override
                public boolean canProvide() {
                    return true;
                }

                @Override
                public ByteBuffer provide20MsAudio() {
                    return ByteBuffer.wrap(silence);
                }

                @Override
                public boolean isOpus() {
                    return false; // Отправляем RAW PCM
                }
            });

            // Ищем подходящий голосовой канал
            AudioChannel targetChannel = findSuitableVoiceChannel(guild);

            if (targetChannel == null) {
                log.warn("Не найден подходящий голосовой канал на сервере: {}", guild.getName());
                return;
            }

            // Закрываем предыдущее соединение если есть
            if (audioManager.isConnected()) {
                audioManager.closeAudioConnection();
                Thread.sleep(100);
            }

            // Настраиваем аудио обработчик
            audioManager.setReceivingHandler(myAudioReceiveHandler);
            audioManager.openAudioConnection(targetChannel);

            // Сохраняем информацию о канале
            String defaultTextChannelId = getDefaultTextChannelId(guild);
            guildListeningChannels.put(guild.getId(), defaultTextChannelId);

            log.info("Бот подключен к голосовому каналу: {} на сервере: {}",
                    targetChannel.getName(), guild.getName());

        } catch (Exception e) {
            log.error("Ошибка подключения к голосовому каналу на сервере: {}", guild.getName(), e);
        }
    }

    private AudioChannel findSuitableVoiceChannel(Guild guild) {
        // Приоритет 2: Любой канал с пользователями
        List<VoiceChannel> voiceChannels = guild.getVoiceChannels();
        for (VoiceChannel channel : voiceChannels) {
            List<Member> members = channel.getMembers().stream()
                    .filter(member -> !member.getUser().isBot())
                    .collect(Collectors.toList());

            if (!members.isEmpty()) {
                return channel;
            }
        }

        // Приоритет 3: Первый доступный канал
        if (!voiceChannels.isEmpty()) {
            return voiceChannels.get(0);
        }

        return null;
    }

    private String getDefaultTextChannelId(Guild guild) {
        List<TextChannel> textChannels = guild.getTextChannels();

        if (!textChannels.isEmpty()) {
            Optional<TextChannel> generalChannel = textChannels.stream()
                    .filter(channel -> channel.getName().toLowerCase().contains("general") ||
                            channel.getName().toLowerCase().contains("основной") ||
                            channel.getName().toLowerCase().contains("общий"))
                    .findFirst();

            if (generalChannel.isPresent()) {
                return generalChannel.get().getId();
            }

            return textChannels.get(0).getId();
        }

        return null;
    }

    public void disconnectFromVoiceChannel(Guild guild) {
        try {
            AudioManager audioManager = guild.getAudioManager();

            if (audioManager.isConnected()) {
                audioManager.closeAudioConnection();
                audioManager.setReceivingHandler(null);

                guildListeningChannels.remove(guild.getId());

                log.info("Бот отключен от голосового канала на сервере: {}", guild.getName());
            }
        } catch (Exception e) {
            log.error("Ошибка отключения от голосового канала на сервере: {}", guild.getName(), e);
        }
    }

    public boolean isListeningInGuild(String guildId) {
        return guildListeningChannels.containsKey(guildId);
    }

    public String getTextChannelForGuild(String guildId) {
        return guildListeningChannels.get(guildId);
    }

    public Map<String, String> getActiveConnections() {
        return new HashMap<>(guildListeningChannels);
    }
}
