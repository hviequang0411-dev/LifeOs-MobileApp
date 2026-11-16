package com.lifeos.ai.service;

import com.lifeos.ai.repository.AiStore;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiDataService {
    private final AiStore store;

    public AiDataService(AiStore store) {
        this.store = store;
    }

    @Transactional
    public long deleteAll(UUID userId) {
        return store.deleteAll(userId);
    }
}
