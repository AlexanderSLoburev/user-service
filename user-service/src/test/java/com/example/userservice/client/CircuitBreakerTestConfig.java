package com.example.userservice.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CircuitBreakerTestConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> notificationServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                        .circuitBreakerConfig(CircuitBreakerConfig.custom()
                                .slidingWindowSize(4)
                                .minimumNumberOfCalls(2)
                                .failureRateThreshold(50)
                                .waitDurationInOpenState(Duration.ofMillis(100))
                                .build())
                        .timeLimiterConfig(TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(1))
                                .build()),
                "notificationService");
    }
}