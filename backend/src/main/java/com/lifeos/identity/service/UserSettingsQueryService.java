package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.identity.dto.SettingsResponse;
import com.lifeos.identity.mapper.IdentityMapper;
import com.lifeos.identity.repository.UserSettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc user_settings của user hiện tại (Q1: tách query khỏi command). */
@Service
public class UserSettingsQueryService {

    private final UserSettingsRepository repository;
    private final IdentityMapper mapper;

    public UserSettingsQueryService(UserSettingsRepository repository, IdentityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get(UUID userId) {
        return repository.findByUserId(userId)
                .map(mapper::toSettings)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
    }
}
