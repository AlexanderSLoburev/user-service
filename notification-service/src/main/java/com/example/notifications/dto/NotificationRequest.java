package com.example.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Запрос на отправку email-уведомления")
public record NotificationRequest(
        @Schema(
                description = "Тип операции",
                example = "CREATED",
                allowableValues = {"CREATED", "DELETED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "operation is mandatory")
        @Pattern(regexp = "CREATED|DELETED",
                message = "operation must be CREATED or DELETED")
        String operation,

        @Schema(
                description = "Email получателя",
                example = "ivan@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email is mandatory")
        @Email(message = "invalid email format")
        String email
) {}