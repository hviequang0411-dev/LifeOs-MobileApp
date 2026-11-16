package com.lifeos.sync.controller;

import com.lifeos.common.security.CurrentUser;
import com.lifeos.sync.dto.PullResponse;
import com.lifeos.sync.dto.PushRequest;
import com.lifeos.sync.dto.PushResponse;
import com.lifeos.sync.service.SyncPullService;
import com.lifeos.sync.service.SyncPushService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đồng bộ (API_CONVENTIONS §8.7, SYSTEM_ARCHITECTURE §4). userId luôn lấy từ CurrentUser. Controller chỉ kiểm cấu
 * trúc (SyncPushRequestParser) rồi giao cho service.
 */
@RestController
@RequestMapping("/api/v1/sync")
class SyncController {

    private final CurrentUser currentUser;
    private final SyncPushRequestParser parser;
    private final SyncPushService pushService;
    private final SyncPullService pullService;
    private final com.lifeos.sync.service.SyncSnapshotService snapshots;
    private final com.lifeos.sync.service.SyncDiagnosticsService diagnostics;

    SyncController(CurrentUser currentUser, SyncPushRequestParser parser, SyncPushService pushService,
            SyncPullService pullService, com.lifeos.sync.service.SyncSnapshotService snapshots,
            com.lifeos.sync.service.SyncDiagnosticsService diagnostics) {
        this.snapshots = snapshots;
        this.diagnostics = diagnostics;
        this.currentUser = currentUser;
        this.parser = parser;
        this.pushService = pushService;
        this.pullService = pullService;
    }

    /** 200 nếu request đúng cấu trúc; trạng thái từng mutation trong results. */
    @PostMapping("/push")
    PushResponse push(@RequestBody PushRequest body) {
        return pushService.push(currentUser.userId(), currentUser.require().deviceId(),
                parser.parse(body.mutations()));
    }

    /** GET /sync/pull?cursor=&limit= (SYSTEM_ARCHITECTURE §4.2, SYNC_PROTOCOL §6). */
    @GetMapping("/pull")
    PullResponse pull(@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer limit) {
        return pullService.pull(currentUser.userId(), cursor, limit);
    }

    /** POST /sync/snapshot (PHASE_5 D152): trạng thái hiện tại theo trang cho full resync. */
    @PostMapping("/snapshot")
    com.lifeos.sync.dto.SnapshotResponse snapshot(
            @RequestBody(required = false) com.lifeos.sync.dto.SnapshotRequest r) {
        com.lifeos.sync.dto.SnapshotRequest req = r == null
                ? new com.lifeos.sync.dto.SnapshotRequest(null, null, null) : r;
        return snapshots.page(currentUser.userId(), req.snapshotToken(), req.next(), req.limit());
    }

    /** GET /sync/entities/{type}/{id}: bản server hiện tại (kể cả tombstone) để client giải quyết xung đột. */
    @GetMapping("/entities/{type}/{id}")
    com.lifeos.sync.dto.ServerSnapshot entity(@org.springframework.web.bind.annotation.PathVariable String type,
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
        return snapshots.entity(currentUser.userId(), type, id);
    }

    /** POST /sync/diagnostics (D170): 202, không chứa dữ liệu trường. */
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Đã nhận báo cáo")
    @PostMapping("/diagnostics")
    org.springframework.http.ResponseEntity<Void> diagnostics(@RequestBody java.util.Map<String, Object> report) {
        diagnostics.submit(currentUser.userId(), report);
        return org.springframework.http.ResponseEntity.accepted().build();
    }
}
