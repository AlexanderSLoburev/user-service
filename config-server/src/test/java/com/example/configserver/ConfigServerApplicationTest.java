package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
    }
}