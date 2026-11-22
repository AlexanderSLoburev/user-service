package com.example.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EurekaServerApplicationTest {

    @Autowired(required = false)
    private EurekaServerAutoConfiguration eurekaServerAutoConfiguration;

    @Test
    void contextLoads() {
    }

    @Test
    void when_context_started_then_eureka_server_autoconfiguration_is_present() {
        assertThat(eurekaServerAutoConfiguration).isNotNull();
    }
}