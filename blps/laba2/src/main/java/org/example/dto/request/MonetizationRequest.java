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

    // userId не передаётся — берётся из JWT токена

    @NotNull(message = "ID видео обязателен")
    private Long videoId;

    @NotBlank(message = "Стратегия монетизации обязательна")
    private String strategy;

    private String configuration;
}
