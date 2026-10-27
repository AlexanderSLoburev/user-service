package com.example.userservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.userservice.entity.User;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the console layer of {@link App}.
 *
 * <p>Each test creates its own {@link App} together with an isolated input
 * stream and a Mockito double for {@link UserService}, so the tests neither
 * share mutable state nor depend on the order in which they run. stdout/stderr
 * are captured and restored around every test. The database is never
 * contacted: the database lifecycle is injected as a pair of lambdas.
 */
class AppTest {

  private static final PrintStream ORIGINAL_OUT = System.out;
  private static final PrintStream ORIGINAL_ERR = System.err;

  private SwappableInputStream input;
  private Scanner scanner;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private UserService userService;
  private App app;

  @BeforeEach
  void setUp() {
    input = new SwappableInputStream();
    scanner = new Scanner(input, StandardCharsets.UTF_8);
    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));

    userService = mock(UserService.class);
    app = new App(scanner, userService);
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(ORIGINAL_OUT);
    System.setErr(ORIGINAL_ERR);
  }

  // ==================== menu ====================

  @Test
  void when_printMenu_then_allMenuItemsAreShown() throws Exception {
    invoke(app, "printMenu");

    String output = output();
    assertTrue(output.contains("--- Menu ---"));
    assertTrue(output.contains("1. Create user"));
    assertTrue(output.contains("2. Find user by id"));
    assertTrue(output.contains("3. Show all users"));
    assertTrue(output.contains("4. Update user"));
    assertTrue(output.contains("5. Delete user"));
    assertTrue(output.contains("0. Exit"));
    assertTrue(output.contains("Choice: "));
  }

  @Test
  void when_exitCommandEntered_then_printsGoodbye() throws Exception {
    input.feed("0\n");

    invoke(app, "runMenu");

    assertTrue(output().contains("Goodbye!"));
  }

  @Test
  void when_unknownMenuCommandEntered_then_printsUnknownAndContinues()
      throws Exception {
    input.feed("9\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("Unknown command, try again"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_createUserCommandEntered_then_printsCreatedUserAndContinues()
      throws Exception {
    User created = new User("Maria", "maria@example.com", 28);
    when(userService.createUser(anyString(), anyString(), any()))
        .thenReturn(created);

    input.feed("1\nMaria\nmaria@example.com\n28\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("User created:"));
    assertTrue(output.contains("name='Maria'"));
    assertTrue(output.contains("Goodbye!"));
    verify(userService).createUser("Maria", "maria@example.com", 28);
  }

  @Test
  void when_allMenuCommandsEntered_then_eachCommandIsDispatched()
      throws Exception {
    User ivan = new User("Ivan", "ivan@example.com", 30);
    User petr = new User("Petr", "petr@example.com", 25);
    when(userService.showUser(anyLong())).thenReturn(ivan);
    when(userService.listUsers()).thenReturn(List.of(ivan));
    when(userService.updateUser(anyLong(), anyString(), anyString(), any()))
        .thenReturn(petr);

    input.feed("2\n1\n3\n4\n2\nPetr\npetr@example.com\n25\n5\n3\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("User: "));
    assertTrue(output.contains("name='Ivan'"));
    assertTrue(output.contains("User updated:"));
    assertTrue(output.contains("name='Petr'"));
    assertTrue(output.contains("User deleted: id=3"));
    assertTrue(output.contains("Goodbye!"));
  }

  // ==================== create user ====================

  @Test
  void when_createUserWithValidInput_then_printsCreatedUser()
      throws Exception {
    User created = new User("Ivan Petrov", "ivan@example.com", 30);
    when(userService.createUser("Ivan Petrov", "ivan@example.com", 30))
        .thenReturn(created);

    input.feed("Ivan Petrov\nivan@example.com\n30\n");

    invoke(app, "createUser");

    String output = output();
    assertTrue(output.contains("User created:"));
    assertTrue(output.contains("name='Ivan Petrov'"));
    assertTrue(output.contains("email='ivan@example.com'"));
    assertTrue(output.contains("age=30"));
    verify(userService).createUser("Ivan Petrov", "ivan@example.com", 30);
  }

  @Test
  void when_readNameReceivesBlank_then_printsErrorAndRereads() throws Exception {
    input.feed("\nIvan\n");

    assertEquals("Ivan", invoke(app, "readName"));
    assertTrue(output().contains("Name must not be empty"));
  }

  @Test
  void when_readEmailReceivesMixedCase_then_returnsNormalizedEmail()
      throws Exception {
    input.feed("Ivan@Example.COM\n");

    assertEquals("ivan@example.com", invoke(app, "readEmail"));
  }

  @Test
  void when_readEmailReceivesInvalid_then_printsErrorAndRereads()
      throws Exception {
    input.feed("not-an-email\nivan@example.com\n");

    assertEquals("ivan@example.com", invoke(app, "readEmail"));
    assertTrue(output().contains("Invalid email format"));
  }

  @Test
  void when_readAgeReceivesBlank_then_returnsNull() throws Exception {
    input.feed("\n");

    assertNull(invoke(app, "readAge"));
  }

  @Test
  void when_readAgeReceivesNonNumeric_then_printsErrorAndRereads()
      throws Exception {
    input.feed("abc\n25\n");

    assertEquals(25, invoke(app, "readAge"));
    assertTrue(output().contains("Age must be a number"));
  }

  @Test
  void when_readIdReceivesNonPositive_then_printsErrorAndRereads()
      throws Exception {
    input.feed("-5\n42\n");

    assertEquals(42L, invoke(app, "readId"));
    assertTrue(output().contains("Id must be positive"));
  }

  // ==================== user commands ====================

  @Test
  void when_showAllUsersCommandSelected_then_printsUsers() throws Exception {
    when(userService.listUsers())
        .thenReturn(List.of(new User("Ivan", "ivan@example.com", 30)));

    invoke(app, "showAllUsers");

    String output = output();
    assertTrue(output.contains("name='Ivan'"));
    assertTrue(output.contains("email='ivan@example.com'"));
  }

  @Test
  void when_showAllUsersHasNoUsers_then_printsNoUsersFound()
      throws Exception {
    when(userService.listUsers()).thenReturn(List.of());

    invoke(app, "showAllUsers");

    assertTrue(output().contains("No users found."));
  }

  @Test
  void when_findUserByIdCommandSelected_then_printsUser() throws Exception {
    User ivan = new User("Ivan", "ivan@example.com", 30);
    when(userService.showUser(42L)).thenReturn(ivan);
    input.feed("42\n");

    invoke(app, "findUserById");

    String output = output();
    assertTrue(output.contains("User: "));
    assertTrue(output.contains("name='Ivan'"));
  }

  @Test
  void when_updateUserCommandSelected_then_printsUpdatedUser()
      throws Exception {
    User updated = new User("Petr", "petr@example.com", 25);
    when(userService.updateUser(7L, "Petr", "petr@example.com", 25))
        .thenReturn(updated);
    input.feed("7\nPetr\npetr@example.com\n25\n");

    invoke(app, "updateUser");

    String output = output();
    assertTrue(output.contains("User updated:"));
    assertTrue(output.contains("name='Petr'"));
  }

  @Test
  void when_deleteUserCommandSelected_then_printsDeletedId()
      throws Exception {
    input.feed("7\n");

    invoke(app, "deleteUser");

    assertTrue(output().contains("User deleted: id=7"));
    verify(userService).deleteUser(7L);
  }

  // ==================== main ====================

  @Test
  void when_appIsConstructed_then_instanceIsCreated() {
    assertNotNull(app);
  }

  @Test
  void when_mainRunsWithExitCommand_then_printsGreetingAndGoodbye()
      throws Exception {
    input.feed("0\n");

    invoke(appWithDatabase(() -> null), "run");

    String output = output();
    assertTrue(output.contains("--- User Service ---"));
    assertTrue(output.contains("Connecting to the database..."));
    assertTrue(output.contains("Database connection established."));
    assertTrue(output.contains("Goodbye!"));
    assertTrue(output.contains("Application stopped."));
  }

  @Test
  void when_databaseConnectionFails_then_printsErrorAndStops()
      throws Exception {
    invoke(appWithDatabase(() -> {
      throw new IllegalStateException("database is down");
    }), "run");

    assertTrue(output().contains("Connecting to the database..."));
    assertTrue(errorOutput().contains(
        "[ERROR] Could not connect to the database"));
    assertTrue(output().contains("Application stopped."));
    assertFalse(output().contains("Database connection established."));
    assertFalse(output().contains("Goodbye!"));
  }

  // ==================== error handling ====================

  @Test
  void when_commandThrowsIllegalArgumentException_then_printsErrorAndContinues()
      throws Exception {
    when(userService.createUser(anyString(), anyString(), any()))
        .thenThrow(new IllegalArgumentException("boom"));
    input.feed("1\nIvan\nivan@example.com\n30\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("ERROR: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_commandThrowsDomainException_then_printsErrorAndContinues()
      throws Exception {
    when(userService.createUser(anyString(), anyString(), any()))
        .thenThrow(new UserNotFoundException("boom"));
    input.feed("1\nIvan\nivan@example.com\n30\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("ERROR: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_commandThrowsUnexpectedException_then_printsErrorAndContinues()
      throws Exception {
    when(userService.createUser(anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("boom"));
    input.feed("1\nIvan\nivan@example.com\n30\n0\n");

    invoke(app, "runMenu");

    String output = output();
    assertTrue(output.contains("ERROR! Unexpected: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  // ==================== auxiliary ====================

  private App appWithDatabase(Supplier<SessionFactory> database) {
    return new App(scanner, userService, database, () -> { });
  }

  private static Object invoke(App target, String methodName) throws Exception {
    return invoke(target, methodName, new Class<?>[0]);
  }

  private static Object invoke(App target, String methodName,
      Class<?>[] parameterTypes, Object... arguments) throws Exception {
    Method method = App.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(target, arguments);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new RuntimeException(cause);
    }
  }

  private String output() {
    return out.toString(StandardCharsets.UTF_8);
  }

  private String errorOutput() {
    return err.toString(StandardCharsets.UTF_8);
  }

  /** An stdin whose content can be replaced between tests. */
  private static final class SwappableInputStream extends InputStream {

    private InputStream delegate = new ByteArrayInputStream(new byte[0]);

    void feed(String text) {
      delegate = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      return delegate.read(buffer, offset, length);
    }
  }
}
