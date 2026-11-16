package com.lifeos.project.service;

import com.lifeos.project.api.ProjectApi;
import com.lifeos.project.api.ProjectRef;
import com.lifeos.project.entity.Project;
import com.lifeos.project.repository.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt ProjectApi (D80). */
@Service
public class ProjectApiImpl implements ProjectApi {

    private final ProjectRepository projects;

    public ProjectApiImpl(ProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProjectRef> findActiveOwned(UUID userId, UUID projectId) {
        return projects.findActive(projectId, userId).map(ProjectApiImpl::ref);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ProjectRef> lockActiveOwned(UUID userId, UUID projectId) {
        return projects.findActiveForShare(projectId, userId).map(ProjectApiImpl::ref);
    }

    private static ProjectRef ref(Project project) {
        return new ProjectRef(project.getId(), project.isArchived());
    }
}
