package com.example.userservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.userservice.dto.NotificationRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4jBulkheadProvider;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.client.RestClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class NotificationClientTest {

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;
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

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        Resilience4jBulkheadProvider bulkheadProvider = new Resilience4jBulkheadProvider(
                new Resilience4jBulkheadConfigurationProperties(),
                CircuitBreakerRegistry.ofDefaults()
        );

        circuitBreakerFactory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry,
                timeLimiterRegistry,
                bulkheadProvider
        );
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