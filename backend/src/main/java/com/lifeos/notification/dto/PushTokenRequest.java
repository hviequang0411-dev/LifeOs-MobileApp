package com.lifeos.notification.dto;

import jakarta.validation.constraints.NotNull;

/** PUT /devices/{id}/push-token. token là dữ liệu nhạy cảm (D147): toString không in. */
public record PushTokenRequest(@NotNull String token, @NotNull String platform) {

    @Override
    public String toString() {
        return "PushTokenRequest[platform=" + platform + "]";
    }
}
