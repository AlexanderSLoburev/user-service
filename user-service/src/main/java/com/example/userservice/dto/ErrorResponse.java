package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Стандартное тело ошибки API")
public record ErrorResponse(
        @Schema(description = "HTTP-статус", example = "400")
        int status,

        @Schema(description = "Человекочитаемое сообщение", example = "Validation failed")
        String message,

        @Schema(description = "Список ошибок по полям (только для 400)")
        List<FieldError> errors
) {
    @Schema(description = "Ошибка валидации конкретного поля")
    public record FieldError(
            @Schema(description = "Имя поля", example = "email")
            String field,
            @Schema(description = "Сообщение об ошибке", example = "invalid email format")
            String message
    ) {}
}