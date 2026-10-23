package com.example.userservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized management of SessionFactory.
 *
 * Contract for the team:
 *  - There is ONE SessionFactory for the entire application. Sessions must be
 * obtained only as follows: try (Session s =
 * HibernateUtil.getSessionFactory().openSession()) { ...
 * }
 *  - Closure is managed solely by the App via shutdown() in the finally block.
 */
public final class HibernateUtil {

  private static final Logger log =
      LoggerFactory.getLogger(HibernateUtil.class);

  private static final List<String> CONNECTION_KEYS = List.of(
      "hibernate.connection.url", "hibernate.connection.username",
      "hibernate.connection.password", "hibernate.connection.driver_class");

  private static SessionFactory factory;

  private HibernateUtil() {
    // no instances
  }

  /**
   * Returns the factory for the entire application. Lazy initialization:
   * On the first call, it reads hibernate.cfg.xml and db.properties (or system
   * overrides) and (if hbm2ddl.auto=validate) validates the User entity against
   * the live database.
   */
  public static synchronized SessionFactory getSessionFactory() {
    if (factory == null || !factory.isOpen()) {
      factory = buildSessionFactory();
    }
    return factory;
  }

  /** Releases the factory. Safe to call any number of times. */
  public static synchronized void shutdown() {
    if (factory != null) {
      if (factory.isOpen()) {
        factory.close();
        log.info("SessionFactory closed");
      }
      factory = null;
    }
  }

  private static SessionFactory buildSessionFactory() {
    Configuration cfg = new Configuration().configure(); // hibernate.cfg.xml
    cfg.addProperties(loadDbProperties()); // secrets from db.properties
    cfg.addProperties(loadSystemOverrides());

    if (cfg.getProperty("hibernate.connection.url") == null) {
      throw new IllegalStateException(
          "hibernate.connection.url did not make it into the configuration. "
          + "Set hibernate.connection.* system properties or provide "
          + "db.properties with the same keys.");
    }

    SessionFactory built = cfg.buildSessionFactory();
    log.info("SessionFactory initialized");
    return built;
  }

  private static Properties loadDbProperties() {
    Properties props = new Properties();
    try (InputStream in =
             HibernateUtil.class.getClassLoader().getResourceAsStream(
                 "db.properties")) {
      if (in == null) {
        log.warn("db.properties was not found in the classpath; "
                 + "falling back to hibernate.connection.* system properties");
        return props;
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read db.properties", e);
    }
    return props;
  }

  private static Properties loadSystemOverrides() {
    Properties props = new Properties();
    for (String key : CONNECTION_KEYS) {
      String value = System.getProperty(key);
      if (value != null && !value.isBlank()) {
        props.setProperty(key, value);
      }
    }
    return props;
  }
}
