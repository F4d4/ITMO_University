package org.example.repository;

import org.example.entity.MonetizationMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonetizationMethodRepository extends JpaRepository<MonetizationMethod, Long> {
    List<MonetizationMethod> findByMonetizationId(Long monetizationId);
}
