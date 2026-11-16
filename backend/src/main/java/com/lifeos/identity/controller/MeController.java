package com.lifeos.identity.controller;

import com.lifeos.common.security.AuthenticatedUser;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.identity.dto.ChangePasswordRequest;
import com.lifeos.identity.dto.ProfileResponse;
import com.lifeos.identity.dto.SettingsResponse;
import com.lifeos.identity.dto.UpdateProfileRequest;
import com.lifeos.identity.dto.UpdateSettingsRequest;
import com.lifeos.identity.service.PasswordService;
import com.lifeos.identity.service.ProfileService;
import com.lifeos.identity.service.UpdateSettingsCommand;
import com.lifeos.identity.service.UserSettingsCommandService;
import com.lifeos.identity.service.UserSettingsQueryService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tài khoản và cài đặt của user hiện tại (API_CONVENTIONS §8.2). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final PasswordService passwordService;
    private final UserSettingsQueryService settingsQuery;
    private final UserSettingsCommandService settingsCommand;

    MeController(CurrentUser currentUser, ProfileService profileService, PasswordService passwordService,
            UserSettingsQueryService settingsQuery, UserSettingsCommandService settingsCommand) {
        this.currentUser = currentUser;
        this.profileService = profileService;
        this.passwordService = passwordService;
        this.settingsQuery = settingsQuery;
        this.settingsCommand = settingsCommand;
    }

    @GetMapping
    ProfileResponse profile() {
        return profileService.get(currentUser.userId());
    }

    @PatchMapping
    ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(currentUser.userId(), request.displayName());
    }

    /** 204. Đường dẫn theo API_CONVENTIONS §8.2 (/me/change-password). */
    @ApiResponse(responseCode = "204", description = "Đã đổi mật khẩu")
    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        AuthenticatedUser user = currentUser.require();
        passwordService.changePassword(user.userId(), user.deviceId(), request.currentPassword(),
                request.newPassword(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    SettingsResponse settings() {
        return settingsQuery.get(currentUser.userId());
    }

    @PatchMapping("/settings")
    SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return settingsCommand.update(currentUser.userId(), new UpdateSettingsCommand(request.version(),
                request.timezone(), request.locale(), request.defaultCurrency(), request.weekStartDay(),
                request.quietStart(), request.quietEnd(), request.showDetailsOnLockScreen()));
    }
}
