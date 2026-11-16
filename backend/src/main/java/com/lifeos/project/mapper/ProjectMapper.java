package com.lifeos.project.mapper;

import com.lifeos.project.dto.ProjectResponse;
import com.lifeos.project.entity.Project;
import org.mapstruct.Mapper;

/** Mapper entity → DTO của module project (D5: MapStruct, unmappedTargetPolicy = ERROR). */
@Mapper
public interface ProjectMapper {

    ProjectResponse toResponse(Project project);
}
