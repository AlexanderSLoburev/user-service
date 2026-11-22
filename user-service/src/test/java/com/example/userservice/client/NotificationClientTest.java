package com.example.userservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.userservice.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@SpringBootTest (properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
        "eureka.client.enabled=false"
})
@Import(CircuitBreakerTestConfig.class)
class NotificationClientTest {

    @Autowired
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private NotificationClient client;

    @BeforeEach
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl("http://notification-service")).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/notifications")).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(NotificationRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        client = new NotificationClient(builder, circuitBreakerFactory);
    }

    @Test
    void when_send_success_then_returns_true() {
        assertThat(client.sendNotification("CREATED", "john@example.com")).isTrue();
    }

    @Test
    void when_send_fails_then_fallback_returns_false() {
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("boom"));
        assertThat(client.sendNotification("CREATED", "john@example.com")).isFalse();
    }
}