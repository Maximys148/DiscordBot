package ru.example.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.example.filter.MessageFilter;
import org.springframework.stereotype.Service;

@Service
public class MessageService extends ListenerAdapter {

    private final Logger log = LogManager.getLogger(MessageService.class);
    private MessageFilter analyzer;

    public MessageService(MessageFilter analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;
        analyzer.checkMessageForViolation(event);
    }
}