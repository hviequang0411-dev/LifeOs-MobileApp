package com.lifeos.notification.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.notification.dto.CreateReminderRequest;
import com.lifeos.notification.dto.NotificationResponse;
import com.lifeos.notification.dto.PreferencesRequest;
import com.lifeos.notification.dto.PreferencesResponse;
import com.lifeos.notification.dto.PushTokenRequest;
import com.lifeos.notification.dto.ReminderResponse;
import com.lifeos.notification.dto.UnreadCountResponse;
import com.lifeos.notification.dto.UpdateReminderRequest;
import com.lifeos.notification.service.InboxService;
import com.lifeos.notification.service.PreferenceService;
import com.lifeos.notification.service.PushTokenService;
import com.lifeos.notification.service.ReminderCommandService;
import com.lifeos.notification.service.ReminderQueryService;
import com.lifeos.notification.service.UnsubscribeService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reminder, hộp thư, tùy chọn, token push, hủy đăng ký email (PHASE_4 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class NotificationController {

    private final CurrentUser currentUser;
    private final ReminderCommandService reminders;
    private final ReminderQueryService reminderQueries;
    private final InboxService inbox;
    private final PreferenceService preferences;
    private final PushTokenService pushTokens;
    private final UnsubscribeService unsubscribe;

    NotificationController(CurrentUser currentUser, ReminderCommandService reminders,
            ReminderQueryService reminderQueries, InboxService inbox, PreferenceService preferences,
            PushTokenService pushTokens, UnsubscribeService unsubscribe) {
        this.currentUser = currentUser;
        this.reminders = reminders;
        this.reminderQueries = reminderQueries;
        this.inbox = inbox;
        this.preferences = preferences;
        this.pushTokens = pushTokens;
        this.unsubscribe = unsubscribe;
    }

    // ---------------------------------------------------------------- reminder

    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ReminderResponse.class)))
    @PostMapping("/reminders")
    ResponseEntity<ReminderResponse> createReminder(@Valid @RequestBody CreateReminderRequest r) {
        ReminderResponse created = reminders.create(currentUser.userId(), r.id(), Patch.of(r.targetType()),
                Patch.of(r.targetId()), new ReminderCommandService.Fields(Patch.of(r.mode()),
                        Patch.of(r.offsetMinutes()), Patch.of(r.localTime()), Patch.of(r.localDate()),
                        Patch.of(r.repeatRule()), Patch.of(r.urgent()), Patch.absent()), null);
        return ResponseEntity.created(URI.create("/api/v1/reminders/" + created.id())).body(created);
    }

    @GetMapping("/reminders")
    PageResponse<ReminderResponse> listReminders(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return reminderQueries.list(currentUser.userId(), limit, cursor);
    }

    @GetMapping("/reminders/{id}")
    ReminderResponse getReminder(@PathVariable UUID id) {
        return reminderQueries.get(currentUser.userId(), id);
    }

    @PatchMapping("/reminders/{id}")
    ReminderResponse updateReminder(@PathVariable UUID id, @Valid @RequestBody UpdateReminderRequest r) {
        return reminders.update(currentUser.userId(), id, r.version(), Patch.absent(), Patch.absent(),
                new ReminderCommandService.Fields(r.mode(), r.offsetMinutes(), r.localTime(), r.localDate(),
                        r.repeatRule(), r.urgent(), Patch.absent()), null);
    }

    @PostMapping("/reminders/{id}/pause")
    ReminderResponse pause(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return reminders.pause(currentUser.userId(), id, r.version());
    }

    @PostMapping("/reminders/{id}/resume")
    ReminderResponse resume(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return reminders.resume(currentUser.userId(), id, r.version());
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/reminders/{id}")
    ResponseEntity<Void> deleteReminder(@PathVariable UUID id, @RequestParam long version) {
        reminders.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- hộp thư

    @GetMapping("/notifications")
    PageResponse<NotificationResponse> list(@RequestParam(required = false) String unread,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return inbox.list(currentUser.userId(), unread, limit, cursor);
    }

    @GetMapping("/notifications/unread-count")
    UnreadCountResponse unreadCount() {
        return inbox.unreadCount(currentUser.userId());
    }

    @PatchMapping("/notifications/{id}/read")
    NotificationResponse markRead(@PathVariable UUID id) {
        return inbox.markRead(currentUser.userId(), id);
    }

    @ApiResponse(responseCode = "204", description = "Đã đánh dấu đọc tất cả")
    @PostMapping("/notifications/read-all")
    ResponseEntity<Void> markAllRead() {
        inbox.markAllRead(currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa")
    @DeleteMapping("/notifications/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        inbox.delete(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications/preferences")
    PreferencesResponse preferences() {
        return preferences.get(currentUser.userId());
    }

    @PutMapping("/notifications/preferences")
    PreferencesResponse replacePreferences(@Valid @RequestBody PreferencesRequest r) {
        return preferences.replace(currentUser.userId(), r.items());
    }

    /** Công khai (D143): token ký HMAC; chỉ tắt email một loại; idempotent; 20 yêu cầu/giờ/IP. */
    @ApiResponse(responseCode = "204", description = "Đã hủy nhận email loại này")
    @GetMapping("/notifications/unsubscribe")
    ResponseEntity<Void> unsubscribe(@RequestParam String token, HttpServletRequest http) {
        unsubscribe.unsubscribe(token, http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- token push

    @ApiResponse(responseCode = "204", description = "Đã đăng ký token")
    @PutMapping("/devices/{id}/push-token")
    ResponseEntity<Void> registerToken(@PathVariable UUID id, @Valid @RequestBody PushTokenRequest r) {
        pushTokens.register(currentUser.userId(), id, r.token(), r.platform());
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = "204", description = "Đã gỡ token")
    @DeleteMapping("/devices/{id}/push-token")
    ResponseEntity<Void> unregisterToken(@PathVariable UUID id) {
        pushTokens.unregister(currentUser.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
