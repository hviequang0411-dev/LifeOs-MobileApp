package com.lifeos.identity.service;

import java.util.Locale;

/** Chuẩn hóa email: trim + lowercase theo Locale.ROOT (DATABASE_DESIGN §4.1). */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
