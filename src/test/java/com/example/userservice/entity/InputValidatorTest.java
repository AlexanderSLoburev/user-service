package com.example.userservice.entity;

import com.example.userservice.console.InputValidator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
public class InputValidatorTest {
    @Test
    void nameIsTrimmed() {
        assertEquals("Ivan", InputValidator.validateName("  Ivan  "));
    }

    @Test
    void blankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName("   "));
    }

    @Test
    void tooLongNameThrows() {
        String longName = "a".repeat(256);
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName(longName));
    }

    @Test
    void emailIsNormalized() {
        assertEquals("ivan@example.com",
                InputValidator.validateEmail("Ivan@Example.COM"));
    }

    @Test
    void malformedEmailThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateEmail("not-an-email"));
    }

    @Test
    void blankAgeBecomesNull() {
        assertNull(InputValidator.validateAge(""));
    }

    @Test
    void ageOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateAge("200"));
    }

    @Test
    void nonNumericAgeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateAge("abc"));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId(""));
    }

    @Test
    void nonPositiveIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId("-5"));
    }

    @Test
    void validIdIsParsed() {
        assertEquals(42L, InputValidator.validateId("42"));
    }
}
