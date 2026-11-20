package com.example.userservice.event;

public interface UserEventPublisher {
  void publishCreated(String email);
  void publishDeleted(String email);
}
