package org.backend.dto;

import lombok.*;
import org.backend.enums.NotificationPriority;
import org.backend.enums.NotificationType;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationEvent {

    private Long userId;

    private String title;

    private String body;

    //private NotificationType type;

    //private NotificationPriority priority;

    //private String deepLink;

    private Map<String,String> data;

}