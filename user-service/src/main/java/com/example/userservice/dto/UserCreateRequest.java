package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Запрос на создание пользователя")
public record UserCreateRequest(
        @Schema(description = "Имя пользователя", example = "Иван", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name is mandatory")
        String name,

        @Schema(description = "Email пользователя (должен быть уникальным)",
                example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email is mandatory")
        @Email(message = "invalid email format")
        String email,

        @Schema(description = "Возраст пользователя (положительное число)",
                example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "age is mandatory")
        @Positive
        Integer age
) {}