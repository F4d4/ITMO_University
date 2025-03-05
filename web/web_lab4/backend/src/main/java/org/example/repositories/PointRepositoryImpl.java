package org.example.repositories;

import org.example.entities.Point;
import org.example.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceUnit;
import java.util.List;

@ApplicationScoped
public class PointRepositoryImpl implements PointRepository {

    @PersistenceUnit(unitName = "primary")
    private EntityManagerFactory emf;

    @Override
    public void createPoint(Point point) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(point);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public List<Point> findPointsByUser(User user) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT p FROM Point p WHERE p.user = :user", Point.class)
                    .setParameter("user", user)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
