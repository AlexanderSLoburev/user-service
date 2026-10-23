package com.example.userservice;

import com.example.userservice.util.HibernateUtil;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class PostgresExtension implements BeforeAllCallback {

  private static final Path SCHEMA =
      Path.of("docker", "init", "01-schema.sql");

  private static PostgreSQLContainer postgres;

  @Override
  public synchronized void beforeAll(ExtensionContext context) {
    if (postgres != null) {
      return;
    }

    String suffix = UUID.randomUUID().toString().replace("-", "");
    PostgreSQLContainer container = new PostgreSQLContainer("postgres:18");
    container.withDatabaseName("user_service_" + suffix);
    container.withUsername("test_user_" + suffix);
    container.withPassword(UUID.randomUUID().toString());
    container.withCopyFileToContainer(
        MountableFile.forHostPath(SCHEMA),
        "/docker-entrypoint-initdb.d/01-schema.sql");
    container.start();

    System.setProperty("hibernate.connection.url", container.getJdbcUrl());
    System.setProperty("hibernate.connection.username", container.getUsername());
    System.setProperty("hibernate.connection.password", container.getPassword());
    HibernateUtil.shutdown();

    Runtime.getRuntime().addShutdownHook(
        new Thread(container::close, "postgres-testcontainer-shutdown"));
    postgres = container;
  }
}
