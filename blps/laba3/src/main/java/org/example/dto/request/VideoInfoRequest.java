package org.example.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VideoInfoRequest {

    @NotBlank(message = "Название видео обязательно")
    private String title;

    @Size(max = 150, message = "Описание не должно превышать 150 символов")
    private String description;

    private String tags;
}
