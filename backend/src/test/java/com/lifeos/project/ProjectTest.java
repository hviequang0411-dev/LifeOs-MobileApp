package com.lifeos.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.project.entity.Project;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Hành vi miền của Project (TESTING_RULES §5.1): lưu trữ/bỏ lưu trữ, đổi tên/mô tả, xóa mềm; báo có thay đổi thật. */
class ProjectTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private static Project project() {
        return Project.create(UUID.randomUUID(), UUID.randomUUID(), "Tên", "Mô tả");
    }

    @Test
    void newProjectIsActiveAndNotYetVersioned() {
        Project p = project();
        assertThat(p.isNew()).isTrue();
        assertThat(p.isArchived()).isFalse();
        assertThat(p.isDeleted()).isFalse();
        assertThat(p.getVersion()).isZero(); // version = 1 được đặt khi persist (SyncedEntity)
    }

    @Test
    void archiveAndUnarchiveReportWhetherAnythingChanged() {
        Project p = project();
        assertThat(p.archive(NOW)).isTrue();
        assertThat(p.getArchivedAt()).isEqualTo(NOW);
        assertThat(p.archive(NOW.plusSeconds(5))).isFalse();
        assertThat(p.getArchivedAt()).isEqualTo(NOW);

        assertThat(p.unarchive()).isTrue();
        assertThat(p.isArchived()).isFalse();
        assertThat(p.unarchive()).isFalse();
    }

    @Test
    void renameAndDescribeReportWhetherAnythingChanged() {
        Project p = project();
        assertThat(p.rename("Tên")).isFalse();
        assertThat(p.rename("Khác")).isTrue();
        assertThat(p.describe("Mô tả")).isFalse();
        assertThat(p.describe(null)).isTrue();
        assertThat(p.getDescription()).isNull();
        assertThat(p.describe(null)).isFalse();
    }

    @Test
    void softDeleteOnlyOnce() {
        Project p = project();
        p.softDelete(NOW);
        assertThat(p.isDeleted()).isTrue();
        assertThat(p.getDeletedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> p.softDelete(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void equalityIsByIdAndType() {
        UUID id = UUID.randomUUID();
        Project a = Project.create(id, UUID.randomUUID(), "A", null);
        Project b = Project.create(id, UUID.randomUUID(), "B", null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(project());
    }
}
