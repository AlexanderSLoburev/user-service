package com.example.userservice;

import com.example.userservice.console.InputValidator;
import com.example.userservice.entity.User;
import java.util.Scanner;

public class App {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println(" User Service ");

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
            } catch (Exception e) {
                System.out.println("ERROR! Something went wrong: " + e.getMessage());
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

    private static void createUser() {
        System.out.print("Name: ");
        String name = InputValidator.validateName(SCANNER.nextLine());

        System.out.print("Email: ");
        String email = InputValidator.validateEmail(SCANNER.nextLine());

        System.out.print("Age (press Enter to skip): ");
        Integer age = InputValidator.validateAge(SCANNER.nextLine());

        User user = new User(name, email, age);
        System.out.println("User created: " + user);
    }

    private static void findUserById() {
        System.out.print("Id: ");
        Long id = InputValidator.validateId(SCANNER.nextLine());
        System.out.println("Looking for user with id=" + id);
    }

    private static void showAllUsers() {
        System.out.println("Showing all users");
    }

    private static void updateUser() {
        System.out.print("Id: ");
        Long id = InputValidator.validateId(SCANNER.nextLine());
        System.out.println("Updating user with id=" + id);
    }

    private static void deleteUser() {
        System.out.print("Id: ");
        Long id = InputValidator.validateId(SCANNER.nextLine());
        System.out.println("Deleting user with id=" + id);
    }
}