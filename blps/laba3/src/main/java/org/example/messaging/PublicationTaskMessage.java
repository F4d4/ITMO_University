package org.example.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationTaskMessage {
    private Long videoId;
    private String videoTitle;
    private String nodeOrigin;
}
