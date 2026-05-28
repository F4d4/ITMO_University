package org.example.repository;

import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserId(Long userId);
    List<Video> findByUserIdAndStatus(Long userId, VideoStatus status);
    boolean existsByUserIdAndStatus(Long userId, VideoStatus status);
    List<Video> findByStatus(VideoStatus status);
    Optional<Video> findByBitrixTaskId(Long bitrixTaskId);
    Page<Video> findByStatusAndUpdatedAtBefore(VideoStatus status, LocalDateTime cutoff, Pageable pageable);
}
