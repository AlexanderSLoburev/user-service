package com.example.userservice.dao;

import com.example.userservice.entity.User;

import java.util.Optional;
import java.util.List;

public interface UserDao {

    User save(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
    User update(User user);
    boolean deleteById(Long id);
    Optional<User> findByEmail(String email);

}
