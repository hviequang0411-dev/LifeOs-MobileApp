package com.lifeos.identity.dto;

import java.time.Instant;
import java.util.UUID;

/** GET/PATCH /me (API_CONVENTIONS §8.2). */
public record ProfileResponse(UUID id, String email, String displayName, boolean emailVerified, Instant createdAt) {
}
