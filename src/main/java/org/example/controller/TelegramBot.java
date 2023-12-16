package org.example.controller;

import org.example.entity.UserEntity;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    // Ім'я телеграм-бота, отриманий з application.yml
    @Value("${telegram.username}")
    private String username;

    // Chat ID телеграм-оператора, отриманий з application.yml
    private final String operatorChatId;

    // Map для зберігання даних користувачів за їхнім chat ID
    private final Map<Long, UserEntity> ticketDataMap = new HashMap<>();

    // Репозиторій для взаємодії з базою даних для збереження даних користувачів
    private final UserRepository repository;

    // Константи для префіксів команд
    private static final String START = "/start";
    private static final String NAME = "name: ";
    private static final String PHONE = "phone: ";
    private static final String RESERVE = "reserve: ";


    // Метод для обробки повідомлень від користувачів
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            String messageText = message.getText();
            String username = message.getChat().getFirstName();

            // Перевірка команди та виконання відповідних дій
            if (messageText.startsWith(START)) {
                startCommand(chatId, username);
            } else if (messageText.startsWith(NAME)) {
                claimName(chatId, messageText);
            } else if (messageText.startsWith(PHONE)) {
                claimPhone(chatId, messageText);
            } else if (messageText.startsWith(RESERVE)) {
                claimReservedAt(chatId, messageText);
            } else {
                sendResponseMessage(chatId, "Cannot recognize your request");
            }
        }
    }

    // Метод для обробки команди "name:"
    public void claimName(Long chatId, String name) {
        UserEntity userEntity = ticketDataMap.getOrDefault(chatId, new UserEntity());

        // Розбиття рядка імені та встановлення його в UserEntity
        String[] parser = name.split(" ");
        String firstName = parser[1];
        String lastName  = parser[2];

        if (firstName == null || lastName == null) {
            sendResponseMessage(chatId, "Ім'я було введено неправильно");
        }

        // Встановлення імені в UserEntity
        userEntity.setFullName(firstName + " " + lastName);

        // Оновлення UserEntity в map
        ticketDataMap.put(chatId, userEntity);

        checkUserData(chatId, userEntity);
    }

    // Метод для обробки команди "phone:"
    public void claimPhone(Long chatId, String phoneNumber) {
        String[] parser = phoneNumber.split(" ");
        UserEntity userEntity = ticketDataMap.getOrDefault(chatId, new UserEntity());

        // Розбиття рядка номера телефону
        String phone = parser[1];

        if (phone.length() != 10) {
            sendResponseMessage(chatId, "Номер телефону має складатися 10 символів");
            return;
        } else if (!phone.startsWith("380")) {
            sendResponseMessage(chatId, "Номер телефону має починатися на 380");
            return;
        }

        userEntity.setPhoneNumber(phone);

        // Оновлення UserEntity в map
        ticketDataMap.put(chatId, userEntity);

        checkUserData(chatId, userEntity);
    }

    // Метод для обробки команди "reserve:"
    public void claimReservedAt(Long chatId, String date) {
        UserEntity userEntity = ticketDataMap.getOrDefault(chatId, new UserEntity());

        // Розбиття рядка дати та часу та встановлення їх в UserEntity
        String[] parser = date.split(" ");

        // Перевірка правильності введення дати
        if (parser.length != 3) {
            dataWasWrittenIncorrectly(chatId);
        }

        String[] helpParser = parser[1].split("/");
        if (helpParser.length != 3) {
            dataWasWrittenIncorrectly(chatId);
        }

        String[] timeParser = parser[2].split(":");

        if (timeParser.length != 2) {
            dataWasWrittenIncorrectly(chatId);
        }

        Integer dayOfMonth = Integer.parseInt(helpParser[0]);
        Integer month = Integer.parseInt(helpParser[1]);
        Integer year = Integer.parseInt(helpParser[2]);

        Integer hour = Integer.parseInt(timeParser[0]);
        Integer  minute = Integer.parseInt(timeParser[1]);


        userEntity.setReservedAt(LocalDateTime.of(year, month, dayOfMonth, hour, minute));
        ticketDataMap.put(chatId, userEntity);

        checkUserData(chatId, userEntity);
    }

    // Метод для перевірки, чи введено всі дані користувача
    public void checkUserData(Long chatId, UserEntity userEntity) {
        String message = "Вам залишилося ввести:";

        sendResponseMessage(chatId, message);

        if (userEntity.getFullName() == null) {
            sendResponseMessage(chatId, "Ваше повне ім'я (name: )");
        }

        if (userEntity.getPhoneNumber() == null) {
            sendResponseMessage(chatId, "Номер телефона (phone: )");
        }

        if (userEntity.getReservedAt() == null) {
            sendResponseMessage(chatId, "Дату бронювання (reserve: )");
        }

        if (userEntity.getFullName() != null && userEntity.getReservedAt() != null && userEntity.getPhoneNumber() != null) {
            UUID ticket = UUID.randomUUID();
            userEntity.setTicket(ticket);
            repository.save(userEntity);

            sendResponseMessage(chatId, "Вітаю, користувач був успішно створений. Ось ваш квиток: " + ticket);

            sendOperatorChatMessage("Новий користувач створений. ID чату: %d".formatted(chatId));
        }

    }

    // Метод для обробки команди "/start"
    public void startCommand(Long chatId, String username) {
        String greeting = """
                Привіт, %s! Я ваш особистий бот-помічник. Щоб почати, виконайте ці команди:\s

                name: Ім'я прізвище - Встановлє Ваше ім'я та прізвище
                phone: XXXYYYYYYY - Встановлює Ваш номер телефону
                reserve: dd/mm/yyyy hh:mm - Встановлює дату бронювання квитка
                Дуже важливо дотримуватися формату вводу даних
                """;

        sendResponseMessage(chatId, greeting.formatted(username));
    }


    // Метод для відправлення повідомлення оператору
    private void sendOperatorChatMessage(String message) {
        SendMessage operatorMessage = new SendMessage();
        operatorMessage.setChatId(operatorChatId);
        operatorMessage.setText(message);

        try {
            execute(operatorMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для відправлення відповіді користувачеві
    public void sendResponseMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    // Метод для повідомлення про неправильно введену дату
    public void dataWasWrittenIncorrectly(Long chatId) {
        sendResponseMessage(chatId, "Дата була введена неправильно");
    }

    // метод для отримання імені телеграм-бота
    @Override
    public String getBotUsername() {
        return username;
    }

    public TelegramBot(@Value("${telegram.secretToken}") String botToken, @Value("${telegram.operatorChatId}") String operatorChatId, UserRepository repository) {
        super(botToken);
        this.operatorChatId = operatorChatId;
        this.repository = repository;
    }
}
