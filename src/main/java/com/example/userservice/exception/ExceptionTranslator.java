package com.example.userservice.exception;

import jakarta.persistence.PersistenceException;
import org.hibernate.HibernateException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionTranslator {

    private static final Logger log = LoggerFactory.getLogger(ExceptionTranslator.class);

    private ExceptionTranslator() {
    }

    public static RuntimeException translate(Exception e) {
        log.error("Database error: {}", e.getMessage(), e);

        if (e instanceof ConstraintViolationException cve) {
            String constraint = cve.getConstraintName();
            if (constraint != null && constraint.toLowerCase().contains("email")) {
                return new DuplicateEmailException("User with this email already exists", e);
            }
            return new DataAccessException("Constraint violation: " + constraint, e);
        }

        if (e instanceof HibernateException || e instanceof PersistenceException) {
            return new DataAccessException("Database operation failed: " + e.getMessage(), e);
        }

        return new DataAccessException("Unexpected data access error: " + e.getMessage(), e);
    }
}