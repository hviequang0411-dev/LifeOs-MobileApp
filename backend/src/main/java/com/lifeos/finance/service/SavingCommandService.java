package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.dto.ContributionResponse;
import com.lifeos.finance.dto.GoalResponse;
import com.lifeos.finance.entity.SavingContribution;
import com.lifeos.finance.entity.SavingGoal;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.FinanceJdbc;
import com.lifeos.finance.repository.SavingContributionRepository;
import com.lifeos.finance.repository.SavingGoalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Mục tiêu tiết kiệm và khoản góp (FR-FIN-08, D111). Khoản góp độc lập với số dư tài khoản. Trạng thái do server tính:
 * mục tiêu chưa lưu trữ có Σ góp ≥ mục tiêu → ACHIEVED, ngược lại ACTIVE; tính lại khi thêm/sửa/xóa khoản góp hoặc đổi
 * số tiền mục tiêu. Client chỉ gửi ACTIVE (mở lại / bỏ lưu trữ) hoặc ARCHIVED; ACHIEVED gửi lên bị bỏ qua (FIN-20).
 * Mọi lệnh ghi khoản góp khóa mục tiêu FOR UPDATE trước (tuần tự hóa, không mồ côi khi mục tiêu bị xóa) và ghi
 * change_logs UPDATE của mục tiêu khi trạng thái đổi (mutation_id NULL).
 */
@Service
public class SavingCommandService {

    static final int NAME_MAX = 80;
    static final String ACTIVE = "ACTIVE";
    static final String ACHIEVED = "ACHIEVED";
    static final String ARCHIVED = "ARCHIVED";
    private static final Set<String> CLIENT_STATUSES = Set.of(ACTIVE, ARCHIVED, ACHIEVED);
    private static final String GOAL_PK = "pk_saving_goals";
    private static final String CONTRIBUTION_PK = "pk_saving_contributions";

    private final SavingGoalRepository goals;
    private final SavingContributionRepository contributions;
    private final FinanceJdbc jdbc;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final FinanceMapper mapper;
    private final Clock clock;

    public SavingCommandService(SavingGoalRepository goals, SavingContributionRepository contributions,
            FinanceJdbc jdbc, ChangeRecorder changeRecorder, VersionedWrites versionedWrites, UserCalendar calendar,
            FinanceMapper mapper, Clock clock) {
        this.goals = goals;
        this.contributions = contributions;
        this.jdbc = jdbc;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- mục tiêu

    public GoalResponse createGoal(UUID userId, FinanceCommands.CreateGoal cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (goals.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = FinanceValues.name(cmd.name().value(), NAME_MAX);
                String currency = FinanceValues.currency(cmd.currency().value(), "currency");
                BigDecimal target = FinanceValues.amount(cmd.targetAmount().value(), currency, "targetAmount");
                LocalDate targetOn = targetOn(cmd.targetOn().value());
                String requested = clientStatus(cmd.status().value());
                SavingGoal goal = new SavingGoal(cmd.id(), userId);
                goal.setName(name);
                goal.setCurrency(currency);
                goal.setTargetAmount(target);
                goal.setTargetOn(targetOn);
                goal.setStatus(ARCHIVED.equals(requested) ? ARCHIVED : ACTIVE);
                SavingGoal saved = goals.saveAndFlush(goal);
                changeRecorder.record(userId, SavingGoal.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (FinanceValues.mentions(e, GOAL_PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public GoalResponse updateGoal(UUID userId, UUID goalId, FinanceCommands.UpdateGoal cmd, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            SavingGoal goal = goals.findActiveForUpdate(goalId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_GOAL_NOT_FOUND));
            VersionedWrites.requireVersion(goal, cmd.expectedVersion());
            boolean changed = false;
            if (cmd.name().present()) {
                changed |= goal.setName(FinanceValues.name(cmd.name().value(), NAME_MAX));
            }
            String currency = cmd.currency().present()
                    ? FinanceValues.currency(cmd.currency().value(), "currency") : goal.getCurrency();
            BigDecimal target;
            if (cmd.targetAmount().present()) {
                target = FinanceValues.amount(cmd.targetAmount().value(), currency, "targetAmount");
            } else {
                target = goal.getTargetAmount();
                FinanceValues.requireFits(target, currency, "targetAmount");
            }
            if (cmd.targetOn().present()) {
                changed |= goal.setTargetOn(targetOn(cmd.targetOn().value()));
            }
            String requested = cmd.status().present() ? clientStatus(cmd.status().value()) : null;
            if (!currency.equals(goal.getCurrency())) {
                // Khoản góp không mang tiền tệ: đổi tiền tệ khi đã có khoản góp làm sai ý nghĩa [PROPOSED].
                if (!contributions.findLiveByGoal(userId, goalId).isEmpty()) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_CURRENCY_LOCKED);
                }
                changed |= goal.setCurrency(currency);
            }
            boolean targetChanged = goal.setTargetAmount(target);
            changed |= targetChanged;
            if (ARCHIVED.equals(requested)) {
                changed |= goal.setStatus(ARCHIVED);
            } else if (ACTIVE.equals(requested)) {
                // ACHIEVED → ACTIVE: người dùng mở lại; ARCHIVED → bỏ lưu trữ, trạng thái tính lại.
                String status = ARCHIVED.equals(goal.getStatus()) ? computed(userId, goal) : ACTIVE;
                changed |= goal.setStatus(status);
            } else if (targetChanged && !ARCHIVED.equals(goal.getStatus())) {
                changed |= goal.setStatus(computed(userId, goal));
            }
            if (!changed) {
                return mapper.toResponse(goal);
            }
            SavingGoal saved = goals.saveAndFlush(goal);
            changeRecorder.record(userId, SavingGoal.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> goalVersion(userId, goalId));
    }

    /** Xóa mục tiêu kèm mọi khoản góp còn sống (cascade, mutation_id NULL; record mục tiêu sau cùng — D86). */
    public long deleteGoal(UUID userId, UUID goalId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            SavingGoal goal = goals.findActiveForUpdate(goalId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_GOAL_NOT_FOUND));
            VersionedWrites.requireVersion(goal, expectedVersion);
            Instant now = clock.instant();
            goal.softDelete(now);
            SavingGoal saved = goals.saveAndFlush(goal);
            List<SavingContribution> children = contributions.findLiveByGoal(userId, goalId);
            children.forEach(c -> c.softDelete(now));
            List<SavingContribution> deleted = contributions.saveAllAndFlush(children);
            for (SavingContribution c : deleted) {
                changeRecorder.record(userId, SavingContribution.ENTITY_TYPE, c.getId(), ChangeOperation.DELETE,
                        c.getVersion(), null);
            }
            changeRecorder.record(userId, SavingGoal.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> goalVersion(userId, goalId));
    }

    // ---------------------------------------------------------------- khoản góp

    /**
     * @param goalFromUrl mục tiêu trong URL (REST); null với sync (goalId lấy từ payload — tham chiếu sai là 422/REJECTED
     *                    FINANCE_GOAL_NOT_FOUND)
     */
    public ContributionResponse createContribution(UUID userId, UUID goalFromUrl,
            FinanceCommands.CreateContribution cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (contributions.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID goalId = goalFromUrl != null ? goalFromUrl
                        : FinanceValues.requiredId(cmd.goalId().value(), "goalId");
                String rawAmount = cmd.amount().value();
                LocalDate occurredOn = FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn",
                        calendar.today(userId));
                String note = FinanceValues.note(cmd.note().value());
                SavingGoal goal = goals.findActiveForUpdate(goalId, userId).orElseThrow(() -> goalFromUrl != null
                        ? FinanceValues.notFound(ErrorCode.FINANCE_GOAL_NOT_FOUND)
                        : FinanceValues.badReference(ErrorCode.FINANCE_GOAL_NOT_FOUND));
                BigDecimal amount = FinanceValues.amount(rawAmount, goal.getCurrency(), "amount");
                SavingContribution c = SavingContribution.create(cmd.id(), userId, goalId);
                c.setAmount(amount);
                c.setOccurredOn(occurredOn);
                c.setNote(note);
                SavingContribution saved = contributions.saveAndFlush(c);
                SavingGoal goalAfter = settle(userId, goal);
                changeRecorder.record(userId, SavingContribution.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                recordGoal(userId, goalAfter);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (FinanceValues.mentions(e, CONTRIBUTION_PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /** UPDATE khoản góp qua sync: goalId bất biến. */
    public ContributionResponse updateContribution(UUID userId, UUID contributionId,
            FinanceCommands.UpdateContribution cmd, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            SavingContribution c = contributions.findActive(contributionId, userId)
                    .orElseThrow(FinanceValues::contributionNotFound);
            SavingGoal goal = goals.findActiveForUpdate(c.getGoalId(), userId)
                    .orElseThrow(FinanceValues::contributionNotFound);
            VersionedWrites.requireVersion(c, cmd.expectedVersion());
            FinanceValues.immutable(cmd.goalId(), c.getGoalId(), "goalId");
            boolean changed = false;
            if (cmd.amount().present()) {
                changed |= c.setAmount(FinanceValues.amount(cmd.amount().value(), goal.getCurrency(), "amount"));
            }
            if (cmd.occurredOn().present()) {
                changed |= c.setOccurredOn(FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn",
                        calendar.today(userId)));
            }
            if (cmd.note().present()) {
                changed |= c.setNote(FinanceValues.note(cmd.note().value()));
            }
            if (!changed) {
                return mapper.toResponse(c);
            }
            SavingContribution saved = contributions.saveAndFlush(c);
            SavingGoal goalAfter = settle(userId, goal);
            changeRecorder.record(userId, SavingContribution.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            recordGoal(userId, goalAfter);
            return mapper.toResponse(saved);
        }, () -> contributionVersion(userId, contributionId));
    }

    /** Xóa khoản góp (sửa sai, D111); mục tiêu có thể trở lại ACTIVE. @return version tombstone */
    public long deleteContribution(UUID userId, UUID contributionId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            SavingContribution c = contributions.findActive(contributionId, userId)
                    .orElseThrow(FinanceValues::contributionNotFound);
            SavingGoal goal = goals.findActiveForUpdate(c.getGoalId(), userId)
                    .orElseThrow(FinanceValues::contributionNotFound);
            VersionedWrites.requireVersion(c, expectedVersion);
            c.softDelete(clock.instant());
            SavingContribution saved = contributions.saveAndFlush(c);
            SavingGoal goalAfter = settle(userId, goal);
            changeRecorder.record(userId, SavingContribution.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            recordGoal(userId, goalAfter);
            return saved.getVersion();
        }, () -> contributionVersion(userId, contributionId));
    }

    /** Tính lại trạng thái sau khi khoản góp đổi; flush nếu đổi. @return mục tiêu đã lưu nếu đổi, ngược lại null */
    private SavingGoal settle(UUID userId, SavingGoal goal) {
        if (ARCHIVED.equals(goal.getStatus()) || !goal.setStatus(computed(userId, goal))) {
            return null;
        }
        return goals.saveAndFlush(goal);
    }

    private void recordGoal(UUID userId, SavingGoal savedOrNull) {
        if (savedOrNull != null) {
            changeRecorder.record(userId, SavingGoal.ENTITY_TYPE, savedOrNull.getId(), ChangeOperation.UPDATE,
                    savedOrNull.getVersion(), null);
        }
    }

    private String computed(UUID userId, SavingGoal goal) {
        BigDecimal saved = jdbc.contributionSums(userId, List.of(goal.getId()))
                .getOrDefault(goal.getId(), BigDecimal.ZERO);
        return saved.compareTo(goal.getTargetAmount()) >= 0 ? ACHIEVED : ACTIVE;
    }

    /** ACHIEVED từ client bị bỏ qua (null); giá trị khác ngoài ACTIVE/ARCHIVED → INVALID_VALUE. */
    private static String clientStatus(String raw) {
        if (raw == null) {
            return null;
        }
        if (!CLIENT_STATUSES.contains(raw)) {
            throw FinanceValues.invalid("status", "INVALID_VALUE", "status là ACTIVE hoặc ARCHIVED.");
        }
        return ACHIEVED.equals(raw) ? null : raw;
    }

    private static LocalDate targetOn(String raw) {
        LocalDate date = FinanceValues.date(raw, "targetOn");
        if (date != null && date.isBefore(FinanceValues.MIN_DATE)) {
            throw FinanceValues.invalid("targetOn", "OUT_OF_RANGE", "Ngày nằm ngoài khoảng cho phép.");
        }
        return date;
    }

    private long goalVersion(UUID userId, UUID goalId) {
        return goals.findVersion(goalId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_GOAL_NOT_FOUND));
    }

    private long contributionVersion(UUID userId, UUID contributionId) {
        return contributions.findVersion(contributionId, userId).orElseThrow(FinanceValues::contributionNotFound);
    }
}
