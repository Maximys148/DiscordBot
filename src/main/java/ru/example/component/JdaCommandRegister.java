package ru.example.component;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class JdaCommandRegister {
    private final JDA jda;
    private final List<SlashCommandData> globalCommands;

    public JdaCommandRegister(JDA jda, List<SlashCommandData> globalCommands) {
        this.jda = jda;
        this.globalCommands = globalCommands;
    }

    @PostConstruct
    public void registerAllCommands() {
        jda.updateCommands().addCommands(globalCommands).queue();
    }
}
