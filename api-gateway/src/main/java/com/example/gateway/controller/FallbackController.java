package com.example.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

  @RequestMapping("/fallback/users")
  public ResponseEntity<Map<String, Object>> userFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of(
            "error", "USER_SERVICE_UNAVAILABLE", "message",
            "User service is temporarily unavailable, please retry later",
            "timestamp", Instant.now().toString()));
  }
}