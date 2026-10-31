package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record
    UserCreateRequest(@NotBlank(message = "name is mandatory") String name,
                      @NotBlank(message = "email is mandatory")
                      @Email(message = "invalid email format") String email,
                      @NotNull(message = "age is mandatory")
                      @Positive Integer age) {
}