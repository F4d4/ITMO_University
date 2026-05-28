package org.example.repository;

import org.example.entity.MethodStatus;
import org.example.entity.MonetizationMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonetizationMethodRepository extends JpaRepository<MonetizationMethod, Long> {
    List<MonetizationMethod> findByMonetizationId(Long monetizationId);
    List<MonetizationMethod> findByStatus(MethodStatus status);
    Optional<MonetizationMethod> findByBitrixTaskId(Long bitrixTaskId);
}
