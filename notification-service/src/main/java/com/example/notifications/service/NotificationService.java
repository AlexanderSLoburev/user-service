package com.example.notifications.service;

import com.example.notifications.config.NotificationProperties;
import com.example.notifications.notification.MessageComposer;
import com.example.notifications.notification.MessageComposer.ComposedMessage;
import com.example.notifications.notification.Operation;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MessageComposer composer;
    private final NotificationProperties props;

    public NotificationService(JavaMailSender mailSender,
                               MessageComposer composer,
                               NotificationProperties props) {
        this.mailSender = mailSender;
        this.composer = composer;
        this.props = props;
    }

    public void send(Operation operation, String email) {
        ComposedMessage composed = composer.compose(operation);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.from());
        message.setTo(email);
        message.setSubject(composed.subject());
        message.setText(composed.text());
        mailSender.send(message);
    }
}