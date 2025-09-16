package org.example.service;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.filter.MessageAnalyzer;
import org.springframework.stereotype.Service;

@Service
public class MessageService extends ListenerAdapter {

    //private final CommandFormatter commandFormatter;
    private final Logger log = LogManager.getLogger(MessageService.class);
    private MessageAnalyzer analyzer;

    public MessageService(CommandFormatter commandFormatter, MessageAnalyzer analyzer) {
        //this.commandFormatter = commandFormatter;
        this.analyzer = analyzer;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        analyzer.checkMessageForViolation(event);
    }
}