package com.lifeos.goal.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.goal.domain.GoalProgress;
import com.lifeos.goal.api.GoalAiQueryApi;
import com.lifeos.goal.domain.GoalStatus;
import com.lifeos.goal.dto.GoalDetail;
import com.lifeos.goal.dto.GoalSyncState;
import com.lifeos.goal.dto.MilestoneView;
import com.lifeos.goal.dto.ProgressResponse;
import com.lifeos.goal.entity.Goal;
import com.lifeos.goal.entity.Milestone;
import com.lifeos.goal.mapper.GoalMapper;
import com.lifeos.goal.repository.GoalRepository;
import com.lifeos.goal.repository.MilestoneRepository;
import com.lifeos.task.api.GoalTaskCount;
import com.lifeos.task.api.TaskApi;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Goal + tiến độ tính (D185) + milestone. Chỉ bản ghi chưa xóa của user hiện tại. */
@Service
public class GoalQueryService implements GoalAiQueryApi {

    static final String SORT = "createdAt:desc";

    private final GoalRepository goals;
    private final MilestoneRepository milestones;
    private final TaskApi taskApi;
    private final GoalMapper mapper;

    public GoalQueryService(GoalRepository goals, MilestoneRepository milestones, TaskApi taskApi,
            GoalMapper mapper) {
        this.goals = goals;
        this.milestones = milestones;
        this.taskApi = taskApi;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public GoalDetail get(UUID userId, UUID goalId) {
        Goal goal = goals.findActive(goalId, userId).orElseThrow(GoalCommandService::notFound);
        return details(userId, List.of(goal)).get(0);
    }

    @Transactional(readOnly = true)
    public PageResponse<GoalDetail> list(UUID userId, String status, Integer limit, String cursor) {
        if (status != null && !GoalStatus.ALL.contains(status)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "status", "INVALID_VALUE",
                    "Trạng thái không hợp lệ.");
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterCreatedAt = null;
        if (after != null) {
            try {
                afterCreatedAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                        "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<Goal> rows = goals.page(userId, status, after == null, afterCreatedAt,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Goal> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Goal last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(details(userId, page), next, hasMore);
    }

    private List<GoalDetail> details(UUID userId, List<Goal> page) {
        if (page.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = page.stream().map(Goal::getId).toList();
        Map<UUID, List<Milestone>> byGoal = milestones.findLiveByGoalsOrdered(userId, ids).stream()
                .collect(Collectors.groupingBy(Milestone::getGoalId));
        Map<UUID, Map<UUID, GoalTaskCount>> counts = new HashMap<>();
        for (GoalTaskCount c : taskApi.goalCounts(userId, ids)) {
            counts.computeIfAbsent(c.goalId(), k -> new HashMap<>()).put(c.milestoneId(), c);
        }
        List<GoalDetail> out = new ArrayList<>();
        for (Goal goal : page) {
            List<Milestone> ms = byGoal.getOrDefault(goal.getId(), List.of());
            Map<UUID, GoalTaskCount> perMilestone = counts.getOrDefault(goal.getId(), Map.of());
            long done = perMilestone.values().stream().mapToLong(GoalTaskCount::done).sum();
            long total = perMilestone.values().stream().mapToLong(GoalTaskCount::total).sum();
            List<GoalProgress.MilestoneInput> inputs = new ArrayList<>();
            List<MilestoneView> views = new ArrayList<>();
            int completed = 0;
            for (Milestone m : ms) {
                GoalTaskCount c = perMilestone.get(m.getId());
                long md = c == null ? 0 : c.done();
                long mt = c == null ? 0 : c.total();
                boolean isDone = m.getCompletedAt() != null;
                completed += isDone ? 1 : 0;
                inputs.add(new GoalProgress.MilestoneInput(isDone, md, mt));
                BigDecimal mp = isDone ? GoalProgress.ratio(BigInteger.ONE, BigInteger.ONE)
                        : GoalProgress.ratio(BigInteger.valueOf(mt == 0 ? 0 : md), BigInteger.valueOf(mt == 0 ? 1 : mt));
                views.add(new MilestoneView(mapper.toResponse(m), md, mt, mp));
            }
            GoalProgress.Result result = GoalProgress.compute(inputs, done, total);
            ProgressResponse progress = new ProgressResponse(result.method(), result.value(), completed, ms.size(),
                    done, total, !ms.isEmpty() && completed == ms.size());
            out.add(new GoalDetail(mapper.toResponse(goal), progress, views));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<GoalSyncState> goalStates(UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return goals.findAllOwned(userId, ids).stream()
                .map(g -> new GoalSyncState(g.getId(), g.getVersion(), g.isDeleted(), mapper.toResponse(g)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GoalSyncState> milestoneStates(UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return milestones.findAllOwned(userId, ids).stream()
                .map(m -> new GoalSyncState(m.getId(), m.getVersion(), m.isDeleted(), mapper.toResponse(m)))
                .toList();
    }

    /** PHASE_7 D197: tối đa {@code limit} Goal còn sống kèm tiến độ (cho GoalStatsApi). */
    @Transactional(readOnly = true)
    public List<GoalDetail> allDetails(UUID userId, int limit) {
        return details(userId, goals.page(userId, null, true, null, null, Limit.of(limit)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalAiQueryApi.Snapshot> aiGoals(UUID userId, int limit) {
        return allDetails(userId, limit).stream().map(d -> new GoalAiQueryApi.Snapshot(d.goal().id(),
                d.goal().title(), d.goal().description(), d.goal().targetOn(), d.goal().status(),
                d.progress().value() == null ? null : d.progress().value().toPlainString(), d.goal().version()))
                .toList();
    }
}
