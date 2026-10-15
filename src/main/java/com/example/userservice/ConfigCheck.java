package com.example.userservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


public class ConfigCheck {

  public static void main(String[] args) {
    // hibernate.cfg.xml
    Configuration cfg = new Configuration().configure();

    // Secrets from db.properties — Load the file once
    cfg.addProperties(loadDbProperties());

    // Fail-fast: if the keys didn't make it through, display a clear message
    // instead of a stack trace from deep within Hibernate
    if (cfg.getProperty("hibernate.connection.url") == null) {
      throw new IllegalStateException(
          "The hibernate.connection.url property was not included in the "
          + "configuration. Please verify that db.properties contains "
          + "hibernate.connection.* keys (not db.*) and that it matches "
          + "the code.");
    }

    // The Validation Itself: Connecting and Validating the User Entity
    try (SessionFactory factory = cfg.buildSessionFactory()) {
      System.out.println("OK: SessionFactory has been initialized; the User "
                         + "has been validated against the users table");
    }
  }

  private static Properties loadDbProperties() {
    Properties props = new Properties();
    try (InputStream in =
             ConfigCheck.class.getClassLoader().getResourceAsStream(
                 "db.properties")) {
      if (in == null) {
        throw new IllegalStateException(
            "db.properties was not found in the classpath—copy "
            + "db.properties.example to db.properties");
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read db.properties", e);
    }
    return props;
  }
}