package com.example.notifications.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotificationRequest(
        @NotBlank(message = "operation is mandatory")
        @Pattern(regexp = "CREATED|DELETED",
                message = "operation must be CREATED or DELETED")
        String operation,

        @NotBlank(message = "email is mandatory")
        @Email(message = "invalid email format")
        String email
) {}