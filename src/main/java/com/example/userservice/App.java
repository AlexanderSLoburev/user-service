package com.example.userservice;

import com.example.userservice.console.InputValidator;
import com.example.userservice.dao.UserDaoImpl;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.service.UserService;
import com.example.userservice.util.HibernateUtil;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;

/**
 * Console entry point.
 *
 * <p>The application state (input scanner, service and database lifecycle) is
 * kept in instance fields instead of static fields. This makes every instance
 * isolated: tests never share a scanner, a service or a Hibernate factory.
 */
public class App {

    private final Scanner scanner;
    private final Supplier<SessionFactory> database;
    private final Runnable databaseShutdown;
    private UserService userService;

    /** Production constructor: reads stdin and uses the application database. */
    public App(Scanner scanner) {
        this(scanner, null, HibernateUtil::getSessionFactory,
             HibernateUtil::shutdown);
    }

    /** Test constructor: the console layer runs against a fake service. */
    App(Scanner scanner, UserService userService) {
        this(scanner, userService, () -> null, () -> { });
    }

    /** Test constructor: lets a test control the database lifecycle. */
    App(Scanner scanner, UserService userService,
        Supplier<SessionFactory> database, Runnable databaseShutdown) {
        this.scanner = scanner;
        this.userService = userService;
        this.database = database;
        this.databaseShutdown = databaseShutdown;
    }

    public static void main(String[] args) {
        new App(new Scanner(System.in)).run();
    }

    private void run() {
        System.out.println("--- User Service ---");

        try {
            if (!initializeDatabase()) {
                return;
            }
            runMenu();
        } finally {
            scanner.close();
            databaseShutdown.run();
            System.out.println("Application stopped.");
        }
    }

    private boolean initializeDatabase() {
        System.out.println("Connecting to the database...");
        try {
            database.get();
            if (userService == null) {
                userService = new UserService(new UserDaoImpl());
            }
            System.out.println("Database connection established.");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Could not connect to the database: " + e.getMessage());
            System.err.println("Make sure PostgreSQL is running (docker compose up -d)");
            System.err.println("and that db.properties has the correct URL, username and password.");
            return false;
        }
    }

    private void runMenu() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createUser();
                    case "2" -> findUserById();
                    case "3" -> showAllUsers();
                    case "4" -> updateUser();
                    case "5" -> deleteUser();
                    case "0" -> {
                        running = false;
                        System.out.println("Goodbye!");
                    }
                    default -> System.out.println("Unknown command, try again");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: " + e.getMessage());
            } catch (UserNotFoundException | DuplicateEmailException | DataAccessException e) {
                System.out.println("ERROR: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("ERROR! Unexpected: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("--- Menu ---");
        System.out.println("1. Create user");
        System.out.println("2. Find user by id");
        System.out.println("3. Show all users");
        System.out.println("4. Update user");
        System.out.println("5. Delete user");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    private String readName() {
        while (true) {
            System.out.print("Name: ");
            try {
                return InputValidator.validateName(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private String readEmail() {
        while (true) {
            System.out.print("Email: ");
            try {
                return InputValidator.validateEmail(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private Integer readAge() {
        while (true) {
            System.out.print("Age (press Enter to skip): ");
            try {
                return InputValidator.validateAge(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private Long readId() {
        while (true) {
            System.out.print("Id: ");
            try {
                return InputValidator.validateId(scanner.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private void createUser() {
        String name = readName();
        String email = readEmail();
        Integer age = readAge();
        User user = userService.createUser(name, email, age);
        System.out.println("User created: " + user);
    }

    private void findUserById() {
        Long id = readId();
        User user = userService.showUser(id);
        System.out.println("User: " + user);
    }

    private void showAllUsers() {
        List<User> users = userService.listUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        users.forEach(System.out::println);
    }

    private void updateUser() {
        Long id = readId();
        String name = readName();
        String email = readEmail();
        Integer age = readAge();
        User user = userService.updateUser(id, name, email, age);
        System.out.println("User updated: " + user);
    }

    private void deleteUser() {
        Long id = readId();
        userService.deleteUser(id);
        System.out.println("User deleted: id=" + id);
    }
}
