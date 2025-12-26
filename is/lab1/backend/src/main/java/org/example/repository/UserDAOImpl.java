package org.example.repository;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.config.HibernateUtil;
import org.example.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Реализация DAO для работы с User через Hibernate Session API
 */
@Stateless
public class UserDAOImpl implements UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Override
    public User save(User user) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            session.persist(user);
            
            tx.commit();
            LOGGER.info("User сохранен с ID: " + user.getId());
            return user;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при сохранении User: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить User", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            User user = session.get(User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске User по ID: " + e.getMessage());
            throw new RuntimeException("Не удалось найти User", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<User> query = session.createQuery(
                    "FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            List<User> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске User по username: " + e.getMessage());
            throw new RuntimeException("Не удалось найти User", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<User> findAll() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<User> query = session.createQuery("FROM User", User.class);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка User: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список User", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public User getOrCreateUser(String username, boolean isAdmin) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            Query<User> query = session.createQuery(
                    "FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            List<User> results = query.getResultList();
            
            User user;
            if (results.isEmpty()) {
                user = new User(username, isAdmin);
                session.persist(user);
                LOGGER.info("Создан новый User: " + username + " (admin=" + isAdmin + ")");
            } else {
                user = results.get(0);
                // Обновляем роль если изменилась
                if (user.isAdmin() != isAdmin) {
                    user.setAdmin(isAdmin);
                    session.merge(user);
                    LOGGER.info("Обновлена роль User: " + username + " (admin=" + isAdmin + ")");
                }
            }
            
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при получении/создании User: " + e.getMessage());
            throw new RuntimeException("Не удалось получить/создать User", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}

