package com.example.userservice.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaUserEventPublisherTest {

  private static final String TOPIC = "user-events";

  @Mock private KafkaTemplate<String, UserEvent> kafkaTemplate;

  private KafkaUserEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new KafkaUserEventPublisher(kafkaTemplate, TOPIC);
  }

  @Test
  void when_user_created_then_event_published_with_email_as_key() {
    when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    publisher.publishCreated("john@example.com");

    verify(kafkaTemplate)
        .send(eq(TOPIC), eq("john@example.com"),
              eq(new UserEvent("CREATED", "john@example.com")));
  }

  @Test
  void when_user_deleted_then_event_published_with_email_as_key() {
    when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
        .thenReturn(CompletableFuture.completedFuture(null));

    publisher.publishDeleted("john@example.com");

    verify(kafkaTemplate)
        .send(eq(TOPIC), eq("john@example.com"),
              eq(new UserEvent("DELETED", "john@example.com")));
  }

  @Test
  void when_send_fails_then_exception_is_swallowed() {
    when(kafkaTemplate.send(anyString(), anyString(), any(UserEvent.class)))
        .thenReturn(CompletableFuture.failedFuture(
            new RuntimeException("broker unreachable")));

    assertThatCode(() -> publisher.publishCreated("john@example.com"))
        .doesNotThrowAnyException();
  }
}
