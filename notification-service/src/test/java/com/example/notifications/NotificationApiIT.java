package com.example.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Direct IT routing: POST /api/notifications -> NotificationService -> SMTP.
 * GreenMail runs an SMTP server in the JVM (port 3025); Kafka is disabled -
 * the broker path is handled by KafkaToEmailIT.
 */
@SpringBootTest(
        properties = {"spring.mail.port=3025", // ServerSetupTest.SMTP port
                "spring.kafka.listener.auto-startup=false"})
@AutoConfigureMockMvc
class NotificationApiIT {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired private MockMvc mockMvc;

    @BeforeEach
    void cleanMailbox() {
        greenMail.reset();
    }

    @Test
    void when_notification_created_then_email_with_assignment_text_sent()
            throws Exception {
        mockMvc
                .perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"operation": "CREATED", "email": "john@example.com"}
                """))
                .andExpect(status().isAccepted());

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();

        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(
                ((InternetAddress)message.getRecipients(Message.RecipientType.TO)[0])
                        .getAddress())
                .isEqualTo("john@example.com");
        assertThat(message.getSubject()).isEqualTo("Ваш аккаунт создан");
        assertThat(bodyOf(message))
                .isEqualTo("Здравствуйте! Ваш аккаунт на сайте site.com был успешно создан.");
    }

    @Test
    void when_notification_deleted_then_email_with_assignment_text_sent()
            throws Exception {
        mockMvc
                .perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"operation": "DELETED", "email": "john@example.com"}
                """))
                .andExpect(status().isAccepted());

        assertThat(greenMail.waitForIncomingEmail(5_000, 1)).isTrue();

        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(
                ((InternetAddress)message.getRecipients(Message.RecipientType.TO)[0])
                        .getAddress())
                .isEqualTo("john@example.com");
        assertThat(message.getSubject()).isEqualTo("Ваш аккаунт удалён");
        assertThat(bodyOf(message))
                .isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    }

    @Test
    void when_request_invalid_then_400_and_nothing_sent() throws Exception {
        mockMvc
                .perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"operation": "UPDATED", "email": "john@example.com"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc
                .perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"operation": "CREATED", "email": "not-an-email"}
                """))
                .andExpect(status().isBadRequest());

        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    /**
     * GreenMail returns the raw body as stored in the MIME part. When the body
     * contains non-ASCII characters, Jakarta Mail encodes it as base64
     * (Content-Transfer-Encoding: base64), and GreenMailUtil.getBody returns the
     * encoded string. Decode it before comparing with the expected text.
     */
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