package org.example.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.AccessType;
import org.example.entity.AudienceType;

@Getter
@Setter
@NoArgsConstructor
public class VideoPublishRequest {

    @NotNull(message = "Тип аудитории обязателен (ALL_AGES или ADULTS_ONLY)")
    private AudienceType audienceType;

    @NotNull(message = "Тип доступа обязателен (PUBLIC или PRIVATE)")
    private AccessType accessType;
}
