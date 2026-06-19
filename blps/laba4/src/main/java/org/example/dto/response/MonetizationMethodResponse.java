package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.AdType;
import org.example.entity.MethodStatus;
import org.example.entity.MonetizationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationMethodResponse {
    private Long id;
    private Long monetizationId;
    private MonetizationType type;
    private AdType adType;
    private String adName;
    private BigDecimal subscriptionPrice;
    private MethodStatus status;
    private String tags;
    private LocalDateTime createdAt;
}
