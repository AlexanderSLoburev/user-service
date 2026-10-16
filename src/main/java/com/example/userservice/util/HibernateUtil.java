package com.example.userservice.util;

import java.io.IOException;
import java.io.InputStream;
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

  private static SessionFactory factory;

  private HibernateUtil() {
    // no instances
  }

  /**
   * Returns the factory for the entire application. Lazy initialization:
   * On the first call, it reads hibernate.cfg.xml and db.properties
   * and (if hbm2ddl.auto=validate) validates the User entity against the live
   * database.
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

    if (cfg.getProperty("hibernate.connection.url") == null) {
      throw new IllegalStateException(
          "hibernate.connection.url did not make it into the configuration. "
          + "Check that db.properties contains hibernate.connection.* keys "
          + "and matches the code.");
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
        throw new IllegalStateException(
            "db.properties was not found in the classpath. "
            + "Copy db.properties.example to db.properties");
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read db.properties", e);
    }
    return props;
  }
}