package com.example.userservice.client;

import com.example.userservice.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public NotificationClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = builder.baseUrl("http://notification-service").build();
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public boolean sendNotification(String operation, String email) {
        Boolean result = circuitBreakerFactory.create("notificationService").run(
                () -> {
                    restClient.post()
                            .uri("/api/notifications")
                            .body(new NotificationRequest(operation, email))
                            .retrieve()
                            .toBodilessEntity();
                    return true;
                },
                throwable -> {
                    log.warn("Fallback: notification-service unavailable for {}: {}",
                            email, throwable.getMessage());
                    return false;
                }
        );
        return Boolean.TRUE.equals(result);
    }
}