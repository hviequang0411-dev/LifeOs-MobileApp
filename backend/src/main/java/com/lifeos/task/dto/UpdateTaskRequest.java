package com.lifeos.task.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * PATCH /tasks/{id} { version, …trường đổi } theo JSON Merge Patch (D62, D78): setter ghi nhận trường có mặt để phân
 * biệt "vắng" với "null". version bắt buộc (D58). Trường lạ (completedAt, userId…) bị bỏ qua.
 */
public class UpdateTaskRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<String> status = Patch.absent();
    private Patch<String> priority = Patch.absent();
    private Patch<String> dueDate = Patch.absent();
    private Patch<String> dueTime = Patch.absent();
    private Patch<UUID> projectId = Patch.absent();
    private Patch<UUID> categoryId = Patch.absent();
    private Patch<UUID> goalId = Patch.absent();
    private Patch<UUID> milestoneId = Patch.absent();

    public UpdateTaskRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = Patch.of(description);
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = Patch.of(status);
    }

    @JsonSetter("priority")
    public void setPriority(String priority) {
        this.priority = Patch.of(priority);
    }

    @JsonSetter("dueDate")
    public void setDueDate(String dueDate) {
        this.dueDate = Patch.of(dueDate);
    }

    @JsonSetter("dueTime")
    public void setDueTime(String dueTime) {
        this.dueTime = Patch.of(dueTime);
    }

    @JsonSetter("projectId")
    public void setProjectId(UUID projectId) {
        this.projectId = Patch.of(projectId);
    }

    @JsonSetter("categoryId")
    public void setCategoryId(UUID categoryId) {
        this.categoryId = Patch.of(categoryId);
    }

    public Long version() {
        return version;
    }

    public Patch<String> title() {
        return title;
    }

    public Patch<String> description() {
        return description;
    }

    public Patch<String> status() {
        return status;
    }

    public Patch<String> priority() {
        return priority;
    }

    public Patch<String> dueDate() {
        return dueDate;
    }

    public Patch<String> dueTime() {
        return dueTime;
    }

    public Patch<UUID> projectId() {
        return projectId;
    }

    public Patch<UUID> categoryId() {
        return categoryId;
    }

    @Override
    public String toString() {
        return "UpdateTaskRequest[version=" + version + "]";
    }

    @JsonSetter("goalId")
    public void setGoalId(UUID goalId) {
        this.goalId = Patch.of(goalId);
    }

    @JsonSetter("milestoneId")
    public void setMilestoneId(UUID milestoneId) {
        this.milestoneId = Patch.of(milestoneId);
    }

    public Patch<UUID> goalId() {
        return goalId;
    }

    public Patch<UUID> milestoneId() {
        return milestoneId;
    }
}
