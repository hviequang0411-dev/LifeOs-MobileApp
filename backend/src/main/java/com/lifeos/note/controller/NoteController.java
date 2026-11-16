package com.lifeos.note.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.util.Patch;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.note.dto.CreateNoteLinkRequest;
import com.lifeos.note.dto.CreateNoteRequest;
import com.lifeos.note.dto.CreateTagRequest;
import com.lifeos.note.dto.NoteDetail;
import com.lifeos.note.dto.NoteLinkResponse;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.dto.NoteSummary;
import com.lifeos.note.dto.ReplaceNoteTagsRequest;
import com.lifeos.note.dto.TagResponse;
import com.lifeos.note.dto.UpdateNoteRequest;
import com.lifeos.note.dto.UpdateTagRequest;
import com.lifeos.note.service.NoteCommandService;
import com.lifeos.note.service.NoteCommands;
import com.lifeos.note.service.NoteQueryService;
import com.lifeos.note.service.NoteRelationService;
import com.lifeos.note.service.TagCommandService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ghi chú, thẻ, liên kết của user hiện tại (PHASE_6 §6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1")
class NoteController {

    private final CurrentUser currentUser;
    private final NoteCommandService commands;
    private final TagCommandService tagCommands;
    private final NoteRelationService relations;
    private final NoteQueryService queries;

    NoteController(CurrentUser currentUser, NoteCommandService commands, TagCommandService tagCommands,
            NoteRelationService relations, NoteQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.tagCommands = tagCommands;
        this.relations = relations;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = NoteResponse.class)))
    @PostMapping("/notes")
    ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest r) {
        NoteResponse created = commands.create(currentUser.userId(), new NoteCommands.Create(r.id(),
                Patch.of(r.title()), Patch.of(r.content()), Patch.absent(), Patch.absent()), null);
        return ResponseEntity.created(URI.create("/api/v1/notes/" + created.id())).body(created);
    }

    /** Lọc tagId (lặp = AND; tagMode=any = OR), pinned, archived (false|true|all), q (tìm toàn văn ≤ 100 ký tự). */
    @GetMapping("/notes")
    PageResponse<NoteSummary> list(@RequestParam(required = false) String q,
            @RequestParam(name = "tagId", required = false) List<UUID> tagIds,
            @RequestParam(required = false) String tagMode, @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) String archived, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), q, tagIds, tagMode, pinned, archived, limit, cursor);
    }

    @GetMapping("/notes/{id}")
    NoteDetail get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/notes/{id}")
    NoteResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest r) {
        return commands.update(currentUser.userId(), id, new NoteCommands.Update(r.version(), r.title(), r.content(),
                Patch.absent(), Patch.absent()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (kèm thẻ gắn và liên kết của ghi chú)")
    @DeleteMapping("/notes/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notes/{id}/archive")
    NoteResponse archive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.archive(currentUser.userId(), id, r.version(), true);
    }

    @PostMapping("/notes/{id}/unarchive")
    NoteResponse unarchive(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.archive(currentUser.userId(), id, r.version(), false);
    }

    @PostMapping("/notes/{id}/pin")
    NoteResponse pin(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.pin(currentUser.userId(), id, r.version(), true);
    }

    @PostMapping("/notes/{id}/unpin")
    NoteResponse unpin(@PathVariable UUID id, @Valid @RequestBody VersionRequest r) {
        return commands.pin(currentUser.userId(), id, r.version(), false);
    }

    @PutMapping("/notes/{id}/tags")
    List<TagResponse> replaceTags(@PathVariable UUID id, @Valid @RequestBody ReplaceNoteTagsRequest r) {
        return relations.replaceTags(currentUser.userId(), id, r.tagIds());
    }

    @ApiResponse(responseCode = "201", description = "Đã liên kết",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NoteLinkResponse.class)))
    @ApiResponse(responseCode = "200", description = "Liên kết đã có (idempotent)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NoteLinkResponse.class)))
    @PostMapping("/notes/{id}/links")
    ResponseEntity<NoteLinkResponse> link(@PathVariable UUID id, @Valid @RequestBody CreateNoteLinkRequest r) {
        NoteRelationService.Created<NoteLinkResponse> created = relations.addLink(currentUser.userId(), r.id(), id,
                r.targetType(), r.targetId(), true, null);
        return ResponseEntity.status(created.created() ? HttpStatus.CREATED : HttpStatus.OK).body(created.value());
    }

    @ApiResponse(responseCode = "204", description = "Đã gỡ liên kết")
    @DeleteMapping("/notes/{id}/links/{linkId}")
    ResponseEntity<Void> unlink(@PathVariable UUID id, @PathVariable UUID linkId, @RequestParam long version) {
        relations.removeLink(currentUser.userId(), id, linkId, version, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/goals/{id}/notes")
    PageResponse<NoteSummary> goalNotes(@PathVariable UUID id, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.goalNotes(currentUser.userId(), id, limit, cursor);
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TagResponse.class)))
    @PostMapping("/tags")
    ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest r) {
        TagResponse created = tagCommands.create(currentUser.userId(), new NoteCommands.CreateTag(r.id(),
                Patch.of(r.name()), Patch.of(r.color())), null);
        return ResponseEntity.created(URI.create("/api/v1/tags/" + created.id())).body(created);
    }

    @GetMapping("/tags")
    List<TagResponse> tags() {
        return queries.tags(currentUser.userId());
    }

    @PatchMapping("/tags/{id}")
    TagResponse updateTag(@PathVariable UUID id, @Valid @RequestBody UpdateTagRequest r) {
        return tagCommands.update(currentUser.userId(), id, new NoteCommands.UpdateTag(r.version(), r.name(),
                r.color()), null);
    }

    @ApiResponse(responseCode = "204", description = "Đã xóa (gỡ khỏi mọi ghi chú)")
    @DeleteMapping("/tags/{id}")
    ResponseEntity<Void> deleteTag(@PathVariable UUID id, @RequestParam long version) {
        tagCommands.delete(currentUser.userId(), id, version, null);
        return ResponseEntity.noContent().build();
    }
}
