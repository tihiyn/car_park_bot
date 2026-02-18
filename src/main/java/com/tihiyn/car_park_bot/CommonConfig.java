package com.tihiyn.car_park_bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class CommonConfig {
    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient("8547551582:AAHP6PWlWdYluyaJA30EXPlTgCyVFfAKFzQ");
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
