package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonetizationRequest {

    @NotNull(message = "ID пользователя обязателен")
    private Long userId;

    @NotNull(message = "ID видео обязателен")
    private Long videoId;

    @NotBlank(message = "Стратегия монетизации обязательна")
    private String strategy;

    private String configuration;
}
