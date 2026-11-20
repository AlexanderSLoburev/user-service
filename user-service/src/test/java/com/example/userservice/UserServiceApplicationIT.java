package com.example.userservice;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.userservice.event.UserEventPublisher;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserServiceApplicationIT {

  @SuppressWarnings("resource")
  @Container
  @ServiceConnection
  static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:17-alpine")
          .withInitScript("01-schema.sql");

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @MockitoBean private UserEventPublisher eventPublisher;

  @BeforeEach
  void cleanDatabase() {
    userRepository.deleteAll();
  }

  @Test
  void when_create_user_then_id_and_created_at_are_assigned() throws Exception {
    mockMvc
        .perform(post("/api/users")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "John", "email": "john@example.com", "age": 30}
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("John"))
        .andExpect(jsonPath("$.email").value("john@example.com"))
        .andExpect(jsonPath("$.age").value(30))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    verify(eventPublisher).publishCreated("john@example.com");
  }

  @Test
  void when_find_all_then_created_users_are_listed() throws Exception {
    createUser("john@example.com");
    createUser("alice@example.com");

    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath("$._embedded.users[0].email").value("john@example.com"))
        .andExpect(
            jsonPath("$._embedded.users[1].email").value("alice@example.com"))
        .andExpect(
            jsonPath("$._links.self.href").value("http://localhost/api/users"))
        .andExpect(jsonPath("$._embedded.users[0]._links.self.href")
                       .value("http://localhost/api/users/1"));
  }

  @Test
  void when_find_by_id_then_user_returned() throws Exception {
    long id = createUser("john@example.com");

    mockMvc.perform(get("/api/users/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value("John"))
        .andExpect(jsonPath("$._links.self.href")
                       .value("http://localhost/api/users/" + id))
        .andExpect(jsonPath("$._links.update.href")
                       .value("http://localhost/api/users/" + id))
        .andExpect(jsonPath("$._links.delete.href")
                       .value("http://localhost/api/users/" + id))
        .andExpect(jsonPath("$._links.users.href")
                       .value("http://localhost/api/users"));
  }

  @Test
  void when_find_unknown_id_then_404() throws Exception {
    mockMvc.perform(get("/api/users/{id}", 999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void when_update_user_then_changes_are_saved() throws Exception {
    long id = createUser("john@example.com");

    mockMvc
        .perform(put("/api/users/{id}", id)
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "John Smith", "email": "john.smith@example.com", "age": 31}
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("john.smith@example.com"));

    mockMvc.perform(get("/api/users/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("John Smith"))
        .andExpect(jsonPath("$.age").value(31));
  }

  @Test
  void when_create_with_duplicate_email_then_409() throws Exception {
    createUser("john@example.com");

    mockMvc
        .perform(post("/api/users")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "John Clone", "email": "john@example.com", "age": 40}
                                """))
        .andExpect(status().isConflict());

    verify(eventPublisher, times(1)).publishCreated("john@example.com");
  }

  @Test
  void when_update_with_email_of_other_user_then_409() throws Exception {
    long id = createUser("john@example.com");
    createUser("alice@example.com");

    mockMvc
        .perform(put("/api/users/{id}", id)
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "John", "email": "alice@example.com", "age": 30}
                                """))
        .andExpect(status().isConflict());
  }

  @Test
  void when_create_with_blank_name_then_400() throws Exception {
    mockMvc
        .perform(post("/api/users")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "", "email": "john@example.com", "age": 30}
                                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void when_create_with_invalid_email_then_400() throws Exception {
    mockMvc
        .perform(post("/api/users")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content("""
                                {"name": "John", "email": "not-an-email", "age": 30}
                                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void when_delete_user_then_it_is_gone() throws Exception {
    long id = createUser("john@example.com");

    mockMvc.perform(delete("/api/users/{id}", id))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/users/{id}", id))
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/api/users/{id}", id))
        .andExpect(status().isNotFound());

    verify(eventPublisher).publishDeleted("john@example.com");
  }

  private long createUser(String email) throws Exception {
    String response = mockMvc
                          .perform(post("/api/users")
                                       .contentType(MediaType.APPLICATION_JSON)
                                       .content("""
                                {"name": "John", "email": "%s", "age": 30}
                                """.formatted(email)))
                          .andExpect(status().isCreated())
                          .andReturn()
                          .getResponse()
                          .getContentAsString();
    return objectMapper.readTree(response).path("id").asLong();
  }
}