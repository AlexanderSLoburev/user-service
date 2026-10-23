package com.example.userservice.dao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.userservice.PostgresExtension;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.util.HibernateUtil;

/**
 * Integration tests for UserDaoImpl (Task 2.5).
 *
 * Prerequisite: a live database — `docker compose up -d`.
 *
 * Isolation: Before each test, the table is cleared using a bulk DELETE,
 * so the tests are independent of one another and of the order in which they
 * are run.
 */
@ExtendWith(PostgresExtension.class)
class UserDaoImplTest {

  private static UserDao dao;

  @BeforeAll
  static void createDao() {
    dao = new UserDaoImpl();
  }

  @BeforeEach
  void cleanUsersTable(TestInfo testInfo) {
    // The DB-down test needs PostgreSQL stopped, so it must not clean the
    // table (the cleanup itself would fail before the test even starts).
    if (testInfo.getTags().contains("db-down")) {
      return;
    }
    // bulk deletion
    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
      Transaction tx = session.beginTransaction();
      session.createMutationQuery("delete from User").executeUpdate();
      tx.commit();
    }
  }

  // ==================== save ====================

  @Test
  void when_saveNewUser_then_idAndCreatedAtAreAssigned() {
    User user = new User("Ivan Petrov", "ivan.petrov@example.com", 30);

    assertNull(user.getId());
    assertNull(user.getCreatedAt());

    User saved = dao.save(user);

    assertNotNull(saved.getId(),
                  "The id must be assigned by the database (IDENTITY)");
    assertNotNull(saved.getCreatedAt(),
                  "createdAt must be assigned by @PrePersist");

    assertTrue(Duration.between(saved.getCreatedAt(), LocalDateTime.now())
                       .abs()
                       .toMinutes() < 1,
               "createdAt must be 'now' (with a tolerance for accuracy)");
  }

  @Test
  void when_saveNewUser_then_rereadHasSameFields() {
    User saved = dao.save(new User("Maria Ivanova", "maria@example.com", 28));

    User reread = dao.findById(saved.getId()).orElseThrow();

    assertEquals(saved.getId(), reread.getId());
    assertEquals("Maria Ivanova", reread.getName());
    assertEquals("maria@example.com", reread.getEmail());
    assertEquals(28, reread.getAge());
    assertNotNull(reread.getCreatedAt());
  }

  @Test
  void
  when_saveWithDuplicateEmail_then_throwsDuplicateEmailAndNothingRemains() {
    long countBefore = dao.findAll().size();
    dao.save(new User("Ivan Petrov", "ivan.petrov@example.com", 30));

    DuplicateEmailException e = assertThrows(
        DuplicateEmailException.class,
        () -> dao.save(new User("Impostor", "ivan.petrov@example.com", 99)));

    assertNotNull(e.getMessage());
    assertEquals(countBefore + 1, dao.findAll().size(),
                 "Only the first user should remain");
  }

  @Test
  void when_saveNullUser_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> dao.save(null));
  }

  @Test
  void when_saveUserWithPresetId_then_throwsIllegalArgument() {
    User withId = new User("Hacker", "hacker@example.com", 20);
    setField(withId, "id", 123L);

    assertThrows(IllegalArgumentException.class, () -> dao.save(withId));
  }

  // ==================== findById / findByEmail ====================

  @Test
  void when_findByIdNonexistent_then_returnsEmpty() {
    assertTrue(dao.findById(999L).isEmpty());
  }

  @Test
  void when_findByIdNull_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> dao.findById(null));
  }

  @Test
  void when_findByEmailExisting_then_returnsThatUser() {
    dao.save(new User("Petr Sidorov", "petr@example.com", 40));

    Optional<User> found = dao.findByEmail("petr@example.com");

    assertTrue(found.isPresent());
    assertEquals("Petr Sidorov", found.get().getName());
  }

  @Test
  void when_findByEmailDiffersOnlyByCase_then_returnsThatUser() {
    User saved = dao.save(new User("Case User", "Case.User@Example.com", 30));

    Optional<User> found = dao.findByEmail("case.user@example.com");

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
  }

  @Test
  void when_findByEmailNonexistent_then_returnsEmpty() {
    assertTrue(dao.findByEmail("nobody@example.com").isEmpty());
  }

  @Test
  void when_findByEmailBlank_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> dao.findByEmail("   "));
  }

  // ==================== findAll ====================

  @Test
  void when_findAll_then_returnsAllSortedByIdAscending() {
    dao.save(new User("A", "a@example.com", 1));
    dao.save(new User("B", "b@example.com", 2));
    dao.save(new User("C", "c@example.com", 3));

    List<User> all = dao.findAll();

    assertEquals(3, all.size());
    List<Long> ids = all.stream().map(User::getId).toList();

    assertTrue(ids.get(0) < ids.get(1) && ids.get(1) < ids.get(2),
               "The ids must be in ascending order: " + ids);
  }

  @Test
  void when_findAllOnEmptyTable_then_returnsEmptyNotNullList() {
    List<User> all = dao.findAll();

    assertNotNull(all);
    assertTrue(all.isEmpty());
  }

  // ==================== update ====================

  @Test
  void when_updateExistingUser_then_fieldsChangeAndCreatedAtStaysSame() {
    User saved = dao.save(new User("Old Name", "old@example.com", 20));
    LocalDateTime createdAtBefore =
        dao.findById(saved.getId()).orElseThrow().getCreatedAt();

    saved.setName("New Name");
    saved.setEmail("new@example.com");
    saved.setAge(21);
    dao.update(saved);

    User reread = dao.findById(saved.getId()).orElseThrow();
    assertEquals("New Name", reread.getName());
    assertEquals("new@example.com", reread.getEmail());
    assertEquals(21, reread.getAge());
    assertEquals(createdAtBefore, reread.getCreatedAt());
  }

  @Test
  void when_updateDeletedUser_then_throwsUserNotFound() {
    User saved = dao.save(new User("Deleted", "deleted@example.com", 20));
    dao.deleteById(saved.getId());

    UserNotFoundException e =
        assertThrows(UserNotFoundException.class, () -> dao.update(saved));

    assertNotNull(e.getMessage());
  }

  @Test
  void
  when_updateToEmailTakenByOther_then_throwsDuplicateEmailAndNeitherChanged() {
    User ivan = dao.save(new User("Ivan", "ivan@example.com", 30));
    User maria = dao.save(new User("Maria", "maria@example.com", 25));

    maria.setEmail("ivan@example.com");

    assertThrows(DuplicateEmailException.class, () -> dao.update(maria));
    // atomicity of rollback
    assertEquals("maria@example.com",
                 dao.findById(maria.getId()).orElseThrow().getEmail());
    assertEquals("ivan@example.com",
                 dao.findById(ivan.getId()).orElseThrow().getEmail());
  }

  @Test
  void when_updateNullUser_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> dao.update(null));
  }

  @Test
  void when_updateUserWithoutId_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
                 () -> dao.update(new User("NoId", "noid@example.com", 1)));
  }

  // ==================== delete ====================

  @Test
  void when_deleteExistingUser_then_returnsTrueAndRowDisappears() {
    User saved = dao.save(new User("Doomed", "doomed@example.com", 50));

    assertTrue(dao.deleteById(saved.getId()));
    assertTrue(dao.findById(saved.getId()).isEmpty());
  }

  @Test
  void when_deleteSameUserTwice_then_secondCallReturnsFalse() {
    User saved = dao.save(new User("Once", "once@example.com", 33));

    assertTrue(dao.deleteById(saved.getId()));
    assertFalse(dao.deleteById(saved.getId()));
  }

  @Test
  void when_deleteByIdNull_then_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> dao.deleteById(null));
  }

  // ==================== infrastructure failure (manual) ====================

  @Test
  @Tag("db-down")
  @Disabled("Manual mode: docker compose stop postgres -> run only this "
            + "test. Cannot be automated: in the same JVM as the other tests, "
            +
            "the singleton factory is already connected to the live database.")
  void
  when_dbIsDown_then_throwsDataAccessException() {
    DataAccessException e =
        assertThrows(DataAccessException.class, () -> dao.findAll());

    assertNotNull(
        e.getCause(),
        "the translation must preserve the original reason for the log entry");
  }

  // ==================== auxiliary ====================

  private static void setField(User user, String name, Object value) {
    try {
      var field = User.class.getDeclaredField(name);
      field.setAccessible(true);
      field.set(user, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}