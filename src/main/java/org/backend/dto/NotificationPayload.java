package org.backend.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationPayload {

    private Map<String,String> data;

}