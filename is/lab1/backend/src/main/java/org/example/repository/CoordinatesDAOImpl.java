package org.example.repository;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.config.HibernateUtil;
import org.example.entity.Coordinates;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Реализация DAO для работы с Coordinates
 */
@Stateless
public class CoordinatesDAOImpl implements CoordinatesDAO {
    
    private static final Logger LOGGER = Logger.getLogger(CoordinatesDAOImpl.class.getName());
    
    @Inject
    private HibernateUtil hibernateUtil;
    
    @Override
    public Coordinates save(Coordinates coordinates) {
        Session session = null;
        Transaction tx = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            
            session.persist(coordinates);
            
            tx.commit();
            LOGGER.info("Coordinates успешно сохранены с ID: " + coordinates.getId());
            return coordinates;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            LOGGER.severe("Ошибка при сохранении Coordinates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось сохранить Coordinates", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    @Override
    public Optional<Coordinates> findById(Long id) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Coordinates coordinates = session.get(Coordinates.class, id);
            return Optional.ofNullable(coordinates);
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Coordinates по ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Coordinates по ID", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    @Override
    public Optional<Coordinates> findByXAndY(double x, long y) {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Coordinates> query = session.createQuery(
                    "FROM Coordinates c WHERE c.x = :x AND c.y = :y",
                    Coordinates.class);
            query.setParameter("x", x);
            query.setParameter("y", y);
            query.setMaxResults(1);
            List<Coordinates> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            LOGGER.severe("Ошибка при поиске Coordinates по x и y: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось найти Coordinates", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    @Override
    public List<Coordinates> findAll() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Coordinates> query = session.createQuery(
                    "FROM Coordinates c ORDER BY c.id", Coordinates.class);
            return query.getResultList();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при получении списка Coordinates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось получить список Coordinates", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    @Override
    public long count() {
        Session session = null;
        try {
            session = hibernateUtil.getSessionFactory().openSession();
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(c) FROM Coordinates c", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            LOGGER.severe("Ошибка при подсчете Coordinates: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось подсчитать количество Coordinates", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}

