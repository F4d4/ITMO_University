package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = true)
    private String title;

    @Column(length = 150, nullable = true)
    private String description;

    @Column(nullable = true)
    private String tags;

    /**
     * Key объекта в MinIO (имя файла внутри бакета)
     */
    @Column(name = "minio_key", nullable = true)
    private String minioKey;

    /**
     * Имя бакета MinIO, где хранится файл
     */
    @Column(name = "bucket_name", nullable = true)
    private String bucketName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = true)
    private AudienceType audienceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = true)
    private AccessType accessType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
