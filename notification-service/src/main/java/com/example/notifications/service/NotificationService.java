package com.example.notifications.service;

import com.example.notifications.config.NotificationProperties;
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

    public void send(String operation, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.from());
        message.setTo(email);
        message.setSubject(composer.subjectFor(operation));
        message.setText(composer.bodyFor(operation, email));
        mailSender.send(message);
    }
}