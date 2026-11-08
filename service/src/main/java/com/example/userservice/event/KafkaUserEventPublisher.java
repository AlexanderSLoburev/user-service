package com.example.userservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaUserEventPublisher implements UserEventPublisher {

  private static final Logger log =
      LoggerFactory.getLogger(KafkaUserEventPublisher.class);

  private static final String OPERATION_CREATED = "CREATED";
  private static final String OPERATION_DELETED = "DELETED";

  private final KafkaTemplate<String, UserEvent> kafkaTemplate;
  private final String topic;

  public KafkaUserEventPublisher(KafkaTemplate<String, UserEvent> kafkaTemplate,
                                 @Value("${app.kafka.topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Override
  public void publishCreated(String email) {
    send(OPERATION_CREATED, email);
  }

  @Override
  public void publishDeleted(String email) {
    send(OPERATION_DELETED, email);
  }

  private void send(String operation, String email) {
    kafkaTemplate.send(topic, email, new UserEvent(operation, email))
        .whenComplete((result, failure) -> {
          if (failure != null) {
            // Delivery failed — log the error and DO NOT abort the user's
            // operation
            log.error("Failed to publish {} event for {}", operation, email,
                      failure);
          }
        });
  }
}
