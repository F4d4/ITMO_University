package org.example.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.AdType;
import org.example.entity.MonetizationType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class MonetizationMethodRequest {

    @NotNull(message = "Тип монетизации обязателен (AD или SUBSCRIPTION)")
    private MonetizationType type;

    /**
     * Обязателен при type = AD. Значения: PRE_ROLL, MID_ROLL, POST_ROLL
     */
    private AdType adType;

    /**
     * Название рекламы. Обязателен при type = AD. Проходит проверку на запрещённые слова.
     */
    private String adName;

    /**
     * Цена подписки. Обязательна при type = SUBSCRIPTION
     */
    @DecimalMin(value = "0.01", message = "Цена подписки должна быть больше 0")
    private BigDecimal subscriptionPrice;
}
