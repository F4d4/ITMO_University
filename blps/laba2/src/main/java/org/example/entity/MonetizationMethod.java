package org.example.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monetization_methods")
@Getter
@Setter
@NoArgsConstructor
public class MonetizationMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monetization_id", nullable = false)
    private Monetization monetization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonetizationType type;

    /**
     * Тип рекламы: PRE_ROLL, MID_ROLL, POST_ROLL. Заполняется только при type = AD
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = true)
    private AdType adType;

    /**
     * Название рекламного объявления. Заполняется только при type = AD
     */
    @Column(name = "ad_name", nullable = true)
    private String adName;

    /**
     * Цена подписки. Заполняется только при type = SUBSCRIPTION
     */
    @Column(name = "subscription_price", nullable = true, precision = 10, scale = 2)
    private BigDecimal subscriptionPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
