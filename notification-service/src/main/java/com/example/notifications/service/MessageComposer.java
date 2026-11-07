package com.example.notifications.service;

import com.example.notifications.config.NotificationProperties;
import org.springframework.stereotype.Component;

@Component
public class MessageComposer {

    private final NotificationProperties props;

    public MessageComposer(NotificationProperties props) {
        this.props = props;
    }

    public String composeCreated(String email) {
        return "Здравствуйте! Ваш аккаунт на сайте " + props.siteName()
                + " был успешно создан.";
    }

    public String composeDeleted(String email) {
        return "Здравствуйте! Ваш аккаунт был удалён.";
    }

    public String subjectFor(String operation) {
        return switch (operation) {
            case "CREATED" -> props.subjectCreated();
            case "DELETED" -> props.subjectDeleted();
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    public String bodyFor(String operation, String email) {
        return switch (operation) {
            case "CREATED" -> composeCreated(email);
            case "DELETED" -> composeDeleted(email);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }
}