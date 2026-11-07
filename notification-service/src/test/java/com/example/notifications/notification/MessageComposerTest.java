package com.example.notifications.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for MessageComposer: no Spring, no Mockito — a pure function of
 * (operation, config). The three assertions pin the assignment's exact texts.
 */
class MessageComposerTest {

  private final MessageComposer composer = new MessageComposer(
      "ваш сайт", "Ваш аккаунт создан", "Ваш аккаунт удалён");

  @Test
  void when_operation_created_then_text_matches_assignment_exactly() {
    assertThat(composer.compose(Operation.CREATED).text())
        .isEqualTo(
            "Здравствуйте! Ваш аккаунт на сайте ваш сайт был успешно создан.");
    assertThat(composer.compose(Operation.CREATED).subject())
        .isEqualTo("Ваш аккаунт создан");
  }

  @Test
  void when_operation_deleted_then_text_matches_assignment_exactly() {
    assertThat(composer.compose(Operation.DELETED).text())
        .isEqualTo("Здравствуйте! Ваш аккаунт был удалён.");
    assertThat(composer.compose(Operation.DELETED).subject())
        .isEqualTo("Ваш аккаунт удалён");
  }

  @Test
  void when_site_name_configured_then_it_is_substituted() {
    MessageComposer prodComposer = new MessageComposer(
        "example.com", "Ваш аккаунт создан", "Ваш аккаунт удалён");

    assertThat(prodComposer.compose(Operation.CREATED).text())
        .isEqualTo("Здравствуйте! Ваш аккаунт на сайте example.com был " +
                   "успешно создан.");
  }
}