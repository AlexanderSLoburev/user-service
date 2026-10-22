package com.example.userservice.service;

import com.example.userservice.dao.UserDao;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User createUser(String name, String email, Integer age) {
        log.info("Creating user: name={}, email={}, age={}", name, email, age);
        String normalizedEmail = normalizeEmail(email);
        User user = new User(name, normalizedEmail, age);
        try {
            if (userDao.findByEmail(normalizedEmail).isPresent()) {
                throw new DuplicateEmailException(normalizedEmail);
            }
            User saved = userDao.save(user);
            log.info("User created: id={}", saved.getId());
            return saved;
        } catch (DuplicateEmailException e) {
            log.error("Duplicate email on create: {}", email, e);
            throw e;
        } catch (DataAccessException e) {
            log.error("Data access error on create user: name={}, email={}", name, email, e);
            throw e;
        }
    }

    public User showUser(Long id) {
        log.info("Fetching user: id={}", id);
        try {
            Optional<User> user = userDao.findById(id);
            if (user.isEmpty()) {
                throw new UserNotFoundException(id);
            }
            log.info("User found: id={}, name={}", id, user.get().getName());
            return user.get();
        } catch (UserNotFoundException e) {
            log.warn("User not found: id={}", id);
            throw e;
        } catch (DataAccessException e) {
            log.error("Data access error on find user: id={}", id, e);
            throw e;
        }
    }

    public List<User> listUsers() {
        log.info("Fetching all users");
        try {
            List<User> users = userDao.findAll();
            log.info("Found {} users", users.size());
            return users;
        } catch (DataAccessException e) {
            log.error("Data access error on list users", e);
            throw e;
        }
    }

    public User updateUser(Long id, String name, String email, Integer age) {
        log.info("Updating user: id={}, name={}, email={}, age={}", id, name, email, age);
        Optional<User> existing = userDao.findById(id);
        if (existing.isEmpty()) {
            log.warn("User not found on update: id={}", id);
            throw new UserNotFoundException(id);
        }
        String normalizedEmail = normalizeEmail(email);
        Optional<User> byEmail = userDao.findByEmail(normalizedEmail);
        if (byEmail.isPresent() && !id.equals(byEmail.get().getId())) {
            log.warn("Duplicate email on update: id={}, email={}", id, normalizedEmail);
            throw new DuplicateEmailException(normalizedEmail);
        }
        User user = existing.get();
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setAge(age);
        try {
            User updated = userDao.update(user);
            log.info("User updated: id={}", updated.getId());
            return updated;
        } catch (UserNotFoundException e) {
            log.warn("User not found on update: id={}", id);
            throw e;
        } catch (DuplicateEmailException e) {
            log.error("Duplicate email on update: id={}, email={}", id, email, e);
            throw e;
        } catch (DataAccessException e) {
            log.error("Data access error on update user: id={}", id, e);
            throw e;
        }
    }

    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);
        try {
            boolean deleted = userDao.deleteById(id);
            if (!deleted) {
                log.warn("User not found on delete: id={}", id);
                throw new UserNotFoundException(id);
            }
            log.info("User deleted: id={}", id);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Data access error on delete user: id={}", id, e);
            throw e;
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
