package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.MonetizationStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationResponse {
    private Long id;
    private Long videoId;
    private Long userId;
    private String strategy;
    private String configuration;
    private MonetizationStatus status;
    private LocalDateTime createdAt;
}
