package com.lifeos.goal.api;

import java.util.UUID;

/** API công khai của module goal (chỉ đọc). */
public interface GoalApi {

    /** Goal còn sống của user, khóa FOR SHARE trong transaction ghi của bên gọi (note_link, D189). */
    boolean lockForShare(UUID userId, UUID goalId);

    /** Goal còn sống của user (đọc). */
    boolean exists(UUID userId, UUID goalId);
}
