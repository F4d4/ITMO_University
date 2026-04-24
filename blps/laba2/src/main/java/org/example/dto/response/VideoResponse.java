package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.entity.AccessType;
import org.example.entity.AudienceType;
import org.example.entity.VideoStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String tags;
    private VideoStatus status;
    private AudienceType audienceType;
    private AccessType accessType;
    private String minioKey;
    private String bucketName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
