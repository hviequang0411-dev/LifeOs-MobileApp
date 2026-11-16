package com.lifeos.identity.controller;

import com.lifeos.common.security.AuthenticatedUser;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.identity.dto.DeviceListResponse;
import com.lifeos.identity.service.DeviceService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thiết bị của user hiện tại (API_CONVENTIONS §8.3). userId luôn lấy từ CurrentUser, không từ path/body. */
@RestController
@RequestMapping("/api/v1/devices")
class DeviceController {

    private final DeviceService deviceService;
    private final CurrentUser currentUser;

    DeviceController(DeviceService deviceService, CurrentUser currentUser) {
        this.deviceService = deviceService;
        this.currentUser = currentUser;
    }

    @GetMapping
    DeviceListResponse list() {
        AuthenticatedUser user = currentUser.require();
        return deviceService.list(user.userId(), user.deviceId());
    }

    /** 204; thiết bị không tồn tại hoặc của user khác → 404 DEVICE_NOT_FOUND (không phân biệt). */
    @ApiResponse(responseCode = "204", description = "Đã thu hồi thiết bị")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> revoke(@PathVariable UUID id, HttpServletRequest http) {
        AuthenticatedUser user = currentUser.require();
        deviceService.revoke(user.userId(), user.deviceId(), id, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
