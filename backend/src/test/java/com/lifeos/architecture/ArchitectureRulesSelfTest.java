package com.lifeos.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Luật ArchUnit phải THẬT SỰ đỏ khi gặp vi phạm (BACKEND_SKELETON_PLAN §8: "chưa thử thêm class vi phạm"). Class vi
 * phạm nằm ở package com.lifeos.architecture.fixture.{controller,entity} (có đoạn ".controller" / ".entity" để khớp that()), chỉ
 * nằm trong mã test nên ArchitectureTest (DoNotIncludeTests) không thấy, và không có annotation Spring/JPA cấp class
 * nên không được Spring hay Hibernate nạp.
 */
class ArchitectureRulesSelfTest {

    private static EvaluationResult evaluate(com.tngtech.archunit.lang.ArchRule rule, Class<?>... classes) {
        JavaClasses imported = new ClassFileImporter().importClasses(classes);
        return rule.evaluate(imported);
    }

    @Test
    void a8FlagsAPackagePrivateControllerMethodReturningAnEntity() {
        EvaluationResult result = evaluate(ArchitectureRules.a8ControllerDoesNotReturnEntity(),
                com.lifeos.architecture.fixture.controller.LeakyController.class,
                com.lifeos.architecture.fixture.entity.FixtureEntity.class);
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).anyMatch(d -> d.contains("leak"));
    }

    @Test
    void a11FlagsJpaRelationsAndEagerFetchOnEntityFields() {
        EvaluationResult result = evaluate(ArchitectureRules.a11NoJpaRelationsOrEagerFetch(),
                com.lifeos.architecture.fixture.entity.FixtureEntity.class);
        List<String> details = result.getFailureReport().getDetails();
        assertThat(details).anyMatch(d -> d.contains("owner") && d.contains("ManyToOne"));
        assertThat(details).anyMatch(d -> d.contains("children") && d.contains("OneToMany"));
        assertThat(details).anyMatch(d -> d.contains("owner") && d.contains("EAGER"));
        assertThat(details).noneMatch(d -> d.contains("plainColumn"));
    }

    @Test
    void a13FlagsRepositoryEntityAndOtherModuleDependenciesOfSyncAdapters() {
        EvaluationResult result = evaluate(ArchitectureRules.a13SyncAdaptersDependOnlyOnServiceDtoAndCommon(),
                com.lifeos.task.sync.fixture.LeakySyncAdapter.class);
        List<String> details = result.getFailureReport().getDetails();
        assertThat(details).anyMatch(d -> d.contains("TaskRepository"));
        assertThat(details).anyMatch(d -> d.contains("entity.Task"));
        assertThat(details).anyMatch(d -> d.contains("ProjectQueryService"));
        assertThat(details).noneMatch(d -> d.contains("TaskQueryService") || d.contains("TaskResponse"));
    }

}
