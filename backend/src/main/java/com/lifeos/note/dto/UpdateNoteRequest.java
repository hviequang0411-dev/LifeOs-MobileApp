package com.lifeos.note.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /notes/{id} theo JSON Merge Patch; ghim/lưu trữ qua hành động riêng. toString không in nội dung. */
public class UpdateNoteRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> content = Patch.absent();

    public UpdateNoteRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    @JsonSetter("content")
    public void setContent(String content) {
        this.content = Patch.of(content);
    }

    public Long version() {
        return version;
    }

    public Patch<String> title() {
        return title;
    }

    public Patch<String> content() {
        return content;
    }

    @Override
    public String toString() {
        return "UpdateNoteRequest[version=" + version + "]";
    }
}
