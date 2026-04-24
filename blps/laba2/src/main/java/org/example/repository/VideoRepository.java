package org.example.repository;

import org.example.entity.Video;
import org.example.entity.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByUserId(Long userId);
    List<Video> findByUserIdAndStatus(Long userId, VideoStatus status);
    boolean existsByUserIdAndStatus(Long userId, VideoStatus status);
    List<Video> findByStatus(VideoStatus status);
}
