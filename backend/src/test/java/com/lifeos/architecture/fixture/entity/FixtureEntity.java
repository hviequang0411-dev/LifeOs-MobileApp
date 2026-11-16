package com.lifeos.architecture.fixture.entity;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.List;

/**
 * Class VI PHẠM có chủ đích, chỉ dùng cho ArchitectureRulesSelfTest (A11). Không có @Entity nên Hibernate không nạp;
 * nằm trong mã test nên ArchitectureTest không thấy.
 */
public class FixtureEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    FixtureEntity owner;

    @OneToMany
    List<FixtureEntity> children;

    String plainColumn;
}
