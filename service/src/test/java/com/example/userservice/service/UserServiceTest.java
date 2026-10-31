package com.example.userservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.userservice.dto.UserCreateRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.dto.UserUpdateRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;


/**
 * Unit tests for UserService: no database, no Docker, UserRepository is a mock.
 *
 * Isolation without @BeforeEach: MockitoExtension creates a FRESH mock for
 * every test, and strict stubs (the default) fail any test with an unused stub
 * — state leakage between tests is impossible by design.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final LocalDateTime CREATED_AT =
      LocalDateTime.of(2026, 1, 1, 0, 0, 0);

  @Mock private UserRepository userRepository;

  @InjectMocks private UserService userService;

  // ==================== create ====================

  @Test
  void when_create_validData_then_returnsPersistedResponse() {
    User saved = user(1L, "John", "john@example.com", 30);
    when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(saved);

    UserResponse response = userService.create(
        new UserCreateRequest("John", "john@example.com", 30));

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("John");
    assertThat(response.email()).isEqualTo("john@example.com");
    assertThat(response.age()).isEqualTo(30);
    assertThat(response.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void when_create_validData_then_repoReceivesNewTransientUser() {
    when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenReturn(user(1L, "John", "john@example.com", 30));

    userService.create(new UserCreateRequest("John", "john@example.com", 30));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(captor.capture());
    User passed = captor.getValue();
    assertThat(passed.getId()).isNull(); // transient: id is assigned by the DB
    assertThat(passed.getName()).isEqualTo("John");
    assertThat(passed.getEmail()).isEqualTo("john@example.com");
    assertThat(passed.getAge()).isEqualTo(30);
  }

  @Test
  void when_create_emailAlreadyTaken_then_DuplicateEmail_and_nothingSaved() {
    when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

    assertThatThrownBy(()
                           -> userService.create(new UserCreateRequest(
                               "John", "john@example.com", 30)))
        .isInstanceOf(DuplicateEmailException.class)
        .hasMessageContaining("john@example.com");

    verify(userRepository, never())
        .saveAndFlush(any(User.class)); // pre-check prevented the INSERT
  }

  @Test
  void when_create_repoThrowsDataIntegrityViolation_then_propagates() {
    // pre-check race path: the constraint fired in the DB — the service neither
    // swallows nor converts it; translating it to 409 is the
    // @RestControllerAdvice's job (task 4)
    when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("uq_users_email"));

    assertThatThrownBy(()
                           -> userService.create(new UserCreateRequest(
                               "John", "john@example.com", 30)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  // ==================== findById / findAll ====================

  @Test
  void when_findById_exists_then_returnsMappedResponse() {
    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user(1L, "John", "john@example.com", 30)));

    UserResponse response = userService.findById(1L);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("john@example.com");
    assertThat(response.age()).isEqualTo(30);
  }

  @Test
  void when_findById_missing_then_UserNotFoundException() {
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.findById(42L))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("42");
  }

  @Test
  void when_findAll_called_then_returnsMappedListInOrder() {
    when(userRepository.findAll(any(Sort.class)))
        .thenReturn(List.of(user(1L, "Alice", "alice@example.com", 25),
                            user(2L, "Bob", "bob@example.com", 40)));

    List<UserResponse> result = userService.findAll();

    assertThat(result)
        .extracting(UserResponse::id, UserResponse::name)
        .containsExactly(tuple(1L, "Alice"), tuple(2L, "Bob"));
  }

  // ==================== update ====================

  @Test
  void when_update_existing_then_modifiesLoadedEntity() {
    User loaded = user(1L, "John", "john@example.com", 30);
    when(userRepository.findById(1L)).thenReturn(Optional.of(loaded));
    when(userRepository.existsByEmailAndIdNot("john.p@example.com", 1L))
        .thenReturn(false);

    UserResponse response = userService.update(
        1L, new UserUpdateRequest("John P.", "john.p@example.com", 31));

    assertThat(loaded.getName())
        .isEqualTo("John P."); // find + modify: the entity from
    assertThat(loaded.getEmail())
        .isEqualTo("john.p@example.com"); // findById is mutated
    assertThat(loaded.getAge()).isEqualTo(31);
    assertThat(response.name())
        .isEqualTo("John P."); // response built from the same entity
    assertThat(response.createdAt())
        .isEqualTo(CREATED_AT); // createdAt untouched
    verify(userRepository, never())
        .save(any(User.class)); // UPDATE will be written by dirty checking
  }

  @Test
  void when_update_missing_then_UserNotFoundException() {
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(()
                           -> userService.update(
                               42L, new UserUpdateRequest(
                                        "John P.", "john.p@example.com", 31)))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("42");

    verify(userRepository, never())
        .existsByEmailAndIdNot(anyString(), anyLong());
  }

  @Test
  void when_update_keepsOwnEmail_then_allowed() {
    // pins the IdNot semantics: renaming WITHOUT changing the email is legal —
    // the pre-check asks "is the email taken by SOMEONE ELSE", oneself is
    // excluded
    User loaded = user(1L, "John", "john@example.com", 30);
    when(userRepository.findById(1L)).thenReturn(Optional.of(loaded));
    when(userRepository.existsByEmailAndIdNot("john@example.com", 1L))
        .thenReturn(false);

    UserResponse response = userService.update(
        1L, new UserUpdateRequest("John P.", "john@example.com", 31));

    assertThat(response.name()).isEqualTo("John P.");
    assertThat(response.email()).isEqualTo("john@example.com");
  }

  @Test
  void when_update_emailTakenByOther_then_DuplicateEmail() {
    User loaded = user(1L, "John", "john@example.com", 30);
    when(userRepository.findById(1L)).thenReturn(Optional.of(loaded));
    when(userRepository.existsByEmailAndIdNot("alice@example.com", 1L))
        .thenReturn(true);

    assertThatThrownBy(
        ()
            -> userService.update(
                1L, new UserUpdateRequest("John P.", "alice@example.com", 31)))
        .isInstanceOf(DuplicateEmailException.class)
        .hasMessageContaining("alice@example.com");

    assertThat(loaded.getName()).isEqualTo("John"); // entity untouched:
    assertThat(loaded.getEmail())
        .isEqualTo("john@example.com"); // pre-check fired BEFORE modify
  }

  // ==================== delete ====================

  @Test
  void when_delete_existing_then_delegatesToRepo() {
    when(userRepository.existsById(1L)).thenReturn(true);

    userService.delete(1L);

    verify(userRepository).deleteById(1L);
  }

  @Test
  void when_delete_missing_then_UserNotFoundException() {
    when(userRepository.existsById(42L)).thenReturn(false);

    assertThatThrownBy(() -> userService.delete(42L))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("42");

    verify(userRepository, never()).deleteById(anyLong());
  }

  // ==================== helpers ====================

  /**
   * A "persisted" entity: id/createdAt are set via reflection — User
   * deliberately has no setters for them.
   */
  private static User user(Long id, String name, String email, Integer age) {
    User u = new User(name, email, age);
    setField(u, "id", id);
    setField(u, "createdAt", CREATED_AT);
    return u;
  }

  private static void setField(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("No access to field " + field, e);
    }
  }
}