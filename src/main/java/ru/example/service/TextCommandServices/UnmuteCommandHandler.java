package ru.example.service.TextCommandServices;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import ru.example.model.CommandInfo;

/**
 * Отвечает за размут (включение микрофона) у пользователя в голосовом канале
 */
@Service
public class UnmuteCommandHandler {

    private final Guild guild;
    private final Transliterator transliterator;
    private final Logger log = LogManager.getLogger(UnmuteCommandHandler.class);

    public UnmuteCommandHandler(Guild guild) {
        this.guild = guild;
        this.transliterator = Transliterator.getInstance("Latin-Cyrillic");
    }

    public void execute(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user").getAsUser();
        Member targetMember = guild.getMember(targetUser);
        if (targetMember == null) {
            log.warn("Пользователь не найден в гильдии");
            return;
        }
        if (!targetMember.getVoiceState().inAudioChannel()) {
            log.warn("Пользователь не находится в голосовом канале");
            return;
        }
        targetMember.mute(false).queue(
                success -> event.reply("Пользователь " + targetUser.getName() + " размьючен").queue(),
                error -> {
                    log.error("Ошибка при размьюте пользователя", error);
                    event.reply("Не удалось размутить пользователя").queue();
                }
        );
    }

}
