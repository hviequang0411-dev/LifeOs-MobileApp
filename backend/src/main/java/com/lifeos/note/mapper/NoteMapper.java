package com.lifeos.note.mapper;

import com.lifeos.note.dto.NoteLinkResponse;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.dto.NoteTagResponse;
import com.lifeos.note.dto.TagResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.entity.NoteLink;
import com.lifeos.note.entity.NoteTag;
import com.lifeos.note.entity.Tag;
import org.mapstruct.Mapper;

/** Mapper entity → DTO của module note (D5). */
@Mapper
public interface NoteMapper {

    NoteResponse toResponse(Note note);

    TagResponse toResponse(Tag tag);

    NoteTagResponse toResponse(NoteTag noteTag);

    NoteLinkResponse toResponse(NoteLink link);
}
