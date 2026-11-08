package com.example.notifications.event;

/**
 * @param operation "CREATED" | "DELETED"
 * @param email      the recipient of the message; also the message key in Kafka
 */
public record UserEvent(String operation, String email) {
}