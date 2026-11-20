package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Запрос на обновление пользователя")
public record UserUpdateRequest(
        @Schema(description = "Новое имя пользователя", example = "Иван Петров",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name is mandatory")
        String name,

        @Schema(description = "Новый email пользователя (должен быть уникальным)",
                example = "ivan.petrov@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email is mandatory")
        @Email(message = "invalid email format")
        String email,

        @Schema(description = "Новый возраст пользователя", example = "31",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "age is mandatory")
        @Positive
        Integer age
) {}