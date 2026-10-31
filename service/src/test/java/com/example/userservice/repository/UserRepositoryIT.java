package com.example.userservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.userservice.entity.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withInitScript("01-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void when_saveNewUser_then_idAndCreatedAtAreAssigned() {
        User user = new User("Ivan", "ivan@example.com", 30);

        assertNotNull(userRepository.save(user).getId());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void when_saveNewUser_then_createdAtIsCloseToNow() {
        User user = new User("Ivan", "ivan@example.com", 30);
        userRepository.save(user);

        assertTrue(java.time.Duration.between(user.getCreatedAt(), LocalDateTime.now())
                .abs().toMinutes() < 1);
    }

    @Test
    void when_findByEmailExisting_then_returnsUser() {
        userRepository.save(new User("Ivan", "ivan@example.com", 30));

        var found = userRepository.findByEmail("ivan@example.com");

        assertTrue(found.isPresent());
        assertEquals("Ivan", found.get().getName());
    }

    @Test
    void when_findByEmailNonexistent_then_returnsEmpty() {
        assertTrue(userRepository.findByEmail("nobody@example.com").isEmpty());
    }

    @Test
    void when_existsByEmailExisting_then_returnsTrue() {
        userRepository.save(new User("Ivan", "ivan@example.com", 30));

        assertTrue(userRepository.existsByEmail("ivan@example.com"));
    }

    @Test
    void when_existsByEmailNonexistent_then_returnsFalse() {
        assertTrue(!userRepository.existsByEmail("nobody@example.com"));
    }

    @Test
    void when_saveTwoUsersWithSameEmail_then_throwsDataIntegrityViolation() {
        userRepository.saveAndFlush(new User("Ivan", "ivan@example.com", 30));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(new User("Petr", "ivan@example.com", 25)));
    }
}