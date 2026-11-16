package com.lifeos.common.device;

import java.util.UUID;

/**
 * Đảo phụ thuộc identity → notification (PHASE_4 D139): identity gọi khi một thiết bị bị thu hồi (DELETE /devices/{id})
 * hoặc đăng xuất, trong CÙNG transaction; module notification gỡ token push của thiết bị đó.
 */
public interface DeviceLifecycleListener {

    void onDeviceSignedOut(UUID userId, UUID deviceId);
}
