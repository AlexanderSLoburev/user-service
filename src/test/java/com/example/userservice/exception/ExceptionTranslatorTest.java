package com.example.userservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class ExceptionTranslatorTest {

    @Test
    void constraintViolationOnEmailBecomesDuplicateEmailException() {
        ConstraintViolationException cve =
                new ConstraintViolationException("duplicate", null, "users_email_key");
        RuntimeException result = ExceptionTranslator.translate(cve);
        assertInstanceOf(DuplicateEmailException.class, result);
    }

    @Test
    void otherConstraintViolationBecomesDataAccessException() {
        ConstraintViolationException cve =
                new ConstraintViolationException("bad value", null, "users_age_check");
        RuntimeException result = ExceptionTranslator.translate(cve);
        assertInstanceOf(DataAccessException.class, result);
    }

    @Test
    void hibernateExceptionBecomesDataAccessException() {
        HibernateException hbex = new HibernateException("connection lost");
        RuntimeException result = ExceptionTranslator.translate(hbex);
        assertInstanceOf(DataAccessException.class, result);
        assertTrue(result.getMessage().contains("connection lost"));
    }

    @Test
    void userNotFoundHasIdInMessage() {
        UserNotFoundException ex = new UserNotFoundException(42L);
        assertTrue(ex.getMessage().contains("42"));
    }
}