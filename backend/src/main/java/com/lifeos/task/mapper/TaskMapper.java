package com.lifeos.task.mapper;

import com.lifeos.task.dto.CategoryResponse;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.entity.Category;
import com.lifeos.task.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entity → DTO của module task (D5: MapStruct, unmappedTargetPolicy = ERROR; một mapper cho mỗi module,
 * JAVA_SPRING_RULES §5.4).
 */
@Mapper
public interface TaskMapper {

    CategoryResponse toResponse(Category category);

    /** dueTime ra "HH:mm" (API_CONVENTIONS §2). */
    @Mapping(target = "dueTime", source = "dueTime", dateFormat = "HH:mm")
    TaskResponse toResponse(Task task);
}
