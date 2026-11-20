package com.example.userservice.event;

/**
 * @param operation "CREATED" | "DELETED"
 * @param email     the recipient of the letter; also the key to the message
 */
public record UserEvent(String operation, String email) {
}