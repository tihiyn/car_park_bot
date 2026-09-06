package com.tihiyn.car_park_bot;

import com.tihiyn.car_park_bot.dao.ManagerChatRepository;
import com.tihiyn.car_park_bot.dao.model.ManagerChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Рассылает алерты Grafana всем менеджерам, вошедшим в бота через /login
 * (таблица {@code manager_chat}).
 */
@Component
public class AlertNotifier {
    private static final Logger log = LoggerFactory.getLogger(AlertNotifier.class);

    private static final int TG_LIMIT = 4096;
    private static final DateTimeFormatter TS =
        DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.of("Europe/Moscow"));
    /** Нулевое время Go: Grafana ставит его в endsAt у активных алертов. */
    private static final int GO_ZERO_YEAR = 1;

    private final TelegramClient client;
    private final ManagerChatRepository repository;
    private final long creatorId;

    public AlertNotifier(TelegramClient client,
                         ManagerChatRepository repository,
                         @Value("${bot.creator-id}") long creatorId) {
        this.client = client;
        this.repository = repository;
        this.creatorId = creatorId;
    }

    public void notify(GrafanaAlertPayload payload) throws TelegramApiException {
        List<GrafanaAlertPayload.Alert> alerts = payload.alerts();
        if (alerts == null || alerts.isEmpty()) {
            log.warn("Получен вебхук Grafana без единого алерта: receiver={}", payload.receiver());
            return;
        }
        // Список получателей резолвится один раз на весь вебхук, а не на каждый алерт:
        // Grafana присылает до maxAlerts штук в одном запросе.
        List<Long> recipients = recipients();
        for (GrafanaAlertPayload.Alert alert : alerts) {
            broadcast(recipients, format(alert, payload));
        }
        if (payload.truncatedAlerts() != null && payload.truncatedAlerts() > 0) {
            broadcast(recipients, "…и ещё %d алерт(ов) не поместились в уведомление"
                .formatted(payload.truncatedAlerts()));
        }
    }

    /**
     * Все менеджеры, вошедшие в систему через /login.
     * <p>
     * Если таблица пуста или недоступна, алерт уходит в чат администратора: уведомление,
     * которое некому доставить, хуже любого шума. Особенно это важно для алертов про БД —
     * они не должны теряться из-за проблем с самой БД (у бота она своя, bot_db, но
     * подстраховаться дешевле, чем разбираться потом, почему уведомление не пришло).
     */
    private List<Long> recipients() {
        try {
            List<Long> chatIds = repository.findAll().stream()
                .map(ManagerChat::getChatId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            if (!chatIds.isEmpty()) {
                return chatIds;
            }
            log.warn("В manager_chat нет ни одного чата, алерт уйдёт в чат администратора");
        } catch (DataAccessException e) {
            log.error("Не удалось прочитать manager_chat, алерт уйдёт в чат администратора", e);
        }
        return List.of(creatorId);
    }

    /**
     * Заблокировавший бота менеджер не должен мешать доставке остальным, поэтому ошибки
     * копятся, а не прерывают рассылку. Исключение пробрасывается только если не дошло
     * вообще ни до кого — тогда контроллер вернёт 500 и Grafana зафиксирует сбой канала.
     */
    private void broadcast(List<Long> recipients, String text) throws TelegramApiException {
        int delivered = 0;
        TelegramApiException failure = null;
        for (Long chatId : recipients) {
            try {
                send(chatId, text);
                delivered++;
            } catch (TelegramApiException e) {
                failure = e;
                log.error("Не удалось отправить алерт в чат {}", chatId, e);
            }
        }
        if (delivered == 0 && failure != null) {
            throw failure;
        }
    }

    private void send(long chatId, String text) throws TelegramApiException {
        client.execute(SendMessage.builder()
            .chatId(String.valueOf(chatId))
            .text(text.length() > TG_LIMIT ? text.substring(0, TG_LIMIT - 1) + "…" : text)
            .parseMode("HTML")
            .build());
    }

    private String format(GrafanaAlertPayload.Alert alert, GrafanaAlertPayload payload) {
        Map<String, String> labels = orEmpty(alert.labels());
        Map<String, String> annotations = orEmpty(alert.annotations());

        String status = alert.status() != null ? alert.status() : payload.status();
        String severity = labels.getOrDefault("severity", "unknown");
        String alertName = labels.getOrDefault("alertname", payload.title() != null ? payload.title() : "Алерт");

        StringBuilder sb = new StringBuilder();
        sb.append(emoji(status, severity)).append(' ')
            .append(escape(String.valueOf(status).toUpperCase())).append(" · ").append(escape(severity)).append('\n')
            .append("<b>").append(escape(alertName)).append("</b>");

        appendLine(sb, annotations.get("summary"));
        appendLine(sb, annotations.get("description"));
        appendLine(sb, annotations.get("runbook"));

        if (alert.startsAt() != null) {
            sb.append("\nНачалось: ").append(TS.format(alert.startsAt())).append(" MSK");
        }
        if (isRealTimestamp(alert.endsAt())) {
            sb.append("\nЗавершилось: ").append(TS.format(alert.endsAt())).append(" MSK");
        }

        String values = formatValues(alert.values());
        if (!values.isEmpty()) {
            sb.append("\nЗначения: ").append(escape(values));
        }

        String links = formatLinks(alert);
        if (!links.isEmpty()) {
            sb.append('\n').append(links);
        }
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append('\n').append(escape(value));
        }
    }

    private static String formatValues(Map<String, Double> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        new LinkedHashMap<>(values).forEach((key, value) -> joiner.add(key + "=" + value));
        return joiner.toString();
    }

    private static String formatLinks(GrafanaAlertPayload.Alert alert) {
        StringJoiner joiner = new StringJoiner(" · ");
        addLink(joiner, "Правило", alert.generatorURL());
        addLink(joiner, "Дашборд", alert.dashboardURL());
        addLink(joiner, "Заглушить", alert.silenceURL());
        return joiner.toString();
    }

    private static void addLink(StringJoiner joiner, String label, String url) {
        if (url != null && !url.isBlank()) {
            joiner.add("<a href=\"%s\">%s</a>".formatted(escape(url), label));
        }
    }

    /**
     * Grafana подставляет в endsAt нулевое время Go (0001-01-01T00:00:00Z), пока алерт активен.
     */
    private static boolean isRealTimestamp(OffsetDateTime ts) {
        return ts != null && ts.getYear() > GO_ZERO_YEAR;
    }

    private static String emoji(String status, String severity) {
        if ("resolved".equalsIgnoreCase(status)) {
            return "✅";
        }
        return switch (severity) {
            case "critical" -> "🔴";
            case "warning" -> "🟡";
            case "info" -> "🔵";
            default -> "⚪";
        };
    }

    /** HTML parse mode Telegram требует экранирования этих трёх символов. */
    private static String escape(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private static <K, V> Map<K, V> orEmpty(Map<K, V> map) {
        return map == null ? Map.of() : map;
    }
}
