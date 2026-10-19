package com.example.userservice.dao;

import com.example.userservice.entity.User;
import com.example.userservice.util.HibernateUtil;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class UserDaoImpl implements UserDao {

    @Override
    public User save(User user) {
        return executeInTransaction(session -> {
            session.persist(user);
            return user;
        });
    }

    @Override
    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.find(User.class, id));
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from User", User.class).list();
        }
    }

    @Override
    public User update(User user) {
        return executeInTransaction(session -> session.merge(user));
    }

    @Override
    public boolean deleteById(Long id) {
        return executeInTransaction(session -> {
            User user = session.find(User.class, id);
            if (user == null) {
                return false;
            }
            session.remove(user);
            return true;
        });
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from User u where u.email = :email", User.class)
                    .setParameter("email", email)
                    .uniqueResultOptional();
        }
    }

    <R> R executeInTransaction(Function<Session, R> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = action.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(tx, e);
                throw e;
            }
        }
    }

    private static void rollbackQuietly(Transaction tx, RuntimeException cause) {
        try {
            if (tx.getStatus().canRollback()) {
                tx.rollback();
            }
        } catch (RuntimeException rollbackError) {
            cause.addSuppressed(rollbackError);
        }
    }
}
