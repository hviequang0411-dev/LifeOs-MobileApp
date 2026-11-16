package com.lifeos.analytics.service;

import java.util.UUID;

/** Một thẻ dashboard độc lập (D196). Ném lỗi ⇒ thẻ ERROR; module không có ⇒ không đăng ký thẻ (UNAVAILABLE). */
public interface DashboardCard {

    String id();

    Object compute(UUID userId);
}
