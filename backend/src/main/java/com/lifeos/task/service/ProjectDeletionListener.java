package com.lifeos.task.service;

import com.lifeos.project.api.ProjectDeletionStarted;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhận sự kiện xóa Project (D80): chạy ĐỒNG BỘ trong transaction xóa của module project (MANDATORY), gỡ project_id
 * khỏi mọi Task còn sống (Project đã được đánh dấu xóa và flush, D86) — change_logs của Task đứng trước change_logs xóa Project
 * (SYSTEM_ARCHITECTURE §3.8). Lỗi ở đây làm rollback cả việc xóa Project.
 */
@Service
public class ProjectDeletionListener {

    private final TaskCommandService taskCommands;

    public ProjectDeletionListener(TaskCommandService taskCommands) {
        this.taskCommands = taskCommands;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onProjectDeletionStarted(ProjectDeletionStarted event) {
        taskCommands.detachFromProject(event.userId(), event.projectId());
    }
}
