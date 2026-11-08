package com.example.notifications.notification;

import java.util.Arrays;
import java.util.Optional;

public enum Operation {
  CREATED,
  DELETED;

  public static Optional<Operation> from(String value) {
    return Arrays.stream(values())
        .filter(op -> op.name().equalsIgnoreCase(value))
        .findFirst();
  }
}