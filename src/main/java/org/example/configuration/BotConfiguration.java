package org.example.configuration;

import org.example.controller.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
//
@Configuration
public class BotConfiguration {

    // Конфігурація для створення та реєстрації біна TelegramBotsApi
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot telegramBot) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        // Реєстрація телеграм-бота в TelegramBotsApi
        telegramBotsApi.registerBot(telegramBot);


        return telegramBotsApi;
    }
}
