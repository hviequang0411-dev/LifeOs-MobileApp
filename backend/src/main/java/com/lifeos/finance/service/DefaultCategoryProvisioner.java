package com.lifeos.finance.service;

import com.lifeos.common.provisioning.UserProvisioner;
import com.lifeos.finance.domain.DefaultCategories;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.repository.FinancialCategoryRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Danh mục hệ thống mặc định (PHASE_3 §5.1, D113): tạo lúc đăng ký và bù cho người dùng đăng ký trước Phase 3.
 * Idempotent theo (kind, systemKey) — danh mục hệ thống còn sống (kể cả đã đổi tên/lưu trữ) không bị tạo lại; nếu người
 * dùng đã có danh mục trùng tên cùng loại thì bỏ qua mục đó [PROPOSED]. Không gọi ChangeRecorder (xem
 * {@link UserProvisioner}); chạy trong transaction của người gọi.
 */
@Component
class DefaultCategoryProvisioner implements UserProvisioner {

    private final FinancialCategoryRepository categories;

    DefaultCategoryProvisioner(FinancialCategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    public List<PendingChange> provision(UUID userId) {
        Set<String> existing = new HashSet<>();
        for (FinancialCategory c : categories.findSystem(userId)) {
            existing.add(c.getKind() + ":" + c.getSystemKey());
        }
        List<FinancialCategory> created = new ArrayList<>();
        for (DefaultCategories.Entry entry : DefaultCategories.ALL) {
            if (existing.contains(entry.kind() + ":" + entry.systemKey())
                    || categories.nameTaken(userId, entry.kind(), entry.name(), new UUID(0, 0))) {
                continue;
            }
            created.add(FinancialCategory.create(UUID.randomUUID(), userId, entry.name(), entry.kind(),
                    entry.systemKey()));
        }
        if (created.isEmpty()) {
            return List.of();
        }
        List<FinancialCategory> saved = categories.saveAllAndFlush(created);
        return saved.stream()
                .map(c -> new PendingChange(FinancialCategory.ENTITY_TYPE, c.getId(), c.getVersion()))
                .toList();
    }
}
