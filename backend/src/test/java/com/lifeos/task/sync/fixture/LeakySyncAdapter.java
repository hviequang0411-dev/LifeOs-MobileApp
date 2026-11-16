package com.lifeos.task.sync.fixture;

import com.lifeos.project.service.ProjectQueryService;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.entity.Task;
import com.lifeos.task.repository.TaskRepository;
import com.lifeos.task.service.TaskQueryService;

/**
 * Class VI PHẠM A13 có chủ đích, chỉ dùng cho ArchitectureRulesSelfTest: adapter sync dùng repository, entity và
 * service của module khác. Hợp lệ: service/dto cùng module. Không có annotation Spring nên không được nạp; nằm trong
 * mã test nên ArchitectureTest (DoNotIncludeTests) không thấy.
 */
public class LeakySyncAdapter {

    TaskRepository repository;
    Task entity;
    ProjectQueryService otherModuleService;
    TaskQueryService ownService;
    TaskResponse ownDto;
}
