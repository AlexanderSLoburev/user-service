package com.example.userservice.repository;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  // Is this email address already taken?
  boolean existsByEmail(String email);
  // Does the database contain a user with the email address email whose id is
  // NOT equal to id?
  boolean existsByEmailAndIdNot(String email, Long id);
}