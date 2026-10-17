package com.example.userservice;

import com.example.userservice.console.InputValidator;
import com.example.userservice.entity.User;
import com.example.userservice.util.HibernateUtil;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.util.Scanner;

public class App {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== User Service ===");

        try {
            if (!initializeDatabase()) {
                return;
            }
            runMenu();
        } finally {
            SCANNER.close();
            HibernateUtil.shutdown();
            System.out.println("Application stopped.");
        }
    }

    private static boolean initializeDatabase() {
        System.out.println("Connecting to the database...");
        try {
            HibernateUtil.getSessionFactory();
            System.out.println("Database connection established.");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Could not connect to the database: " + e.getMessage());
            System.err.println("Make sure PostgreSQL is running (docker compose up -d)");
            System.err.println("and that db.properties has the correct URL, username and password.");
            return false;
        }
    }

    private static void runMenu() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = SCANNER.nextLine().trim();

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

    private static void printMenu() {
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

    private static String readName() {
        while (true) {
            System.out.print("Name: ");
            try {
                return InputValidator.validateName(SCANNER.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private static String readEmail() {
        while (true) {
            System.out.print("Email: ");
            try {
                return InputValidator.validateEmail(SCANNER.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private static Integer readAge() {
        while (true) {
            System.out.print("Age (press Enter to skip): ");
            try {
                return InputValidator.validateAge(SCANNER.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private static Long readId() {
        while (true) {
            System.out.print("Id: ");
            try {
                return InputValidator.validateId(SCANNER.nextLine());
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR " + e.getMessage());
            }
        }
    }

    private static void createUser() {
        String name = readName();
        String email = readEmail();
        Integer age = readAge();

        User user = new User(name, email, age);
        System.out.println("User created: " + user);
    }

    private static void findUserById() {
        Long id = readId();
        System.out.println("Looking for user with id=" + id);
    }

    private static void showAllUsers() {
        System.out.println("Showing all users");
    }

    private static void updateUser() {
        Long id = readId();
        System.out.println("Updating user with id=" + id);
    }

    private static void deleteUser() {
        Long id = readId();
        System.out.println("Deleting user with id=" + id);
    }
}