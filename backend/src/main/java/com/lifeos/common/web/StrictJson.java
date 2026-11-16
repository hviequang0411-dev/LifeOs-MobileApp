package com.lifeos.common.web;

/**
 * Đánh dấu DTO request của endpoint ghi nhạy cảm (mọi /auth/* và /me/change-password, Q14): trường JSON lạ bị từ
 * chối với 400 MALFORMED_REQUEST thay vì bị bỏ qua (SECURITY_RULES §9.4). DTO không đánh dấu vẫn bỏ qua trường lạ để
 * tương thích tiến lên.
 */
public interface StrictJson {
}
