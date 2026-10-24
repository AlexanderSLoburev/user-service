package com.example.userservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.userservice.entity.User;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.service.UserService;
import com.example.userservice.util.HibernateUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * {@code userService} is replaced with a fake, and the {@code HibernateUtil}
 * factory is replaced with a proxy when {@code initializeDatabase} needs to
 * succeed or fail.
 *
 * <p>Note: {@code App} captures {@code System.in} once, in a static final
 * {@code Scanner}. Therefore stdin is replaced with {@link SwappableInputStream}
 * before the {@code App} class is initialized, and each test swaps its delegate.
 * Tests that call {@code main} close that scanner, so they run last.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppTest {

    private static final InputStream ORIGINAL_IN = System.in;
    private static final PrintStream ORIGINAL_OUT = System.out;
    private static final PrintStream ORIGINAL_ERR = System.err;
    private static final SwappableInputStream INPUT = new SwappableInputStream();

    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private UserService userService;

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
    void setUp() throws Exception {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));

        userService = mock(UserService.class);
        setUserService(userService);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(ORIGINAL_OUT);
        System.setErr(ORIGINAL_ERR);
        INPUT.feed("");
        setUserService(null);
    }

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
        User created = new User("Maria", "maria@example.com", 28);
        when(userService.createUser(anyString(), anyString(), any()))
                .thenReturn(created);

        INPUT.feed("1\nMaria\nmaria@example.com\n28\n0\n");

        invoke("runMenu");

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

        INPUT.feed("2\n1\n3\n4\n2\nPetr\npetr@example.com\n25\n5\n3\n0\n");

        invoke("runMenu");

        String output = output();
        assertTrue(output.contains("User: "));
        assertTrue(output.contains("name='Ivan'"));
        assertTrue(output.contains("User updated:"));
        assertTrue(output.contains("name='Petr'"));
        assertTrue(output.contains("User deleted: id=3"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void when_createUserWithValidInput_then_printsCreatedUser()
            throws Exception {
        User created = new User("Ivan Petrov", "ivan@example.com", 30);
        when(userService.createUser("Ivan Petrov", "ivan@example.com", 30))
                .thenReturn(created);

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

    @Test
    void when_showAllUsersCommandSelected_then_printsUsers() throws Exception {
        when(userService.listUsers())
                .thenReturn(List.of(new User("Ivan", "ivan@example.com", 30)));

        invoke("showAllUsers");

        String output = output();
        assertTrue(output.contains("name='Ivan'"));
        assertTrue(output.contains("email='ivan@example.com'"));
    }

    @Test
    void when_showAllUsersHasNoUsers_then_printsNoUsersFound()
            throws Exception {
        when(userService.listUsers()).thenReturn(List.of());

        invoke("showAllUsers");

        assertTrue(output().contains("No users found."));
    }

    @Test
    void when_findUserByIdCommandSelected_then_printsUser() throws Exception {
        User ivan = new User("Ivan", "ivan@example.com", 30);
        when(userService.showUser(42L)).thenReturn(ivan);
        INPUT.feed("42\n");

        invoke("findUserById");

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
        INPUT.feed("7\nPetr\npetr@example.com\n25\n");

        invoke("updateUser");

        String output = output();
        assertTrue(output.contains("User updated:"));
        assertTrue(output.contains("name='Petr'"));
    }

    @Test
    void when_deleteUserCommandSelected_then_printsDeletedId()
            throws Exception {
        INPUT.feed("7\n");

        invoke("deleteUser");

        assertTrue(output().contains("User deleted: id=7"));
        verify(userService).deleteUser(7L);
    }

    @Test
    void when_appIsConstructed_then_instanceIsCreated() {
        assertNotNull(new App());
    }

    @Test
    @Order(Integer.MAX_VALUE - 1)
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
        assertTrue(output.contains("Application stopped."));
    }

    @Test
    @Order(Integer.MAX_VALUE)
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
        assertTrue(output().contains("Application stopped."));
        assertFalse(output().contains("Database connection established."));
        assertFalse(output().contains("Goodbye!"));
    }

    @Test
    void when_commandThrowsIllegalArgumentException_then_printsErrorAndContinues()
            throws Exception {
        when(userService.createUser(anyString(), anyString(), any()))
                .thenThrow(new IllegalArgumentException("boom"));
        INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");

        invoke("runMenu");

        String output = output();
        assertTrue(output.contains("ERROR: boom"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void when_commandThrowsDomainException_then_printsErrorAndContinues()
            throws Exception {
        when(userService.createUser(anyString(), anyString(), any()))
                .thenThrow(new UserNotFoundException("boom"));
        INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");

        invoke("runMenu");

        String output = output();
        assertTrue(output.contains("ERROR: boom"));
        assertTrue(output.contains("Goodbye!"));
    }

    @Test
    void when_commandThrowsUnexpectedException_then_printsErrorAndContinues()
            throws Exception {
        when(userService.createUser(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("boom"));
        INPUT.feed("1\nIvan\nivan@example.com\n30\n0\n");

        invoke("runMenu");

        String output = output();
        assertTrue(output.contains("ERROR! Unexpected: boom"));
        assertTrue(output.contains("Goodbye!"));
    }

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

    private static void setUserService(UserService service) throws Exception {
        Field field = App.class.getDeclaredField("userService");
        field.setAccessible(true);
        field.set(null, service);
    }

    private String output() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String errorOutput() {
        return err.toString(StandardCharsets.UTF_8);
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
        AtomicBoolean openState = new AtomicBoolean(open);
        AtomicBoolean firstFailure = new AtomicBoolean(true);
        return (SessionFactory) Proxy.newProxyInstance(
                AppTest.class.getClassLoader(),
                new Class<?>[] {SessionFactory.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isOpen" -> {
                            if (!open) {
                                if (firstFailure.getAndSet(false)) {
                                    throw new IllegalStateException("factory is broken");
                                }
                                return false;
                            }
                            return openState.get();
                        }
                        case "close" -> {
                            openState.set(false);
                            return null;
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
}