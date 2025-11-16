package ru.example.service.TextCommandServices;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final Logger log = LogManager.getLogger(ProfileService.class);

    public void execute(SlashCommandInteractionEvent event) {
        String user = event.getUser().getAsMention();
        String joinDate = event.getMember().getTimeJoined()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));


        MessageCreateData message = new MessageCreateBuilder()
                .setContent("""
                        **👤 ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ**
                        
                        **Никнейм:** %s
                        **На сервере с:** %s
                        **Роль:** %s
                        """.formatted(
                        user,
                        joinDate,
                        getRoleName(event)
                ))
                .build();

        event.getHook().sendMessage(message).queue();
    }

    private String getRoleName(SlashCommandInteractionEvent event) {
        return event.getMember().getRoles().stream()
                .findFirst()
                .map(role -> role.getName())
                .orElse("Участник");
    }
}