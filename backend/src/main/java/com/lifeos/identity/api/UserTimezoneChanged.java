package com.lifeos.identity.api;

import java.util.UUID;

/**
 * Sự kiện miền: múi giờ của người dùng vừa đổi (PHASE_4 D134). Phát ĐỒNG BỘ trong transaction cập nhật user_settings,
 * sau khi flush và TRƯỚC change_logs của user_settings (D86): module notification tính lại next_fire_at của các
 * reminder theo giờ địa phương trong cùng transaction.
 */
public record UserTimezoneChanged(UUID userId, String timezone) {
}
