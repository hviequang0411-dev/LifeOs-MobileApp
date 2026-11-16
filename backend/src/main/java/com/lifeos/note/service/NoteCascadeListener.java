package com.lifeos.note.service;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.PendingChanges;
import com.lifeos.goal.api.GoalDeletionStarted;
import com.lifeos.note.entity.NoteLink;
import com.lifeos.note.repository.NoteLinkRepository;
import com.lifeos.task.api.TaskDeletionStarted;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cascade D189: xóa Task/Goal → xóa mềm note_links tới mục tiêu trong CÙNG transaction (MANDATORY), chỉ ghi nghiệp vụ
 * + flush và thêm change_logs vào PendingChanges của module phát sự kiện (D86, mutation_id NULL).
 */
@Service
public class NoteCascadeListener {

    private final NoteLinkRepository links;
    private final Clock clock;

    public NoteCascadeListener(NoteLinkRepository links, Clock clock) {
        this.links = links;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTaskDeletion(TaskDeletionStarted event) {
        detach(event.userId(), "TASK", event.taskId(), event.changes());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onGoalDeletion(GoalDeletionStarted event) {
        detach(event.userId(), "GOAL", event.goalId(), event.changes());
    }

    private void detach(UUID userId, String targetType, UUID targetId, PendingChanges changes) {
        List<NoteLink> affected = links.findLiveByTarget(userId, targetType, targetId);
        if (affected.isEmpty()) {
            return;
        }
        affected.forEach(l -> l.softDelete(clock.instant()));
        for (NoteLink l : links.saveAllAndFlush(affected)) {
            changes.add(NoteLink.ENTITY_TYPE, l.getId(), ChangeOperation.DELETE, l.getVersion());
        }
    }
}
