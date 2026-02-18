package com.tihiyn.car_park_bot;

import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.abilitybots.api.objects.ReplyFlow;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId;

public class ReportBot extends AbilityBot {
    private final Map<Long, Credentials> users = new ConcurrentHashMap<>();

    public ReportBot(TelegramClient telegramClient, String botUsername) {
        super(telegramClient, botUsername);
    }

    @Override
    public long creatorId() {
        return 715624734;
    }

    public ReplyFlow login() {
        return ReplyFlow.builder(db)
            .action((bot, upd) -> silent.send("Введите имя пользователя и пароль", getChatId(upd)))
            .onlyIf(hasMessageWith("/login"))
            .next(Reply.of(
                (bot, upd) -> {
                    String[] creds = upd.getMessage().getText().split(" ");
                    if (creds.length != 2) {
                        silent.send("Неверный формат. Повторите /login", getChatId(upd));
                        return;
                    }
                    HttpClient client = HttpClient.newHttpClient();
                    String form = "username=" + URLEncoder.encode(creds[0], StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(creds[1], StandardCharsets.UTF_8);
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/auth/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                    try {
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.headers().firstValue("location").get().contains("/auth/login?error")) {
                            silent.send("Неверное имя пользователя или пароль. Повторите /login", getChatId(upd));
                            return;
                        }
                        if (resp.statusCode() == 302) {
                            String jwt = resp.headers().firstValue("set-cookie").get().split(";")[0];
                            users.put(upd.getMessage().getChatId(), new Credentials(creds[0], creds[1], jwt));
                            silent.send("Вы успешно вошли!", getChatId(upd));
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException();
                    }
                },
                upd -> upd.hasMessage()
                    && !upd.getMessage().getText().equalsIgnoreCase("/login")
            ))
            .build();
    }

//    private boolean isCredsValid(Message msg) {
//        String[] creds = msg.getText().split(" ");
//        if (creds.length == 2) {
//            HttpClient client = HttpClient.newHttpClient();
//            String form = "username=" + URLEncoder.encode(creds[0], StandardCharsets.UTF_8)
//                + "&password=" + URLEncoder.encode(creds[1], StandardCharsets.UTF_8);
//            HttpRequest req = HttpRequest.newBuilder()
//                .uri(URI.create("http://localhost:8080/auth/login"))
//                .header("Content-Type", "application/x-www-form-urlencoded")
//                .POST(HttpRequest.BodyPublishers.ofString(form))
//                .build();
//            try {
//                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
//                if (resp.statusCode() == 302) {
//                    String jwt = resp.headers().firstValue("set-cookie").get().split(";")[0];
//                    users.put(msg.getChatId(), new Credentials(creds[0], creds[1], jwt));
//                    return true;
//                }
//            } catch (IOException | InterruptedException e) {
//                return false;
//            }
//        }
//        return false;
//    }

    public ReplyFlow dayReport() {
        return ReplyFlow.builder(db)
            .action((bot, upd) -> {
                if (!users.containsKey(upd.getMessage().getChatId())) {
                    silent.send("Вы не вошли в систему. Для входа введите /login", getChatId(upd));
                    return;
                }
                silent.send("Введите через пробел номер авто (БЦЦЦББ) и дату (ГГГГ-ММ-ДД)", getChatId(upd));
            })
            .onlyIf(hasMessageWith("/day_report"))
            .next(Reply.of(
                (bot, upd) -> {
                    String[] data = upd.getMessage().getText().split(" ");
                    String regNum = data[0];
                    LocalDate date = LocalDate.parse(data[1]);
                    ZonedDateTime since = ZonedDateTime.of(date, LocalTime.of(0, 0, 0), ZoneId.of("UTC"));
                    ZonedDateTime before = since.plusDays(1L);
                    String period = "day";

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/reports/vehicle/mileage?regNum=%s&period=%s&begin=%s&end=%s".formatted(regNum, period,
                            URLEncoder.encode(since.format(DateTimeFormatter.ISO_DATE_TIME), StandardCharsets.UTF_8),
                            URLEncoder.encode(before.format(DateTimeFormatter.ISO_DATE_TIME), StandardCharsets.UTF_8))))
                        .header("Cookie", users.get(upd.getMessage().getChatId()).jwt())
                        .GET()
                        .build();
                    try {
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        ObjectMapper om = new ObjectMapper();
                        VehicleMileageReport report = om.readValue(resp.body(), VehicleMileageReport.class);
                        String answer = "";
                        for (Map.Entry<String, Long> entry: report.getResult().entrySet()) {
                            answer = "Пробег авто с номером %s за %s составил %d км".formatted(regNum, entry.getKey().substring(0, entry.getKey().length() - 1), entry.getValue());
                        }
                        silent.send(answer, getChatId(upd));
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                upd -> upd.hasMessage()
                    && !upd.getMessage().getText().equalsIgnoreCase("/day_report"),
                upd -> users.containsKey(upd.getMessage().getChatId()))
            )
//            .next(Reply.of(
//                (bot, upd) -> {
//                    silent.send("Вы не вошли в систему. Для входа введите /login", getChatId(upd));
//                },
//                upd -> upd.hasMessage(),
//                hasMessageWith("/day_report"),
//                upd -> !users.containsKey(upd.getMessage().getChatId())
//            ))
            .build();
    }

    public ReplyFlow monthReport() {
        return ReplyFlow.builder(db)
            .action((bot, upd) -> {
                if (!users.containsKey(upd.getMessage().getChatId())) {
                    silent.send("Вы не вошли в систему. Для входа введите /login", getChatId(upd));
                    return;
                }
                silent.send("Введите через пробел номер авто (БЦЦЦББ) и дату (ГГГГ-ММ)", getChatId(upd));
            })
            .onlyIf(hasMessageWith("/month_report"))
            .next(Reply.of(
                (bot, upd) -> {
                    String[] data = upd.getMessage().getText().split(" ");
                    String regNum = data[0];
                    LocalDate date = LocalDate.parse(data[1] + "-01");
                    ZonedDateTime since = ZonedDateTime.of(date, LocalTime.of(0, 0, 0), ZoneId.of("UTC"));
                    ZonedDateTime before = since.plusMonths(1L).minusDays(1L);
                    String period = "month";

                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/reports/vehicle/mileage?regNum=%s&period=%s&begin=%s&end=%s".formatted(regNum, period,
                            URLEncoder.encode(since.format(DateTimeFormatter.ISO_DATE_TIME), StandardCharsets.UTF_8),
                            URLEncoder.encode(before.format(DateTimeFormatter.ISO_DATE_TIME), StandardCharsets.UTF_8))))
                        .header("Cookie", users.get(upd.getMessage().getChatId()).jwt())
                        .GET()
                        .build();
                    try {
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        ObjectMapper om = new ObjectMapper();
                        VehicleMileageReport report = om.readValue(resp.body(), VehicleMileageReport.class);
                        String answer = "";
                        for (Map.Entry<String, Long> entry: report.getResult().entrySet()) {
                            answer = "Пробег авто с номером %s за %s %dг. составил %d км".formatted(regNum, entry.getKey(), date.getYear(), entry.getValue());
                        }
                        silent.send(answer, getChatId(upd));
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                upd -> upd.hasMessage()
                    && !upd.getMessage().getText().equalsIgnoreCase("/month_report"),
                upd -> users.containsKey(upd.getMessage().getChatId()))
            )
            .build();
    }

    private Predicate<Update> hasMessageWith(String msg) {
        return upd -> upd.getMessage().getText().equalsIgnoreCase(msg);
    }
}
