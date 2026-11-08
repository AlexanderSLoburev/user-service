package com.example.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.notifications.event.UserEvent;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Full IT pipeline: Kafka (Testcontainers, apache/kafka, KRaft) ->
 * @KafkaListener (task 5.5)
 * -> NotificationService (task 5.4) -> SMTP (GreenMail). Publication uses the
 * same contract as user-service: key = email, JsonSerializer without type
 * headers.
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.mail.port=3025",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer"
})
class KafkaToEmailIT {

    @SuppressWarnings("resource")
    @Container
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${app.kafka.topic}") private String topic;

    @BeforeEach
    void cleanMailbox() {
        greenMail.reset();
    }

    @Test
    void when_user_created_event_then_email_with_assignment_text_sent()
            throws Exception {
        kafkaTemplate
                .send(topic, "john@example.com",
                        new UserEvent("CREATED", "john@example.com"))
                .get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            MimeMessage[] received = greenMail.getReceivedMessages();
            assertThat(received).hasSize(1);
            assertThat(((InternetAddress)received[0].getRecipients(
                    Message.RecipientType.TO)[0])
                    .getAddress())
                    .isEqualTo("john@example.com");
            assertThat(received[0].getSubject()).isEqualTo("Ваш аккаунт создан");
            assertThat(bodyOf(received[0]))
                    .isEqualTo("Здравствуйте! Ваш аккаунт на сайте site.com был "
                            + "успешно создан.");
        });
    }

    @Test
    void when_user_deleted_event_then_email_with_assignment_text_sent()
            throws Exception {
        kafkaTemplate
                .send(topic, "john@example.com",
                        new UserEvent("DELETED", "john@example.com"))
                .get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            MimeMessage[] received = greenMail.getReceivedMessages();
            assertThat(received).hasSize(1);
            assertThat(received[0].getSubject()).isEqualTo("Ваш аккаунт удалён");
            assertThat(bodyOf(received[0]))
                    .isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
        });
    }

    /** GreenMail returns base64 for non-ASCII bodies; decode before compare. */
    private static String bodyOf(MimeMessage message) throws Exception {
        String raw = GreenMailUtil.getBody(message);
        String compact = raw.replaceAll("\\s", "");
        if (!compact.isEmpty()
                && compact.matches("^[A-Za-z0-9+/]+={0,2}$")
                && compact.length() % 4 == 0) {
            try {
                return new String(
                        Base64.getDecoder().decode(compact), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return raw;
    }
}