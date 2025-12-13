package org.example.repository;

import org.example.entity.ImportOperation;

import java.util.List;
import java.util.Optional;

/**
 * DAO интерфейс для работы с ImportOperation
 */
public interface ImportOperationDAO {
    
    ImportOperation save(ImportOperation operation);
    
    ImportOperation update(ImportOperation operation);
    
    Optional<ImportOperation> findById(Long id);
    
    List<ImportOperation> findAll();
    
    List<ImportOperation> findByUserId(Long userId);
}

