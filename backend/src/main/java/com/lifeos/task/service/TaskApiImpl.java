package com.lifeos.task.service;

import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskSchedule;
import com.lifeos.task.entity.Task;
import com.lifeos.task.repository.TaskRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link TaskApi}. */
@Service
class TaskApiImpl implements TaskApi {

    private final TaskRepository tasks;

    TaskApiImpl(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void detachFromGoal(UUID userId, UUID goalId, com.lifeos.common.sync.PendingChanges changes) {
        detach(tasks.findLiveByGoal(userId, goalId), t -> t.link(null, null), changes);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void detachFromMilestone(UUID userId, UUID milestoneId, com.lifeos.common.sync.PendingChanges changes) {
        detach(tasks.findLiveByMilestone(userId, milestoneId), t -> t.link(t.getGoalId(), null), changes);
    }

    private void detach(java.util.List<Task> affected, java.util.function.Predicate<Task> change,
            com.lifeos.common.sync.PendingChanges changes) {
        java.util.List<Task> changed = affected.stream().filter(change).toList();
        if (changed.isEmpty()) {
            return;
        }
        for (Task t : tasks.saveAllAndFlush(changed)) {
            changes.add(Task.ENTITY_TYPE, t.getId(), com.lifeos.common.sync.ChangeOperation.UPDATE, t.getVersion());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.lifeos.task.api.GoalTaskCount> goalCounts(UUID userId,
            java.util.Collection<UUID> goalIds) {
        if (goalIds.isEmpty()) {
            return java.util.List.of();
        }
        return tasks.goalCounts(userId, goalIds).stream()
                .map(r -> new com.lifeos.task.api.GoalTaskCount((UUID) r[0], (UUID) r[1], ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskSchedule> schedule(UUID userId, UUID taskId) {
        return tasks.findActive(taskId, userId).map(TaskApiImpl::toSchedule);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<TaskSchedule> lockForShare(UUID userId, UUID taskId) {
        return tasks.findActiveForShare(taskId, userId).map(TaskApiImpl::toSchedule);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.lifeos.task.api.TaskDue> dueBetween(UUID userId, java.time.LocalDate from,
            java.time.LocalDate to, int limit) {
        return tasks.dueBetween(userId, from, to, org.springframework.data.domain.Limit.of(limit)).stream()
                .map(t -> new com.lifeos.task.api.TaskDue(t.getId(), t.getTitle(), t.getDueDate(), t.getDueTime(),
                        t.getStatus().name()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> title(UUID userId, UUID taskId) {
        return tasks.findActive(taskId, userId).map(Task::getTitle);
    }

    private static TaskSchedule toSchedule(Task task) {
        return new TaskSchedule(task.getId(), task.getDueDate(), task.getDueTime());
    }
}
