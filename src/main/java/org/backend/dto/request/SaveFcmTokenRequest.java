package org.backend.dto.request;

import lombok.Data;

@Data
public class SaveFcmTokenRequest {

    private String fcmToken;

    private String deviceType;
}