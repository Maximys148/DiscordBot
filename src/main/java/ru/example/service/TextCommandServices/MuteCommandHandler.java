package ru.example.service.TextCommandServices;

import com.ibm.icu.text.Transliterator;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Отвечает за мут (выключение микрофона) у пользователя в голосовом канале
 */
@Service("textMuteCommandHandler")
public class MuteCommandHandler {

    private final Guild guild;
    private final Transliterator transliterator;
    private final Logger log = LogManager.getLogger(MuteCommandHandler.class);

    public MuteCommandHandler(Guild guild) {
        this.guild = guild;
        this.transliterator = Transliterator.getInstance("Latin-Cyrillic");
    }

    public void execute(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user").getAsUser();
        Member targetMember = guild.getMember(targetUser);
        
        if (targetMember == null) {
            log.warn("Пользователь {} не найден в гильдии", targetUser.getName());
            event.reply("Пользователь не найден в гильдии").setEphemeral(true).queue();
            return;
        }
        
        if (!targetMember.getVoiceState().inAudioChannel()) {
            log.warn("Пользователь {} не находится в голосовом канале", targetUser.getName());
            event.reply("Пользователь не находится в голосовом канале").setEphemeral(true).queue();
            return;
        }
        
        // ✅ МУТИМ пользователя (выключаем микрофон)
        targetMember.mute(true).queue(
            success -> {
                log.info("Пользователь {} замучен в голосовом канале", targetUser.getName());
                event.reply("Пользователь **" + targetUser.getName() + "** замучен (микрофон выключен)").queue();
            },
            error -> {
                log.error("Ошибка при муте пользователя {}: {}", targetUser.getName(), error.getMessage());
                event.reply(" Не удалось замутить пользователя **" + targetUser.getName() + "**").setEphemeral(true).queue();
            }
        );
    }
}
