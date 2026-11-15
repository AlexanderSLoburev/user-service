package com.example.notifications.controller;

import com.example.notifications.dto.ErrorResponse;
import com.example.notifications.dto.NotificationRequest;
import com.example.notifications.notification.Operation;
import com.example.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Прямая отправка email-уведомлений")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Отправить уведомление",
            description = "Отправляет email через SMTP в зависимости от операции. "
                    + "CREATED — письмо о создании аккаунта, DELETED — письмо об удалении.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Запрос принят, письмо отправлено"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидное тело запроса (operation или email)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> send(@Valid @RequestBody NotificationRequest request) {
        Operation operation = Operation.from(request.operation())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown operation: " + request.operation()));
        notificationService.send(operation, request.email());
        return ResponseEntity.accepted().build();
    }
}