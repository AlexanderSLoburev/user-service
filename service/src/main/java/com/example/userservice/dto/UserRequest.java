package com.example.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Name must not be empty")
        @Size(max = 255, message = "Name is too long")
        String name,

        @NotBlank(message = "Email must not be empty")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email is too long")
        String email,

        @Min(value = 0, message = "Age must be between 0 and 150")
        @Max(value = 150, message = "Age must be between 0 and 150")
        Integer age
) {}