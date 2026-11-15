package com.example.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Тело ошибки API")
public record ErrorResponse(
        @Schema(description = "HTTP-статус", example = "400")
        int status,

        @Schema(description = "Сообщение", example = "Validation failed")
        String message,

        @Schema(description = "Ошибки по полям")
        List<FieldError> errors
) {
    @Schema(description = "Ошибка валидации поля")
    public record FieldError(
            @Schema(description = "Имя поля", example = "operation")
            String field,
            @Schema(description = "Сообщение", example = "operation must be CREATED or DELETED")
            String message
    ) {}
}