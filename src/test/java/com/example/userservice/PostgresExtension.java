package com.example.userservice;

import com.example.userservice.util.HibernateUtil;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Starts one PostgreSQL Testcontainer per JVM and points Hibernate at it.
 *
 * <p>The container is shared by all test classes to keep the suite fast, but
 * the Hibernate {@code SessionFactory} is closed after every test class, so a
 * class never inherits an open factory (or its cached state) from another.
 */
public class PostgresExtension
    implements BeforeAllCallback, AfterAllCallback {

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

  @Override
  public synchronized void afterAll(ExtensionContext context) {
    HibernateUtil.shutdown();
  }
}
