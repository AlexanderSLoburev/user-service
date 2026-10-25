package com.example.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.userservice.dao.UserDao;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserDao userDao;

  @InjectMocks
  private UserService userService;

  @Test
  void createUser_whenEmailIsAvailable_savesAndReturnsUser() {
    User saved = new User("Ivan", "ivan@example.com", 30);
    when(userDao.findByEmail("ivan@example.com")).thenReturn(Optional.empty());
    when(userDao.save(any(User.class))).thenReturn(saved);

    User result = userService.createUser("Ivan", "ivan@example.com", 30);

    assertSame(saved, result);
    verify(userDao).findByEmail("ivan@example.com");
    verify(userDao).save(any(User.class));
  }

  @Test
  void createUser_normalizesEmailBeforeCheckingAndSaving() {
    User saved = new User("Ivan", "ivan@example.com", 30);
    when(userDao.findByEmail("ivan@example.com")).thenReturn(Optional.empty());
    when(userDao.save(any(User.class))).thenReturn(saved);

    userService.createUser("Ivan", " Ivan@Example.COM ", 30);

    verify(userDao).findByEmail("ivan@example.com");
    verify(userDao).save(argThat(user ->
        "ivan@example.com".equals(user.getEmail())));
  }

  @Test
  void createUser_whenExactEmailAlreadyExists_throwsDuplicateWithoutSaving() {
    User existing = new User("Existing", "ivan@example.com", 30);
    when(userDao.findByEmail("ivan@example.com"))
        .thenReturn(Optional.of(existing));

    assertThrows(DuplicateEmailException.class,
                 () -> userService.createUser("Ivan", "ivan@example.com", 25));

    verify(userDao).findByEmail("ivan@example.com");
    verify(userDao, never()).save(any(User.class));
  }

  @Test
  void createUser_whenEmailExistsWithDifferentCase_throwsDuplicateWithoutSaving() {
    User existing = mock(User.class);
    when(userDao.findByEmail("ivan@example.com"))
        .thenReturn(Optional.of(existing));

    assertThrows(DuplicateEmailException.class,
                 () -> userService.createUser("Ivan", " IVAN@Example.COM ", 25));

    verify(userDao).findByEmail("ivan@example.com");
    verify(userDao, never()).save(any(User.class));
    verifyNoInteractions(existing);
  }

  @Test
  void createUser_whenDaoFails_rethrowsDataAccessException() {
    DataAccessException failure = new DataAccessException("database unavailable");
    when(userDao.findByEmail("ivan@example.com")).thenReturn(Optional.empty());
    when(userDao.save(any(User.class))).thenThrow(failure);

    DataAccessException thrown = assertThrows(
        DataAccessException.class,
        () -> userService.createUser("Ivan", "ivan@example.com", 30));

    assertSame(failure, thrown);
    verify(userDao).findByEmail("ivan@example.com");
    verify(userDao).save(any(User.class));
  }

  @Test
  void showUser_whenFound_returnsUser() {
    User user = new User("Ivan", "ivan@example.com", 30);
    when(userDao.findById(1L)).thenReturn(Optional.of(user));

    assertSame(user, userService.showUser(1L));

    verify(userDao).findById(1L);
  }

  @Test
  void showUser_whenNotFound_throwsUserNotFound() {
    when(userDao.findById(5L)).thenReturn(Optional.empty());

    UserNotFoundException exception = assertThrows(
        UserNotFoundException.class, () -> userService.showUser(5L));

    assertTrue(exception.getMessage().contains("5"));
    verify(userDao).findById(5L);
  }

  @Test
  void listUsers_whenEmpty_returnsEmptyList() {
    when(userDao.findAll()).thenReturn(List.of());

    assertEquals(List.of(), userService.listUsers());

    verify(userDao).findAll();
  }

  @Test
  void listUsers_whenNotEmpty_returnsUsers() {
    List<User> users = List.of(new User("Ivan", "ivan@example.com", 30));
    when(userDao.findAll()).thenReturn(users);

    assertSame(users, userService.listUsers());

    verify(userDao).findAll();
  }

  @Test
  void updateUser_whenUserExists_updatesAndReturnsUser() {
    User existing = mock(User.class);
    when(existing.getId()).thenReturn(7L);
    when(userDao.findById(7L)).thenReturn(Optional.of(existing));
    when(userDao.findByEmail("new@example.com")).thenReturn(Optional.empty());
    when(userDao.update(existing)).thenReturn(existing);

    User result =
        userService.updateUser(7L, "New Name", " New@Example.COM ", 31);

    assertSame(existing, result);
    verify(userDao).findById(7L);
    verify(userDao).findByEmail("new@example.com");
    verify(existing).setName("New Name");
    verify(existing).setEmail("new@example.com");
    verify(existing).setAge(31);
    verify(userDao).update(existing);
  }

  @Test
  void updateUser_whenUserNotFound_throwsWithoutCheckingEmailOrUpdating() {
    when(userDao.findById(7L)).thenReturn(Optional.empty());

    assertThrows(UserNotFoundException.class,
                 () -> userService.updateUser(
                     7L, "New Name", "new@example.com", 31));

    verify(userDao).findById(7L);
    verify(userDao, never()).findByEmail(any(String.class));
    verify(userDao, never()).update(any(User.class));
  }

  @Test
  void updateUser_whenEmailBelongsToAnotherUser_throwsWithoutUpdating() {
    User existing = mock(User.class);
    User other = mock(User.class);
    when(other.getId()).thenReturn(8L);
    when(userDao.findById(7L)).thenReturn(Optional.of(existing));
    when(userDao.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

    assertThrows(DuplicateEmailException.class,
                 () -> userService.updateUser(
                     7L, "New Name", "taken@example.com", 31));

    verify(userDao).findById(7L);
    verify(userDao).findByEmail("taken@example.com");
    verify(userDao, never()).update(any(User.class));
  }

  @Test
  void updateUser_whenEmailBelongsToSameUser_allowsUpdate() {
    User existing = mock(User.class);
    when(existing.getId()).thenReturn(7L);
    when(userDao.findById(7L)).thenReturn(Optional.of(existing));
    when(userDao.findByEmail("same@example.com"))
        .thenReturn(Optional.of(existing));
    when(userDao.update(existing)).thenReturn(existing);

    User result = userService.updateUser(
        7L, "Updated Name", "SAME@Example.COM", 32);

    assertSame(existing, result);
    verify(userDao).findById(7L);
    verify(userDao).findByEmail("same@example.com");
    verify(userDao).update(existing);
  }

  @Test
  void deleteUser_whenDeleted_completesSuccessfully() {
    when(userDao.deleteById(9L)).thenReturn(true);

    userService.deleteUser(9L);

    verify(userDao).deleteById(9L);
  }

  @Test
  void deleteUser_whenNotFound_throwsUserNotFound() {
    when(userDao.deleteById(9L)).thenReturn(false);

    assertThrows(UserNotFoundException.class, () -> userService.deleteUser(9L));

    verify(userDao).deleteById(9L);
  }
}
