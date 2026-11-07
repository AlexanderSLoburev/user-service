package com.example.notifications.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>The text is hard-coded (this is the task contract), and anything the team
 * might want to change without rebuilding (the site name, email subject lines)
 * is read from application.yaml.
 */
@Component
public class MessageComposer {
  private static final String MSG_ACCOUNT_CREATED =
      "Здравствуйте! Ваш аккаунт на сайте %s был успешно создан.";
  private static final String MSG_ACCOUNT_DELETED =
      "Здравствуйте! Ваш аккаунт был удалён.";

  private final String siteName;
  private final String subjectCreated;
  private final String subjectDeleted;

  public MessageComposer(
      @Value("${app.notification.site-name}") String siteName,
      @Value("${app.notification.subject-created}") String subjectCreated,
      @Value("${app.notification.subject-deleted}") String subjectDeleted) {
    this.siteName = siteName;
    this.subjectCreated = subjectCreated;
    this.subjectDeleted = subjectDeleted;
  }

  /**
   * Subject and body of the message for the operation. The sender's address is
   * part of the messaging infrastructure and is managed by NotificationService.
   */
  public ComposedMessage compose(Operation operation) {
    return switch (operation) {
      case CREATED ->
        new ComposedMessage(subjectCreated,
                            MSG_ACCOUNT_CREATED.formatted(siteName));
      case DELETED -> new ComposedMessage(subjectDeleted, MSG_ACCOUNT_DELETED);
    };
  }

  /** Completed email: subject + body. */
  public record ComposedMessage(String subject, String text) {
  }
}
