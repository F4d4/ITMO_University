package org.example.repository;

import org.example.entity.Monetization;
import org.example.entity.MonetizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonetizationRepository extends JpaRepository<Monetization, Long> {
    List<Monetization> findByVideoId(Long videoId);
    List<Monetization> findByUserId(Long userId);
    Optional<Monetization> findByVideoIdAndStatus(Long videoId, MonetizationStatus status);
    boolean existsByVideoIdAndStatus(Long videoId, MonetizationStatus status);
}
