package com.example.userservice.service;

import com.example.userservice.dto.UserCreateRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.dto.UserUpdateRequest;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<UserResponse> findAll() {
    return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
        .stream()
        .map(UserResponse::from)
        .toList();
  }

  public UserResponse findById(Long id) {
    return userRepository.findById(id)
        .map(UserResponse::from)
        .orElseThrow(() -> new UserNotFoundException(id));
  }

  @Transactional
  public UserResponse create(UserCreateRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new DuplicateEmailException(request.email());
    }
    User saved = userRepository.saveAndFlush(
        new User(request.name(), request.email(), request.age()));

    return UserResponse.from(saved);
  }

  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {
    User user = userRepository.findById(id).orElseThrow(
        () -> new UserNotFoundException(id));
    if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
      // Is this someone else's email address?
      throw new DuplicateEmailException(request.email());
    }
    user.setName(request.name());
    user.setEmail(request.email());
    user.setAge(request.age());

    return UserResponse.from(user);
  }

  @Transactional
  public void delete(Long id) {
    if (!userRepository.existsById(id)) {
      throw new UserNotFoundException(id);
    }
    userRepository.deleteById(id);
  }
}