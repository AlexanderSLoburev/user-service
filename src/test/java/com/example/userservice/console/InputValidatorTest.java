package com.example.userservice.console;

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

    @Test
    void nullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName(null));
    }

    @Test
    void nameAtMaxLengthIsValid() {
        String maxName = "a".repeat(255);
        assertEquals(maxName, InputValidator.validateName(maxName));
    }

    @Test
    void nullEmailThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateEmail(null));
    }

    @Test
    void blankEmailThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateEmail("   "));
    }

    @Test
    void emailWithoutTldThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateEmail("ivan@example"));
    }

    @Test
    void emailAtMaxLengthIsValid() {
        String maxEmail = "a".repeat(250) + "@a.co";
        assertEquals(maxEmail, InputValidator.validateEmail(maxEmail));
    }

    @Test
    void tooLongEmailThrows() {
        String longEmail = "a".repeat(251) + "@a.co";
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateEmail(longEmail));
    }

    @Test
    void nullAgeBecomesNull() {
        assertNull(InputValidator.validateAge(null));
    }

    @Test
    void whitespaceAgeBecomesNull() {
        assertNull(InputValidator.validateAge("   "));
    }

    @Test
    void zeroAgeIsValid() {
        assertEquals(0, InputValidator.validateAge("0"));
    }

    @Test
    void maxAgeIsValid() {
        assertEquals(150, InputValidator.validateAge("150"));
    }

    @Test
    void negativeAgeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateAge("-1"));
    }

    @Test
    void nullIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId(null));
    }

    @Test
    void blankIdWithSpacesThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId("   "));
    }

    @Test
    void zeroIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId("0"));
    }

    @Test
    void nonNumericIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateId("abc"));
    }

    @Test
    void idIsTrimmed() {
        assertEquals(42L, InputValidator.validateId("  42  "));
    }
}
