package com.lifeos.goal.service;

import com.lifeos.goal.api.GoalStatsApi;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link GoalStatsApi} bằng GoalQueryService (tiến độ D185). */
@Service
class GoalStatsApiImpl implements GoalStatsApi {

    private final GoalQueryService queries;

    GoalStatsApiImpl(GoalQueryService queries) {
        this.queries = queries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalSnapshot> goals(UUID userId) {
        return queries.allDetails(userId, 500).stream().map(d -> new GoalSnapshot(d.goal().id(), d.goal().title(),
                d.goal().status(), d.goal().startOn(), d.goal().targetOn(), d.progress().value())).toList();
    }
}
