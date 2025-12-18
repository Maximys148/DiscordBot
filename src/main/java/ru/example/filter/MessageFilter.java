package ru.example.filter;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.example.service.ViolationTrackerService;
import org.springframework.stereotype.Service;

import static ru.example.constant.MessageConstant.BAN_WORD;

/**
 * Фильтрует сообщение на нарушение правил
 */
@Service
public class MessageFilter {

    private final ViolationTrackerService violationTrackerService;
    public MessageFilter(ViolationTrackerService violationTrackerService) {
        this.violationTrackerService = violationTrackerService;
    }

    public void checkMessageForViolation(MessageReceivedEvent event) {
        checkForBannedWords(event);
    }

    private void checkForBannedWords(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String author = message.getAuthor().getAsMention();
        String message1 = message.getContentRaw().toLowerCase();
        for (String BAN_WORD: BAN_WORD) {
            if (message1.contains(BAN_WORD)) {
                String violationMessage = String.format(
                                "%s, еще раз %s спизданёшь в мут отлетишь",
                        author,
                        BAN_WORD,
                        message.getContentRaw()
                );

                Member member = message.getMember();
                String id = member.getId();
                violationTrackerService.addViolation(member);
                int violationCount = violationTrackerService.getViolationCount(id);
                if(violationCount >= 2) {
                    violationTrackerService.muteUser(event, member);
                    String violationMessage1 = String.format(
                            "%s, пиздуй в мут",
                            author
                    );
                    event.getChannel().sendMessage(violationMessage1).queue();
                    break;
                }else{
                    event.getChannel().sendMessage(violationMessage).queue();
                    break;
                }
            }
        }
    }
}
