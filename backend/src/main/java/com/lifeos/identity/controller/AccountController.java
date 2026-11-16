package com.lifeos.identity.controller;

import com.lifeos.common.security.CurrentUser;
import com.lifeos.identity.dto.AccountDeletionRequest;
import com.lifeos.identity.dto.AccountDeletionResponse;
import com.lifeos.identity.dto.ExportResponse;
import com.lifeos.identity.service.AccountDeletionService;
import com.lifeos.identity.service.DataExportService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Xuất dữ liệu và xóa tài khoản của user hiện tại (PHASE_7 §6, D204, D205). */
@RestController
@RequestMapping("/api/v1/account")
class AccountController {

    private final CurrentUser currentUser;
    private final DataExportService exports;
    private final AccountDeletionService deletion;

    AccountController(CurrentUser currentUser, DataExportService exports, AccountDeletionService deletion) {
        this.currentUser = currentUser;
        this.exports = exports;
        this.deletion = deletion;
    }

    @ApiResponse(responseCode = "202", description = "Đã nhận yêu cầu",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportResponse.class)))
    @ApiResponse(responseCode = "200", description = "Đang có yêu cầu chờ/chạy (trả yêu cầu đó)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportResponse.class)))
    @PostMapping("/export")
    ResponseEntity<ExportResponse> export() {
        DataExportService.Requested r = exports.request(currentUser.userId());
        return ResponseEntity.status(r.created() ? HttpStatus.ACCEPTED : HttpStatus.OK).body(r.export());
    }

    @GetMapping("/export/{id}")
    ExportResponse exportStatus(@PathVariable UUID id) {
        return exports.get(currentUser.userId(), id);
    }

    @ApiResponse(responseCode = "200", description = "Tệp zip", content = @Content(mediaType = "application/zip"))
    @GetMapping("/export/{id}/download")
    ResponseEntity<Resource> download(@PathVariable UUID id) {
        Path file = exports.download(currentUser.userId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lifeos-export.zip\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new FileSystemResource(file));
    }

    @ApiResponse(responseCode = "202", description = "Đã gửi mã xác nhận qua email")
    @PostMapping("/deletion/code")
    ResponseEntity<Void> deletionCode() {
        deletion.requestCode(currentUser.userId());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/deletion")
    AccountDeletionResponse schedule(@Valid @RequestBody AccountDeletionRequest r) {
        return deletion.schedule(currentUser.userId(), r.password(), r.code());
    }

    @GetMapping("/deletion")
    AccountDeletionResponse deletionStatus() {
        return deletion.status(currentUser.userId());
    }

    @ApiResponse(responseCode = "204", description = "Đã hủy yêu cầu xóa")
    @DeleteMapping("/deletion")
    ResponseEntity<Void> cancel() {
        deletion.cancel(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
