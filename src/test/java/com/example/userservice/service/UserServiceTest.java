package com.example.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.userservice.dao.UserDao;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import java.lang.reflect.Field;
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
    void createUser_throwsDuplicateEmailWhenEmailAlreadyStored() {
        User existing = new User("Ivan", "ivan@example.com", 30);
        UserDao dao = new UserDao() {
            public User save(User u) { throw new AssertionError("save must not be called"); }
            public Optional<User> findById(Long id) { return Optional.empty(); }
            public Optional<User> findByEmail(String email) {
                assertEquals("ivan@example.com", email);
                return Optional.of(existing);
            }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        UserService service = new UserService(dao);
        assertThrows(DuplicateEmailException.class,
                () -> service.createUser("Petr", " IVAN@Example.COM ", 25));
    }

    @Test
    void createUser_normalizesEmail() {
        UserDao dao = new UserDao() {
            public User save(User u) { return u; }
            public Optional<User> findById(Long id) { return Optional.empty(); }
            public Optional<User> findByEmail(String email) { return Optional.empty(); }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        User created = new UserService(dao).createUser("Ivan", " Ivan@Example.COM ", 30);
        assertEquals("ivan@example.com", created.getEmail());
    }

    @Test
    void updateUser_throwsDuplicateEmailWhenEmailBelongsToAnotherUser() {
        User target = new User("Petr", "petr@example.com", 25);
        User existing = new User("Ivan", "ivan@example.com", 30);
        UserDao dao = new UserDao() {
            public User save(User u) { return u; }
            public Optional<User> findById(Long id) { return Optional.of(target); }
            public Optional<User> findByEmail(String email) {
                assertEquals("ivan@example.com", email);
                return Optional.of(existing);
            }
            public List<User> findAll() { return List.of(); }
            public User update(User u) { return u; }
            public boolean deleteById(Long id) { return false; }
        };
        UserService service = new UserService(dao);
        assertThrows(DuplicateEmailException.class,
                () -> service.updateUser(2L, "Petr", "IVAN@Example.COM", 25));
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

    // ==================== remaining branches ====================

    @Test
    void createUser_whenEmailIsNull_passesNullToDao() {
        StubUserDao dao = new StubUserDao();

        User created = new UserService(dao).createUser("Ivan", null, 30);

        assertNull(created.getEmail());
        assertNull(dao.lookedUpEmail);
        assertSame(created, dao.saved);
    }

    @Test
    void showUser_whenDaoFails_rethrowsDataAccessException() {
        DataAccessException failure = new DataAccessException("db down");
        StubUserDao dao = new StubUserDao();
        dao.findByIdFailure = failure;

        DataAccessException thrown = assertThrows(DataAccessException.class,
                () -> new UserService(dao).showUser(1L));

        assertSame(failure, thrown);
    }

    @Test
    void listUsers_whenDaoFails_rethrowsDataAccessException() {
        DataAccessException failure = new DataAccessException("db down");
        StubUserDao dao = new StubUserDao();
        dao.findAllFailure = failure;
        UserService service = new UserService(dao);

        DataAccessException thrown = assertThrows(DataAccessException.class,
                service::listUsers);

        assertSame(failure, thrown);
    }

    @Test
    void updateUser_whenUserNotFound_throwsWithoutUpdating() {
        StubUserDao dao = new StubUserDao();
        UserService service = new UserService(dao);

        assertThrows(UserNotFoundException.class,
                () -> service.updateUser(7L, "New", "new@example.com", 30));
        assertNull(dao.updated);
    }

    @Test
    void updateUser_whenEmailIsFree_updatesAndReturnsUser() {
        User existing = new User("Old", "old@example.com", 20);
        StubUserDao dao = new StubUserDao();
        dao.byId = Optional.of(existing);
        UserService service = new UserService(dao);

        User result = service.updateUser(7L, "New", " NEW@Example.COM ", 21);

        assertSame(existing, result);
        assertEquals("New", existing.getName());
        assertEquals("new@example.com", existing.getEmail());
        assertEquals(21, existing.getAge());
        assertSame(existing, dao.updated);
    }

    @Test
    void updateUser_whenEmailBelongsToSameUser_updatesAndReturnsUser() {
        User existing = new User("Old", "same@example.com", 20);
        setId(existing, 7L);
        StubUserDao dao = new StubUserDao();
        dao.byId = Optional.of(existing);
        dao.byEmail = Optional.of(existing);
        UserService service = new UserService(dao);

        User result = service.updateUser(7L, "New", " SAME@Example.COM ", 21);

        assertSame(existing, result);
        assertEquals("same@example.com", existing.getEmail());
        assertSame(existing, dao.updated);
    }

    @Test
    void updateUser_whenDaoUpdateThrowsUserNotFound_rethrows() {
        UpdateFixture fixture = updateFixture();
        UserNotFoundException failure = new UserNotFoundException(7L);
        fixture.dao.updateFailure = failure;

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class,
                () -> fixture.service.updateUser(
                        7L, "New", "new@example.com", 21));

        assertSame(failure, thrown);
    }

    @Test
    void updateUser_whenDaoUpdateThrowsDuplicateEmail_rethrows() {
        UpdateFixture fixture = updateFixture();
        DuplicateEmailException failure =
                new DuplicateEmailException("new@example.com");
        fixture.dao.updateFailure = failure;

        DuplicateEmailException thrown =
                assertThrows(DuplicateEmailException.class,
                        () -> fixture.service.updateUser(
                                7L, "New", "new@example.com", 21));

        assertSame(failure, thrown);
    }

    @Test
    void updateUser_whenDaoUpdateThrowsDataAccess_rethrows() {
        UpdateFixture fixture = updateFixture();
        DataAccessException failure = new DataAccessException("db down");
        fixture.dao.updateFailure = failure;

        DataAccessException thrown = assertThrows(DataAccessException.class,
                () -> fixture.service.updateUser(
                        7L, "New", "new@example.com", 21));

        assertSame(failure, thrown);
    }

    @Test
    void deleteUser_whenUserExists_deletesSuccessfully() {
        StubUserDao dao = new StubUserDao();

        new UserService(dao).deleteUser(9L);

        assertEquals(9L, dao.deletedId);
    }

    @Test
    void deleteUser_whenDaoFails_rethrowsDataAccessException() {
        DataAccessException failure = new DataAccessException("db down");
        StubUserDao dao = new StubUserDao();
        dao.deleteFailure = failure;
        UserService service = new UserService(dao);

        DataAccessException thrown = assertThrows(DataAccessException.class,
                () -> service.deleteUser(9L));

        assertSame(failure, thrown);
    }

    // ==================== auxiliary ====================

    private static UpdateFixture updateFixture() {
        User existing = new User("Old", "old@example.com", 20);
        StubUserDao dao = new StubUserDao();
        dao.byId = Optional.of(existing);
        return new UpdateFixture(new UserService(dao), dao);
    }

    private static void setId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private record UpdateFixture(UserService service, StubUserDao dao) {
    }

    /** A configurable DAO double for branch-specific scenarios. */
    private static final class StubUserDao implements UserDao {

        private Optional<User> byId = Optional.empty();
        private Optional<User> byEmail = Optional.empty();
        private User saved;
        private User updated;
        private Long deletedId;
        private String lookedUpEmail;
        private boolean deleted = true;
        private RuntimeException findByIdFailure;
        private RuntimeException findAllFailure;
        private RuntimeException updateFailure;
        private RuntimeException deleteFailure;

        @Override
        public User save(User user) {
            saved = user;
            return user;
        }

        @Override
        public Optional<User> findById(Long id) {
            if (findByIdFailure != null) {
                throw findByIdFailure;
            }
            return byId;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            lookedUpEmail = email;
            return byEmail;
        }

        @Override
        public List<User> findAll() {
            if (findAllFailure != null) {
                throw findAllFailure;
            }
            return List.of();
        }

        @Override
        public User update(User user) {
            if (updateFailure != null) {
                throw updateFailure;
            }
            updated = user;
            return user;
        }

        @Override
        public boolean deleteById(Long id) {
            if (deleteFailure != null) {
                throw deleteFailure;
            }
            deletedId = id;
            return deleted;
        }
    }
}
