package com.example.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Framework smoke test: the notification-service context is assembled in its
 * entirety: domain (MessageComposer, NotificationService), transport
 * (@KafkaListener), configuration (all @Value placeholders from
 * application.yaml are resolved).
 *
 * <p>No Docker, Kafka, or SMTP is required: the listener container is not
 * started (spring.kafka.listener.auto-startup=false), and JavaMailSender
 * opens a connection only at the moment of sending. Live paths (HTTP -> email,
 * broker -> email) are tested by NotificationApiIT and KafkaToEmailIT from the
 * task 5.7.
 */
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class NotificationServiceApplicationIT {

  @Autowired private KafkaListenerEndpointRegistry listenerRegistry;

  @Test
  void contextLoads() {
    // Implicit check: all beans have been created, and constructors with @Value
    // have received their values
  }

  @Test
  void when_context_started_then_kafka_listeners_are_not_auto_started() {
    assertThat(listenerRegistry.getListenerContainers())
        .allMatch(container -> !container.isAutoStartup());
  }
}