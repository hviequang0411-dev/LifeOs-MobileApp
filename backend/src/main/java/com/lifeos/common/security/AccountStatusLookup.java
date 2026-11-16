package com.lifeos.common.security;

import java.util.UUID;

/** Trạng thái tài khoản cho filter bảo mật (PHASE_7 D205); module identity cài đặt (đảo phụ thuộc). */
public interface AccountStatusLookup {

    /** Tài khoản đang trong thời gian chờ xóa. */
    boolean deletionPending(UUID userId);
}
