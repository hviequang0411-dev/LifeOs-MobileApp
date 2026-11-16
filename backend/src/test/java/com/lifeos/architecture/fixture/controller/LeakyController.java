package com.lifeos.architecture.fixture.controller;

import com.lifeos.architecture.fixture.entity.FixtureEntity;

/**
 * Class VI PHẠM có chủ đích, chỉ dùng cho ArchitectureRulesSelfTest (A8): method package-private trả về entity. Không
 * có annotation Spring nên không được nạp; nằm trong mã test nên ArchitectureTest không thấy.
 */
public class LeakyController {

    FixtureEntity leak() {
        return new FixtureEntity();
    }
}
