package com.example.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
  // Only wrappers (Long, Integer), not primitives. age is nullable in the
  // database; a primitive int would throw an NPE if read as NULL, and a
  // long id cannot be unassigned.
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "name", nullable = false) private String name;
  @Column(name = "email", nullable = false, unique = true) private String email;
  @Column(name = "age") private Integer age;
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected User() {}

  public User(String name, String email, Integer age) {
    this.name = name;
    this.email = email;
    this.age = age;
  }

  public Long getId() { return id; }
  public String getName() { return name; }
  public String getEmail() { return email; }
  public Integer getAge() { return age; }
  public LocalDateTime getCreatedAt() { return createdAt; }

  public void setName(String name) { this.name = name; }
  public void setEmail(String email) { this.email = email; }
  public void setAge(Integer age) { this.age = age; }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      // null check — so the test can substitute its own datetime
      createdAt = LocalDateTime.now();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User other)) {
      return false;
    }
    // Unsaved entities (id == null) are not equal to anyone but themselves
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    // The hash should not change when the database assigns an ID
    return getClass().hashCode();
  }

  @Override
  public String toString() {
    return "User{id=%d, name='%s', email='%s', age=%s, createdAt=%s}"
        .formatted(id, name, email, age, createdAt);
  }
}