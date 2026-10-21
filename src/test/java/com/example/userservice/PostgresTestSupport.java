package com.example.userservice;

import com.example.userservice.util.HibernateUtil;
import java.nio.file.Path;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public abstract class PostgresTestSupport {

  private static final Path SCHEMA =
      Path.of("docker", "init", "01-schema.sql");

  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18")
          .withDatabaseName("user_service")
          .withUsername("postgres")
          .withPassword("postgres")
          .withCopyFileToContainer(
              MountableFile.forHostPath(SCHEMA),
              "/docker-entrypoint-initdb.d/01-schema.sql");

  static {
    POSTGRES.start();
    System.setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl());
    System.setProperty("hibernate.connection.username", POSTGRES.getUsername());
    System.setProperty("hibernate.connection.password", POSTGRES.getPassword());
    HibernateUtil.shutdown();
    Runtime.getRuntime().addShutdownHook(
        new Thread(POSTGRES::stop, "postgres-testcontainer-shutdown"));
  }
}
