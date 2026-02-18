package com.tihiyn.car_park_bot;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class Bot {
    public static void main(String[] args) {
        try {
            String token = "8547551582:AAHP6PWlWdYluyaJA30EXPlTgCyVFfAKFzQ";
            TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication();
            TelegramClient client = new OkHttpTelegramClient(token);
            ReportBot bot = new ReportBot(client, "car_park_report_bot");
            bot.onRegister();
            app.registerBot(token, bot);
        } catch (TelegramApiException e) {
            throw new RuntimeException("Initialize error");
        }
    }
}
