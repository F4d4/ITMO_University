package org.example.repository;

import org.example.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * DAO интерфейс для работы с User
 */
public interface UserDAO {
    
    User save(User user);
    
    Optional<User> findById(Long id);
    
    Optional<User> findByUsername(String username);
    
    List<User> findAll();
    
    User getOrCreateUser(String username, boolean isAdmin);
}

