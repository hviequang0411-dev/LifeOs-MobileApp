package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /auth/verify-email. */
public record VerifyEmailRequest(@NotBlank @Size(max = 128) String token) implements StrictJson {

    @Override
    public String toString() {
        return "VerifyEmailRequest[***]";
    }
}
