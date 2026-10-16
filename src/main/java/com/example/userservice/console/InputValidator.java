package com.example.userservice.console;

public final class InputValidator {
    private InputValidator() {
    }

    public static String validateName(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        String trimmed = input.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Name is too long (max 255 characters)");
        }
        return trimmed;
    }

    public static String validateEmail(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Email must not be empty");
        }
        String trimmed = input.trim().toLowerCase();
        if (!trimmed.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException(
                    "Invalid email format (example: ivan@mail.com)");
        }
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Email is too long (max 255 characters)");
        }
        return trimmed;
    }

    public static Integer validateAge(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        try {
            int age = Integer.parseInt(input.trim());
            if (age < 0 || age > 150) {
                throw new IllegalArgumentException("Age must be between 0 and 150");
            }
            return age;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Age must be a number");
        }
    }

    public static Long validateId(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Id must not be empty");
        }
        try {
            long id = Long.parseLong(input.trim());
            if (id <= 0) {
                throw new IllegalArgumentException("Id must be positive");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id must be a number");
        }
    }
}
