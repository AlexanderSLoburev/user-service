package com.example.userservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.util.HibernateUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Unit tests for the console layer of {@link App}.
 *
 * <p>These tests drive the private command handlers and {@code main} with a
 * scripted stdin and captured stdout/stderr. The database is never contacted:
 * the {@code HibernateUtil} factory is replaced with a proxy when
 * {@code initializeDatabase} needs to succeed or fail.
 *
 * <p>Note: {@code App} captures {@code System.in} once, in a static final
 * {@code Scanner}. Therefore stdin is replaced with {@link SwappableInputStream}
 * before the {@code App} class is initialized, and each test swaps its delegate.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppTest {

  private static final InputStream ORIGINAL_IN = System.in;
  private static final PrintStream ORIGINAL_OUT = System.out;
  private static final PrintStream ORIGINAL_ERR = System.err;
  private static final SwappableInputStream INPUT = new SwappableInputStream();

  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @BeforeAll
  static void bindAppToControlledInput() throws ClassNotFoundException {
    System.setIn(INPUT);
    Class.forName("com.example.userservice.App");
  }

  @AfterAll
  static void restoreSystemIn() {
    System.setIn(ORIGINAL_IN);
  }

  @BeforeEach
  void captureOutput() {
    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(ORIGINAL_OUT);
    System.setErr(ORIGINAL_ERR);
    INPUT.feed("");
  }

  // ==================== menu ====================

  @Test
  void when_printMenu_then_allMenuItemsAreShown() throws Exception {
    invoke("printMenu");

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
    INPUT.feed("0\n");

    invoke("runMenu");

    assertTrue(output().contains("Goodbye!"));
  }

  @Test
  void when_unknownMenuCommandEntered_then_printsUnknownAndContinues()
      throws Exception {
    INPUT.feed("9\n0\n");

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("Unknown command, try again"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_createUserCommandEntered_then_printsCreatedUserAndContinues()
      throws Exception {
    INPUT.feed("1\nMaria\nmaria@example.com\n28\n0\n");

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("User created:"));
    assertTrue(output.contains("name='Maria'"));
    assertTrue(output.contains("Goodbye!"));
  }

  // ==================== create user ====================

  @Test
  void when_createUserWithValidInput_then_printsCreatedUser()
      throws Exception {
    INPUT.feed("Ivan Petrov\nivan@example.com\n30\n");

    invoke("createUser");

    String output = output();
    assertTrue(output.contains("User created:"));
    assertTrue(output.contains("name='Ivan Petrov'"));
    assertTrue(output.contains("email='ivan@example.com'"));
    assertTrue(output.contains("age=30"));
  }

  @Test
  void when_readNameReceivesBlank_then_printsErrorAndRereads() throws Exception {
    INPUT.feed("\nIvan\n");

    assertEquals("Ivan", invoke("readName"));
    assertTrue(output().contains("Name must not be empty"));
  }

  @Test
  void when_readEmailReceivesMixedCase_then_returnsNormalizedEmail()
      throws Exception {
    INPUT.feed("Ivan@Example.COM\n");

    assertEquals("ivan@example.com", invoke("readEmail"));
  }

  @Test
  void when_readEmailReceivesInvalid_then_printsErrorAndRereads()
      throws Exception {
    INPUT.feed("not-an-email\nivan@example.com\n");

    assertEquals("ivan@example.com", invoke("readEmail"));
    assertTrue(output().contains("Invalid email format"));
  }

  @Test
  void when_readAgeReceivesBlank_then_returnsNull() throws Exception {
    INPUT.feed("\n");

    assertNull(invoke("readAge"));
  }

  @Test
  void when_readAgeReceivesNonNumeric_then_printsErrorAndRereads()
      throws Exception {
    INPUT.feed("abc\n25\n");

    assertEquals(25, invoke("readAge"));
    assertTrue(output().contains("Age must be a number"));
  }

  @Test
  void when_readIdReceivesNonPositive_then_printsErrorAndRereads()
      throws Exception {
    INPUT.feed("-5\n42\n");

    assertEquals(42L, invoke("readId"));
    assertTrue(output().contains("Id must be positive"));
  }

  // ==================== placeholder commands ====================

  @Test
  void when_showAllUsersCommandSelected_then_printsPlaceholder()
      throws Exception {
    invoke("showAllUsers");

    assertTrue(output().contains("Showing all users"));
  }

  @Test
  void when_findUserByIdCommandSelected_then_printsRequestedId()
      throws Exception {
    INPUT.feed("42\n");

    invoke("findUserById");

    assertTrue(output().contains("Looking for user with id=42"));
  }

  @Test
  void when_updateUserCommandSelected_then_printsRequestedId()
      throws Exception {
    INPUT.feed("7\n");

    invoke("updateUser");

    assertTrue(output().contains("Updating user with id=7"));
  }

  @Test
  void when_deleteUserCommandSelected_then_printsRequestedId()
      throws Exception {
    INPUT.feed("7\n");

    invoke("deleteUser");

    assertTrue(output().contains("Deleting user with id=7"));
  }

  // ==================== main ====================

  @Test
  void when_appIsConstructed_then_instanceIsCreated() {
    assertNotNull(new App());
  }

  @Test
  void when_mainRunsWithExitCommand_then_printsGreetingAndGoodbye()
      throws Exception {
    SessionFactory previous = replaceSessionFactory(factoryProxy(true));
    try {
      INPUT.feed("0\n");

      invoke("main", new Class<?>[] {String[].class},
          new Object[] {new String[0]});
    } finally {
      replaceSessionFactory(previous);
    }

    String output = output();
    assertTrue(output.contains("--- User Service ---"));
    assertTrue(output.contains("Connecting to the database..."));
    assertTrue(output.contains("Database connection established."));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_databaseConnectionFails_then_printsErrorAndStops()
      throws Exception {
    SessionFactory previous = replaceSessionFactory(factoryProxy(false));
    try {
      invoke("main", new Class<?>[] {String[].class},
          new Object[] {new String[0]});
    } finally {
      replaceSessionFactory(previous);
    }

    assertTrue(output().contains("Connecting to the database..."));
    assertTrue(errorOutput().contains(
        "[ERROR] Could not connect to the database"));
    assertFalse(output().contains("Database connection established."));
    assertFalse(output().contains("Goodbye!"));
  }

  @Test
  void when_allMenuCommandsEntered_then_eachCommandIsDispatched()
      throws Exception {
    INPUT.feed("2\n1\n3\n4\n2\n5\n3\n0\n");

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("Looking for user with id=1"));
    assertTrue(output.contains("Showing all users"));
    assertTrue(output.contains("Updating user with id=2"));
    assertTrue(output.contains("Deleting user with id=3"));
    assertTrue(output.contains("Goodbye!"));
  }

  // ==================== error handling ====================

  @Test
  void when_commandThrowsIllegalArgumentException_then_printsErrorAndContinues()
      throws Exception {
    INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");
    System.setOut(failingStreamOn("User created:",
        new IllegalArgumentException("boom")));

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("ERROR: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_commandThrowsDomainException_then_printsErrorAndContinues()
      throws Exception {
    INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");
    System.setOut(failingStreamOn("User created:",
        new UserNotFoundException("boom")));

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("ERROR: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  void when_commandThrowsUnexpectedException_then_printsErrorAndContinues()
      throws Exception {
    INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");
    System.setOut(failingStreamOn("User created:",
        new RuntimeException("boom")));

    invoke("runMenu");

    String output = output();
    assertTrue(output.contains("ERROR! Unexpected: boom"));
    assertTrue(output.contains("Goodbye!"));
  }

  @Test
  @Order(Integer.MAX_VALUE)
  void when_shutdownHookRuns_then_scannerIsClosedAndFactoryShutDown()
      throws Exception {
    invoke("lambda$main$0");

    assertThrows(IllegalStateException.class, () -> invoke("readName"));
  }

  // ==================== auxiliary ====================

  private static Object invoke(String methodName) throws Exception {
    return invoke(methodName, new Class<?>[0]);
  }

  private static Object invoke(String methodName, Class<?>[] parameterTypes,
      Object... arguments) throws Exception {
    Method method = App.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    try {
      return method.invoke(null, arguments);
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

  private PrintStream failingStreamOn(String trigger, RuntimeException failure) {
    ThrowingPrintStream stream = new ThrowingPrintStream(out);
    stream.failOn(trigger, failure);
    return stream;
  }

  private static SessionFactory replaceSessionFactory(SessionFactory replacement)
      throws Exception {
    Field field = HibernateUtil.class.getDeclaredField("factory");
    field.setAccessible(true);
    SessionFactory previous = (SessionFactory) field.get(null);
    field.set(null, replacement);
    return previous;
  }

  private static SessionFactory factoryProxy(boolean open) {
    return (SessionFactory) Proxy.newProxyInstance(
        AppTest.class.getClassLoader(),
        new Class<?>[] {SessionFactory.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "isOpen" -> {
              if (open) {
                return true;
              }
              throw new IllegalStateException("factory is broken");
            }
            case "toString" -> {
              return "factory-proxy";
            }
            case "hashCode" -> {
              return System.identityHashCode(proxy);
            }
            case "equals" -> {
              return proxy == args[0];
            }
            default -> throw new UnsupportedOperationException(method.getName());
          }
        });
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

  /** An stdout that throws a given exception once, on a matching line. */
  private static final class ThrowingPrintStream extends PrintStream {

    private String trigger;
    private RuntimeException failure;

    ThrowingPrintStream(OutputStream out) {
      super(out, true, StandardCharsets.UTF_8);
    }

    void failOn(String trigger, RuntimeException failure) {
      this.trigger = trigger;
      this.failure = failure;
    }

    @Override
    public void println(String text) {
      if (failure != null && text.startsWith(trigger)) {
        RuntimeException toThrow = failure;
        failure = null;
        throw toThrow;
      }
      super.println(text);
    }
  }
}
