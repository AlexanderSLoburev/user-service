package com.example.userservice.dao;

import com.example.userservice.entity.User;
import com.example.userservice.exception.DataAccessException;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.ExceptionTranslator;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.util.HibernateUtil;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate implementation of the UserDao contract (Task 2.2).
 *
 * Rules:
 *  - One session per method call; use try-with-resources (Session is not
 * thread-safe and accumulates persistence contexts - long-lived sessions are
 * a bad idea);
 *  - Reading - no explicit transaction (each read method is a single SELECT);
 *  - Writing: use a begin/commit transaction + rollback on failure
 *    (extracting the wrapper into executeInTransaction - Task 2.3);
 *  - Handling infrastructure exceptions - Task 2.4.
 */
public class UserDaoImpl implements UserDao {

  private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);

  // ==================== reading ====================

  @Override
  public Optional<User> findById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("findById: id can not be null");
    }
    log.debug("findById [{}]", id);
    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
      return Optional.ofNullable(session.find(User.class, id));
    } catch (RuntimeException e) {
      throw translate(e);
    }
  }

  @Override
  public Optional<User> findByEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("findByEmail: email can not be empty");
    }
    log.debug("findByEmail [{}]", email);
    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
      return session
          .createQuery("from User u where u.email = :email", User.class)
          .setParameter("email", email)
          .uniqueResultOptional();
    } catch (RuntimeException e) {
      throw translate(e);
    }
  }

  @Override
  public List<User> findAll() {
    log.debug("findAll");
    try (Session session = HibernateUtil.getSessionFactory().openSession()) {
      return session.createQuery("from User u order by u.id", User.class)
          .list();
    } catch (RuntimeException e) {
      throw translate(e);
    }
  }

  // ==================== writing ====================

  @Override
  public User save(User user) {
    if (user == null || user.getId() != null) {
      throw new IllegalArgumentException(
          "save: new user required (user != null, id == null)");
    }
    User saved = executeInTransaction(session -> {
      session.persist(user);
      return user;
    });
    log.info("User saved: id={}, email={}", saved.getId(), saved.getEmail());
    return saved;
  }

  @Override
  public User update(User user) {
    if (user == null || user.getId() == null) {
      throw new IllegalArgumentException(
          "update: a user with a filled-in id is required");
    }
    User updated = executeInTransaction(session -> {
      User managed = session.find(User.class, user.getId());
      if (managed == null) {
        throw new UserNotFoundException(user.getId());
      }
      managed.setName(user.getName());
      managed.setEmail(user.getEmail());
      managed.setAge(user.getAge());
      return managed;
    });
    log.info("User updated: id={}", updated.getId());
    return updated;
  }

  @Override
  public boolean deleteById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("deleteById: id can not be null");
    }
    boolean deleted = executeInTransaction(session -> {
      User managed = session.find(User.class, id);
      if (managed == null) {
        return false;
      }
      session.remove(managed);
      return true;
    });
    if (deleted) {
      log.info("User deleted: id={}", id);
    } else {
      log.debug("deleteById [{}]: not found", id);
    }
    return deleted;
  }

  // ==================== auxiliary ====================

  /**
   * A rollback if the transaction is still active. Never masks the original.
   */
  private static void rollbackIfActive(Transaction tx) {
    if (tx == null || !tx.isActive()) {
      return;
    }
    try {
      tx.rollback();
      log.debug("Transaction rolled back");
    } catch (RuntimeException rollbackFailure) {
      log.warn("Rollback failed", rollbackFailure);
    }
  }

  /**
   * Passes our own domain exceptions through; translates infrastructure ones.
   */
  private static RuntimeException translate(RuntimeException e) {
    if (e instanceof UserNotFoundException ||
        e instanceof DuplicateEmailException ||
        e instanceof DataAccessException) {
      return e;
    }
    return ExceptionTranslator.translate(e);
  }

  private static <T> T executeInTransaction(Function<Session, T> action) {
    Transaction tx = null;
    Session session = null;
    try {
      session = HibernateUtil.getSessionFactory().openSession();
      tx = session.beginTransaction();
      log.debug("Transaction begun");
      T result = action.apply(session);
      tx.commit();
      log.debug("Transaction committed");
      return result;
    } catch (RuntimeException e) {
      // Roll back BEFORE the session is closed, and never let a rollback
      // failure hide the original exception.
      rollbackIfActive(tx);
      throw translate(e);
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }
}