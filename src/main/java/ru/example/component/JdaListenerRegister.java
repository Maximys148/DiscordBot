package ru.example.component;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.EventListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Добавляет слушателей в jda(Чтобы jda был бином без создания зацикливания)
 */
@Component
public class JdaListenerRegister {

    private final JDA jda;
    private final ApplicationContext applicationContext;

    public JdaListenerRegister(JDA jda, ApplicationContext applicationContext) {
        this.jda = jda;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void registerAllListeners() {
        // Получаем все бины, которые реализуют EventListener
        Map<String, EventListener> listeners = applicationContext.getBeansOfType(EventListener.class);
        
        listeners.values().forEach(jda::addEventListener);
    }
}
