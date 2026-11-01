package com.example.userservice.dto;

import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}