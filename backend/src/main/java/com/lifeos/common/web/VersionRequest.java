package com.lifeos.common.web;

import jakarta.validation.constraints.NotNull;

/** Thân của hành động nghiệp vụ POST /x/{id}/{action}: { "version": n } bắt buộc (D58). */
public record VersionRequest(@NotNull Long version) {
}
