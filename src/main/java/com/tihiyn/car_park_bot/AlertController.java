package com.tihiyn.car_park_bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/bot")
public class AlertController {
    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final AlertNotifier notifier;
    private final byte[] expectedAuth;

    public AlertController(AlertNotifier notifier, @Value("${alerts.webhook-token}") String token) {
        this.notifier = notifier;
        this.expectedAuth = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/alerts")
    public ResponseEntity<Void> alerts(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
        @RequestBody GrafanaAlertPayload payload) {
        if (!authorized(authorization)) {
            log.warn("Отклонён вебхук алертов с неверным токеном");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            notifier.notify(payload);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить алерт в Telegram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.accepted().build();
    }

    private boolean authorized(String authorization) {
        return authorization != null
            && MessageDigest.isEqual(authorization.getBytes(StandardCharsets.UTF_8), expectedAuth);
    }
}
