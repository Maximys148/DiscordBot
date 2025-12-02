package ru.example.config;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.example.enums.VoiceCommandType;
import ru.example.service.PhoneticTransliterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.example.enums.VoiceCommandType.*;

@Configuration
public class BotConfig {

    @Value("${guild.id}")
    private String guildId;
    @Value("${transliterator.CYRILLIC_TO_LATIN}")
    public String CYRILLIC_TO_LATIN;
    private final Logger log = LogManager.getLogger(BotConfig.class);


    @Bean
    public Guild guild(JDA jda) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException("Гильдия с ID " + guildId + " не найдена");
        }
        return guild;
    }

    @Bean
    public String botName(JDA jda) {
        Transliterator toLatinTrans = Transliterator.getInstance(CYRILLIC_TO_LATIN);
        String botNameDiscord = jda.getSelfUser().getName();
        String botName = PhoneticTransliterator.transliterate(botNameDiscord);
        log.info("Имя бота на сервере - {}", botNameDiscord);
        log.info("Как его поняла программа имя бота программа - {}", botName.toLowerCase());
        return botName.toLowerCase();
    }

    @Bean
    @Qualifier("audioCommands")
    public Map<List<String>, VoiceCommandType> audioCommands() {
        Map<List<String>, VoiceCommandType> commands = new HashMap<>();

        commands.put(List.of("удали пользователя", "забань пользователя", "кикни пользователя"), BAN_USER);

        commands.put(List.of("за муть пользователя", "замуть пользователя", "отключи микрофон ",
                "выключи микрофон", "заглуши пользователя", "за муть", "заглуши", "замуть"), MUTE_USER);

        commands.put(List.of("включи звук", "включи микрофон", "размьють"), UNMUTE_USER);

        commands.put(List.of("уменьши громкость", "приглуши звук", "снизь громкость"), DECREASE_VOLUME);

        commands.put(List.of("увеличь громкость", "сделай громче", "повысь громкость"), INCREASE_VOLUME);

        commands.put(List.of("пригласить в голосовой", "подключи меня к голосовому каналу"), JOIN_VOICE_CHANNEL);
        commands.put(List.of("перезапустить бота", "рестарт бота", "перезагрузка бота"), RESTART_BOT);
        commands.put(List.of("показать список команд", "список команд", "что ты умеешь"), SHOW_COMMANDS);

        return commands;
    }

}