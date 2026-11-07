package com.example.notifications.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notifications.config.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceTest {

    private JavaMailSender mailSender;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        NotificationProperties props = new NotificationProperties(
                "your-site.com",
                "noreply@your-site.com",
                "Аккаунт создан",
                "Аккаунт удалён"
        );
        MessageComposer composer = new MessageComposer(props);
        service = new NotificationService(mailSender, composer, props);
    }

    @Test
    void when_created_then_sendsEmailWithCreatedBody() {
        service.send("CREATED", "ivan@example.com");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("ivan@example.com");
        assertThat(msg.getFrom()).isEqualTo("noreply@your-site.com");
        assertThat(msg.getSubject()).isEqualTo("Аккаунт создан");
        assertThat(msg.getText()).isEqualTo(
                "Здравствуйте! Ваш аккаунт на сайте your-site.com был успешно создан.");
    }

    @Test
    void when_deleted_then_sendsEmailWithDeletedBody() {
        service.send("DELETED", "ivan@example.com");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("ivan@example.com");
        assertThat(msg.getFrom()).isEqualTo("noreply@your-site.com");
        assertThat(msg.getSubject()).isEqualTo("Аккаунт удалён");
        assertThat(msg.getText()).isEqualTo(
                "Здравствуйте! Ваш аккаунт был удалён.");
    }

    @Test
    void when_unknownOperation_then_throwsIllegalArgument() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.send("UNKNOWN", "ivan@example.com"));
    }
}
