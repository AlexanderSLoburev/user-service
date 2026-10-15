package com.example.userservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;


class UserTest {

  private static void setField(User user, String name, Object value)
      throws Exception {
    Field field = User.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(user, value);
  }

  @Test
  void unsavedUsersAreNeverEqual() {
    User first = new User("Ivan", "ivan@example.com", 30);
    User second = new User("Ivan", "ivan@example.com", 30);

    assertEquals(first, first);
    // Both have `id == null`, so they are not equal
    assertNotEquals(first, second);
    assertNotEquals(first, null);
    assertNotEquals(first, "user");
  }

  @Test
  void savedUsersAreEqualByIdOnly() throws Exception {
    User first = new User("Ivan", "ivan@example.com", 30);
    User second = new User("Другое имя", "other@example.com", 99);
    setField(first, "id", 42L);
    setField(second, "id", 42L);

    assertEquals(first, second); // equality based on ID only

    setField(second, "id", 43L);
    assertNotEquals(first, second);
  }

  @Test
  void hashCodeDoesNotChangeWhenIdAssigned() throws Exception {
    User user = new User("Ivan", "ivan@example.com", 30);
    int before = user.hashCode();
    setField(user, "id", 42L);
    assertEquals(before, user.hashCode());
  }

  @Test
  void prePersistSetsCreatedAtOnlyOnce() throws Exception {
    User user = new User("Ivan", "ivan@example.com", 30);
    assertNull(user.getCreatedAt());

    user.prePersist();
    assertNotNull(user.getCreatedAt());

    LocalDateTime forced = LocalDateTime.of(2026, 1, 1, 12, 0);
    setField(user, "createdAt", forced);
    user.prePersist(); // A subsequent call does not overwrite createdAt
    assertEquals(forced, user.getCreatedAt());
  }

  @Test
  void toStringContainsMainFields() {
    User user = new User("Ivan", "ivan@example.com", 30);
    String text = user.toString();
    assertTrue(text.contains("Ivan") && text.contains("ivan@example.com"));
  }
}