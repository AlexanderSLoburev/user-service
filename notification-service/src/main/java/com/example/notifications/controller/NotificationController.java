package com.example.notifications.controller;

import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.notification.Operation;
import com.example.notifications.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Void> send(@Valid @RequestBody NotificationRequest request) {
        Operation operation = Operation.from(request.operation())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown operation: " + request.operation()));
        notificationService.send(operation, request.email());
        return ResponseEntity.accepted().build();
    }
}