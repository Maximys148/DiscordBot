package ru.example.service.AudioCommandHandlers;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.enums.AccessLevel;
import ru.example.model.CommandInfo;
import ru.example.service.PhoneticTransliterator;

/**
 * Отвечает за мут(выключение микрофона) у пользователя в голосовом канале
 */
// TODO повысить точность распознания никнейма
@Service
public class MuteCommandHandler {

    private final Guild guild;
    private final Transliterator transliterator;
    private final Logger log = LogManager.getLogger(MuteCommandHandler.class);

    public MuteCommandHandler(Guild guild) {
        this.guild = guild;
        this.transliterator = Transliterator.getInstance("Latin-Cyrillic");
    }

    public void execute(CommandInfo commandInfo, User issuer) {
        String targetUsername = commandInfo.getTargetUser();
        if (targetUsername == null || targetUsername.isEmpty()) {
            log.warn("[Не указан пользователь для мута]");
            return;
        }

        String targetUsernameCyr = transliterator.transliterate(targetUsername).toLowerCase();

        Member targetMember = guild.getMembers().stream()
                .filter(m -> {
                    String memberNameCyr = PhoneticTransliterator.transliterate(m.getEffectiveName()).toLowerCase();
                    log.info("Сравниваем пользователя {} с запросом {}]", memberNameCyr, targetUsernameCyr);
                    return memberNameCyr.contains(targetUsernameCyr);
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if(targetMember != null & targetMember.getRoles().contains(AccessLevel.ADMIN)) {
            log.warn("Не могу замутить {} с ролью {}", targetMember.getEffectiveName(), targetMember.getRoles().get(0).getName());
        }
        guild.mute(targetMember, true).queue(
                success -> log.info("Пользователь {} замьючен по приказу {}", targetMember.getEffectiveName(), issuer),
                error -> log.error("Ошибка при муте пользователя {}: {}", targetMember.getEffectiveName(), error.getMessage())
        );
    }


}
