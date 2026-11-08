package com.example.notifications.kafka;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.notifications.event.UserEvent;
import com.example.notifications.notification.Operation;
import com.example.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private UserEventListener listener;

    @Test
    void when_event_is_created_then_notification_sent() {
        listener.onEvent(new UserEvent("CREATED", "john@example.com"));

        verify(notificationService).send(Operation.CREATED, "john@example.com");
    }

    @Test
    void when_event_is_deleted_then_notification_sent() {
        listener.onEvent(new UserEvent("DELETED", "john@example.com"));

        verify(notificationService).send(Operation.DELETED, "john@example.com");
    }

    @Test
    void when_operation_is_unknown_then_event_skipped() {
        listener.onEvent(new UserEvent("UPDATED", "john@example.com"));

        verifyNoInteractions(notificationService);
    }
}