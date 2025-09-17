package org.example.filter;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.example.service.ViolationTrackerService;
import org.springframework.stereotype.Service;

import static org.example.constant.MessageConstant.BAN_WORD;

@Service
public class MessageAnalyzer {

    private final ViolationTrackerService violationTrackerService;
    public MessageAnalyzer(ViolationTrackerService violationTrackerService) {
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
                // Формируем сообщение о нарушении
                String violationMessage = String.format(
                                "%s, еще раз %s спизданёж в мут отлетишь",
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
                    // Формируем сообщение о муте
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
