package com.lifeos.routine.api;

import java.util.UUID;

/** API công khai của module routine (chỉ đọc). */
public interface RoutineApi {

    /** Routine còn sống của user (khóa FOR SHARE trong transaction ghi của bên gọi). */
    boolean lockForShare(UUID userId, UUID routineId);
}
