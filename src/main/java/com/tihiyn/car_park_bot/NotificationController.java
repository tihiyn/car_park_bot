package com.tihiyn.car_park_bot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot")
public class NotificationController {
    private final ReportBot rb;

    public NotificationController(ReportBot rb) {
        this.rb = rb;
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notify(@RequestBody Notification n) {
        rb.sendNotification(n);
        return ResponseEntity.noContent().build();
    }
}
