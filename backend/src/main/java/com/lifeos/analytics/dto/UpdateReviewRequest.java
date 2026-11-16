package com.lifeos.analytics.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /reviews/{id} theo JSON Merge Patch; chỉ reflection, mood đổi được. */
public class UpdateReviewRequest {

    @NotNull
    private Long version;
    private Patch<String> reflection = Patch.absent();
    private Patch<Integer> mood = Patch.absent();

    public UpdateReviewRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("reflection")
    public void setReflection(String reflection) {
        this.reflection = Patch.of(reflection);
    }

    @JsonSetter("mood")
    public void setMood(Integer mood) {
        this.mood = Patch.of(mood);
    }

    public Long version() {
        return version;
    }

    public Patch<String> reflection() {
        return reflection;
    }

    public Patch<Integer> mood() {
        return mood;
    }

    @Override
    public String toString() {
        return "UpdateReviewRequest[version=" + version + "]";
    }
}
