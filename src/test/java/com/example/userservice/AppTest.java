package com.example.userservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the console layer of {@link App}.
 *
 * <p>The DB-dependent {@code main}/{@code initializeDatabase} path is not
 * covered here: these tests drive the private command handlers directly with a
 * scripted stdin and capture stdout, so no database is required.
 *
 * <p>Note: {@code App} captures {@code System.in} once, in a static final
 * {@code Scanner}. Therefore stdin is replaced with {@link SwappableInputStream}
 * before the {@code App} class is initialized, and each test swaps its delegate.
 */
class AppTest {

  private static final InputStream ORIGINAL_IN = System.in;
  private static final PrintStream ORIGINAL_OUT = System.out;
  private static final SwappableInputStream INPUT = new SwappableInputStream();

  private ByteArrayOutputStream out;

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
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreOutput() {
    System.setOut(ORIGINAL_OUT);
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

  // ==================== auxiliary ====================

  private static Object invoke(String methodName) throws Exception {
    Method method = App.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    try {
      return method.invoke(null);
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
