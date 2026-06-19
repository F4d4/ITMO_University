package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "monetizations")
@Getter
@Setter
@NoArgsConstructor
public class Monetization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Выбранная стратегия монетизации (описание от пользователя)
     */
    @Column(nullable = false)
    private String strategy;

    /**
     * Начальная конфигурация монетизации (описание от пользователя)
     */
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonetizationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
