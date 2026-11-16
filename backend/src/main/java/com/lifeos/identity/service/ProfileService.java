package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.common.util.Patch;
import com.lifeos.identity.dto.ProfileResponse;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.mapper.IdentityMapper;
import com.lifeos.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ người dùng (UC-ID-08, FR-ID-07): xem và sửa tên hiển thị; email không đổi được ở Phase 1. users không phải
 * entity đồng bộ nên không có version/ChangeRecorder.
 */
@Service
public class ProfileService {

    static final int DISPLAY_NAME_MAX = 100;

    private final UserRepository users;
    private final IdentityMapper mapper;

    public ProfileService(UserRepository users, IdentityMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return mapper.toProfile(load(userId));
    }

    /** PATCH /me theo JSON Merge Patch: displayName vắng = không đổi; null/rỗng → 400 (trường không cho phép null). */
    @Transactional
    public ProfileResponse update(UUID userId, Patch<String> displayName) {
        User user = load(userId);
        if (displayName.present()) {
            String value = displayName.value() == null ? null : displayName.value().trim();
            if (value == null || value.isEmpty()) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "displayName", "REQUIRED",
                        "Tên hiển thị không được để trống.");
            }
            if (value.codePointCount(0, value.length()) > DISPLAY_NAME_MAX) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "displayName", "SIZE",
                        "Tên hiển thị tối đa " + DISPLAY_NAME_MAX + " ký tự.");
            }
            user.changeDisplayName(value);
        }
        return mapper.toProfile(users.saveAndFlush(user));
    }

    private User load(UUID userId) {
        // Access token hợp lệ của một user không còn tồn tại: coi như phiên không hợp lệ.
        return users.findById(userId).orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
    }
}
