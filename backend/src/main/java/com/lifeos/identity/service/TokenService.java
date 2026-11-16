package com.lifeos.identity.service;

import com.lifeos.common.security.JwtService;
import com.lifeos.common.security.SecurityEvent;
import com.lifeos.common.security.SecurityEventLogger;
import com.lifeos.identity.entity.Device;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.repository.DeviceRepository;
import com.lifeos.identity.repository.RefreshTokenRepository;
import com.lifeos.identity.repository.RefreshTokenRepository.ConsumedToken;
import com.lifeos.identity.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiên đăng nhập và refresh token (SECURITY_RULES §4.2–§4.5; D48, D71, D72, D75).
 * <p>
 * Các method trả kết quả dạng giá trị thay vì ném exception khi token không hợp lệ: khi phát hiện dùng lại, việc thu
 * hồi cả family phải được COMMIT dù request cuối cùng trả 401 (ném exception trong transaction sẽ rollback nó).
 * AuthService chuyển kết quả rỗng thành 401 sau khi transaction đã kết thúc.
 */
@Service
public class TokenService {

    static final Duration REFRESH_TTL = Duration.ofDays(30);
    static final Duration FAMILY_MAX_LIFETIME = Duration.ofDays(90);

    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final DeviceRepository devices;
    private final DeviceService deviceService;
    private final JwtService jwtService;
    private final SecurityEventLogger securityEvents;
    private final Clock clock;

    public TokenService(RefreshTokenRepository refreshTokens, UserRepository users, DeviceRepository devices,
            DeviceService deviceService, JwtService jwtService, SecurityEventLogger securityEvents, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.devices = devices;
        this.deviceService = deviceService;
        this.jwtService = jwtService;
        this.securityEvents = securityEvents;
        this.clock = clock;
    }

    /**
     * Mở phiên sau khi đã xác thực mật khẩu (UC-ID-03): chọn/tạo thiết bị, thu hồi family cũ của thiết bị (mỗi thiết bị
     * một phiên, D75), cấp family mới. rehashedPassword khác null → lưu hash mới (tham số Argon2 đã đổi, D46).
     */
    @Transactional
    public TokenPair openSession(UUID userId, UUID requestedDeviceId, DeviceInfo deviceInfo,
            String rehashedPassword) {
        Instant now = clock.instant();
        if (rehashedPassword != null) {
            User user = users.findById(userId).orElseThrow();
            user.changePasswordHash(rehashedPassword);
        }
        Device device = deviceService.resolveForLogin(userId, requestedDeviceId, deviceInfo);
        refreshTokens.revokeByDevice(userId, device.getId(), now);

        OpaqueTokens.OpaqueToken refresh = OpaqueTokens.generate();
        Instant expiresAt = now.plus(REFRESH_TTL);
        refreshTokens.insert(UUID.randomUUID(), userId, device.getId(), UUID.randomUUID(), refresh.hash(), now, now,
                expiresAt);
        return pair(userId, device.getId(), refresh.raw());
    }

    /**
     * Xoay vòng (UC-ID-04). Token còn hiệu lực → đánh dấu đã dùng bằng một câu UPDATE có điều kiện và cấp token mới
     * cùng family trong cùng transaction (D72); hạn mới = min(now + 30 ngày, đầu family + 90 ngày) (D71).
     * Token đã dùng bị trình lại (kể cả request thua khi hai refresh đồng thời) → thu hồi cả family (D48).
     *
     * @return rỗng nếu token không dùng được (sai, hết hạn, đã dùng, đã thu hồi, thiết bị bị thu hồi, user không ACTIVE)
     */
    @Transactional
    public Optional<TokenPair> rotate(String rawRefreshToken, String clientIp) {
        Instant now = clock.instant();
        String hash = OpaqueTokens.sha256Hex(rawRefreshToken);
        UUID newTokenId = UUID.randomUUID();

        Optional<ConsumedToken> consumed = refreshTokens.consume(hash, newTokenId, now);
        if (consumed.isEmpty()) {
            refreshTokens.findByHash(hash)
                    .filter(state -> state.usedAt() != null)
                    .ifPresent(state -> {
                        refreshTokens.revokeFamily(state.familyId(), now);
                        securityEvents.log(SecurityEvent.REFRESH_TOKEN_REUSED, state.userId(), state.deviceId(),
                                clientIp);
                    });
            return Optional.empty();
        }

        ConsumedToken old = consumed.get();
        Instant familyCap = old.familyStartedAt().plus(FAMILY_MAX_LIFETIME);
        Instant expiresAt = min(now.plus(REFRESH_TTL), familyCap);
        OpaqueTokens.OpaqueToken refresh = OpaqueTokens.generate();
        refreshTokens.insert(newTokenId, old.userId(), old.deviceId(), old.familyId(), refresh.hash(),
                old.familyStartedAt(), now, expiresAt);
        devices.touch(old.deviceId(), old.userId(), now);
        return Optional.of(pair(old.userId(), old.deviceId(), refresh.raw()));
    }

    /**
     * Đăng xuất (UC-ID-05, D75): thu hồi cả family nếu token gửi lên CÒN HIỆU LỰC. Token đã dùng, hết hạn, đã thu hồi
     * hoặc không tồn tại: không làm gì (và không kích hoạt phát hiện dùng lại). Không thu hồi bản ghi thiết bị.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        Instant now = clock.instant();
        refreshTokens.findByHash(OpaqueTokens.sha256Hex(rawRefreshToken))
                .filter(s -> s.usedAt() == null && s.revokedAt() == null && s.expiresAt().isAfter(now))
                .ifPresent(s -> {
                    refreshTokens.revokeFamily(s.familyId(), now);
                    // D139: đăng xuất gỡ token push của thiết bị (bản ghi thiết bị giữ nguyên).
                    deviceService.signedOut(s.userId(), s.deviceId());
                });
    }

    private TokenPair pair(UUID userId, UUID deviceId, String rawRefresh) {
        JwtService.IssuedAccessToken access = jwtService.issue(userId, deviceId);
        return new TokenPair(access.token(), access.expiresIn(), rawRefresh, deviceId);
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
