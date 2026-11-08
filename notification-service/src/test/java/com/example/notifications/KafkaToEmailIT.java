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
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
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
@SpringBootTest(properties = "spring.mail.port=3025")
class KafkaToEmailIT {

  @SuppressWarnings("resource")
  // the container closes the Testcontainers extension in afterAll
  @Container
  @ServiceConnection
  static final KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

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
      assertThat(GreenMailUtil.getBody(received[0]))
          .isEqualTo("Здравствуйте! Ваш аккаунт на сайте ваш сайт был "
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
      assertThat(GreenMailUtil.getBody(received[0]))
          .isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    });
  }
}