package com.example.notifications.kafka;

import com.example.notifications.event.UserEvent;
import com.example.notifications.notification.Operation;
import com.example.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

  private static final Logger log =
      LoggerFactory.getLogger(UserEventListener.class);

  private final NotificationService notificationService;

  public UserEventListener(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @KafkaListener(topics = "${app.kafka.topic}")
  public void onEvent(UserEvent event) {
    Operation.from(event.operation())
        .ifPresentOrElse(
            operation
            -> notificationService.send(operation.toString(), event.email()),
            ()
                -> log.warn("Unknown operation '{}' for {} — message skipped",
                            event.operation(), event.email()));
  }
}
