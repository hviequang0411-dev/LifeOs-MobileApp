package com.lifeos.routine.service;

import com.lifeos.routine.api.RoutineApi;
import com.lifeos.routine.repository.RoutineRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link RoutineApi}. */
@Service
class RoutineApiImpl implements RoutineApi {

    private final RoutineRepository routines;

    RoutineApiImpl(RoutineRepository routines) {
        this.routines = routines;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockForShare(UUID userId, UUID routineId) {
        return routines.findActiveForShare(routineId, userId).isPresent();
    }
}
