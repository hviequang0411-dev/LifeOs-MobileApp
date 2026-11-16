package com.lifeos.goal.domain;

import java.util.Map;
import java.util.Set;

/** Vòng đời Goal D188: ACTIVE → {PAUSED, ACHIEVED, ABANDONED}; PAUSED/ACHIEVED/ABANDONED → ACTIVE. */
public final class GoalStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String ACHIEVED = "ACHIEVED";
    public static final String ABANDONED = "ABANDONED";
    public static final Set<String> ALL = Set.of(ACTIVE, PAUSED, ACHIEVED, ABANDONED);

    private static final Map<String, Set<String>> NEXT = Map.of(
            ACTIVE, Set.of(PAUSED, ACHIEVED, ABANDONED),
            PAUSED, Set.of(ACTIVE),
            ACHIEVED, Set.of(ACTIVE),
            ABANDONED, Set.of(ACTIVE));

    private GoalStatus() {
    }

    public static boolean allowed(String from, String to) {
        return from.equals(to) || NEXT.getOrDefault(from, Set.of()).contains(to);
    }
}
