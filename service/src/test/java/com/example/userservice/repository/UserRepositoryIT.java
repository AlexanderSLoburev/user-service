package com.example.userservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryIT {

  // The container closes the Testcontainers extension
  // (@Testcontainers/@Container) in afterAll
  @SuppressWarnings("resource")
  @Container
  @ServiceConnection
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withInitScript("01-schema.sql");

  @Autowired private UserRepository userRepository;

  @Test
  void when_save_new_user_then_id_and_created_at_are_assigned() {
    User saved = userRepository.save(new User("John", "john@example.com", 30));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void when_user_saved_then_exists_by_email_returns_true() {
    userRepository.save(new User("John", "john@example.com", 30));

    assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
  }

  @Test
  void when_email_unknown_then_exists_by_email_returns_false() {
    assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
  }

  @Test
  void
  when_user_keeps_own_email_then_exists_by_email_and_id_not_returns_false() {
    User saved = userRepository.save(new User("John", "john@example.com", 30));

    assertThat(
        userRepository.existsByEmailAndIdNot("john@example.com", saved.getId()))
        .isFalse();
  }

  @Test
  void
  when_email_owned_by_other_user_then_exists_by_email_and_id_not_returns_true() {
    User john = userRepository.save(new User("John", "john@example.com", 30));
    userRepository.save(new User("Alice", "alice@example.com", 25));

    assertThat(
        userRepository.existsByEmailAndIdNot("alice@example.com", john.getId()))
        .isTrue();
  }

  @Test
  void when_save_duplicate_email_then_data_integrity_violation_is_thrown() {
    userRepository.saveAndFlush(new User("John", "john@example.com", 30));

    assertThatThrownBy(()
                           -> userRepository.saveAndFlush(
                               new User("John Clone", "john@example.com", 31)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}