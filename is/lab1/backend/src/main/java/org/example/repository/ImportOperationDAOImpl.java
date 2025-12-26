package org.example.repository;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.config.HibernateUtil;
import org.example.entity.ImportOperation;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Реализация DAO для работы с ImportOperation через Hibernate Session API
 */
@Stateless
public class ImportOperationDAOImpl implements ImportOperationDAO {

    private static final Logger LOGGER = Logger.getLogger(ImportOperationDAOImpl.class.getName());

    @Inject
    private HibernateUtil hibernateUtil;

    @Override
    public ImportOperation save(ImportOperation operation) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            session.persist(operation);
            
            tx.commit();
            LOGGER.info("ImportOperation сохранена с ID: " + operation.getId());
            return operation;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при сохранении ImportOperation: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить ImportOperation", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public ImportOperation update(ImportOperation operation) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            ImportOperation merged = session.merge(operation);
            
            tx.commit();
            LOGGER.info("ImportOperation обновлена с ID: " + operation.getId());
            return merged;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при обновлении ImportOperation: " + e.getMessage());
            throw new RuntimeException("Не удалось обновить ImportOperation", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public Optional<ImportOperation> findById(Long id) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            ImportOperation operation = session.get(ImportOperation.class, id);
            return Optional.ofNullable(operation);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске ImportOperation по ID: " + e.getMessage());
            throw new RuntimeException("Не удалось найти ImportOperation", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<ImportOperation> findAll() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<ImportOperation> query = session.createQuery(
                    "FROM ImportOperation o JOIN FETCH o.user ORDER BY o.createdAt DESC", ImportOperation.class);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка ImportOperation: " + e.getMessage());
            throw new RuntimeException("Не удалось получить список ImportOperation", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<ImportOperation> findByUserId(Long userId) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<ImportOperation> query = session.createQuery(
                    "FROM ImportOperation o JOIN FETCH o.user WHERE o.user.id = :userId ORDER BY o.createdAt DESC", 
                    ImportOperation.class);
            query.setParameter("userId", userId);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске ImportOperation по userId: " + e.getMessage());
            throw new RuntimeException("Не удалось найти ImportOperation", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}

