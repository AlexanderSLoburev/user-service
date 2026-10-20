package com.example.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.userservice.dao.UserDao;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class UserServiceTest {
    private static UserDao daoReturning(Optional<User> user) {
        return new UserDao() {
            public User save(User u) { return u; }
            public Optional<User> findById(Long id) { return user; }
            public Optional<User> findByEmail(String email) { return Optional.empty(); }
            public List<User> findAll() { return user.map(List::of).orElse(List.of()); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return true; }
        };
    }

    private static UserDao daoEmpty() {
        return daoReturning(Optional.empty());
    }

    @Test
    void showUser_throwsWhenDaoReturnsEmpty() {
        UserService service = new UserService(daoEmpty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> service.showUser(5L));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void showUser_returnsUser() {
        User user = new User("Ivan", "ivan@example.com", 30);
        UserService service = new UserService(daoReturning(Optional.of(user)));
        assertEquals(user, service.showUser(1L));
    }

    @Test
    void createUser_throwsDuplicateEmail() {
        UserDao dao = new UserDao() {
            public User save(User u) { throw new DuplicateEmailException(u.getEmail()); }
            public Optional<User> findById(Long id) { return Optional.empty(); }
            public Optional<User> findByEmail(String email) { return Optional.empty(); }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        UserService service = new UserService(dao);
        assertThrows(DuplicateEmailException.class,
                () -> service.createUser("Ivan", "ivan@example.com", 30));
    }

    @Test
    void createUser_throwsDataAccessException() {
        UserDao dao = new UserDao() {
            public User save(User u) { throw new DataAccessException("db down"); }
            public Optional<User> findById(Long id) { return Optional.empty(); }
            public Optional<User> findByEmail(String email) { return Optional.empty(); }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        UserService service = new UserService(dao);
        assertThrows(DataAccessException.class,
                () -> service.createUser("Ivan", "ivan@example.com", 30));
    }

    @Test
    void deleteUser_throwsWhenNotFound() {
        UserDao dao = new UserDao() {
            public User save(User u) { return u; }
            public Optional<User> findById(Long id) { return Optional.empty(); }
            public Optional<User> findByEmail(String email) { return Optional.empty(); }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        UserService service = new UserService(dao);
        assertThrows(UserNotFoundException.class, () -> service.deleteUser(42L));
    }

    @Test
    void listUsers_returnsList() {
        User user = new User("Ivan", "ivan@example.com", 30);
        UserService service = new UserService(daoReturning(Optional.of(user)));
        assertEquals(1, service.listUsers().size());
    }
}
