package com.lifeos.project.api;

import java.util.UUID;

/** Những gì module khác được biết về một Project (JAVA_SPRING_RULES §5: cross-module view). */
public record ProjectRef(UUID id, boolean archived) {
}
