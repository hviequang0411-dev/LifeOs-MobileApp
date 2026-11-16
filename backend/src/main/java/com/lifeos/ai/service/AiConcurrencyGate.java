package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.RateLimitedException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class AiConcurrencyGate {
    private final Semaphore global;
    private final int perUser;
    private final ConcurrentHashMap<UUID, Semaphore> users = new ConcurrentHashMap<>();

    public AiConcurrencyGate(AiProperties properties) {
        global = new Semaphore(properties.getMaxConcurrentGlobal(), true);
        perUser = properties.getMaxConcurrentPerUser();
    }

    public <T> T execute(UUID userId, Supplier<T> call) {
        Semaphore user = users.computeIfAbsent(userId, ignored -> new Semaphore(perUser, true));
        if (!global.tryAcquire()) throw limited();
        if (!user.tryAcquire()) { global.release(); throw limited(); }
        try { return call.get(); } finally { user.release(); global.release(); }
    }

    private static BusinessException limited() {
        return new RateLimitedException(1).with("errorCode", "AI_RATE_LIMITED");
    }
}
