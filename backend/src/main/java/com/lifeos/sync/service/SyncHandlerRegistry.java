package com.lifeos.sync.service;

import com.lifeos.common.sync.SyncEntityHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Danh sách trắng entityType (SECURITY_RULES §8.2): mọi {@link SyncEntityHandler} trong context, theo entityType. Hai
 * handler cùng entityType làm context không khởi động được. Dùng chung cho push và pull.
 */
@Component
public class SyncHandlerRegistry {

    private final Map<String, SyncEntityHandler> handlers;

    public SyncHandlerRegistry(List<SyncEntityHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(SyncEntityHandler::entityType,
                Function.identity()));
    }

    public Optional<SyncEntityHandler> find(String entityType) {
        return Optional.ofNullable(handlers.get(entityType));
    }

    /** Mọi handler, sắp theo entityType (thứ tự ổn định cho snapshot). */
    public List<SyncEntityHandler> all() {
        return handlers.values().stream().sorted(java.util.Comparator.comparing(SyncEntityHandler::entityType))
                .toList();
    }
}
