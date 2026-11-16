package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.sync.service.SyncSnapshotService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import org.junit.jupiter.api.Test;

/**
 * Đợt sửa backend 1 (F9): khóa HMAC snapshot là secret bắt buộc — application.yml và profile prod không có giá trị mặc
 * định (thiếu SNAPSHOT_HMAC_SECRET thì không khởi động), chỉ dev/test có giá trị riêng; khóa ngắn bị từ chối.
 */
class SnapshotSecretConfigTest {

    private static String read(String relativeToBackend) throws Exception {
        return Files.readString(Path.of(relativeToBackend));
    }

    @Test
    void F9_theSecretHasNoDefaultOutsideDevAndTest() throws Exception {
        assertThat(read("src/main/resources/application.yml"))
                .contains("snapshot-hmac-secret: ${SNAPSHOT_HMAC_SECRET}");
        assertThat(read("src/main/resources/application-prod.yml")).doesNotContain("snapshot-hmac-secret");
        assertThat(read("src/main/resources/application-dev.yml"))
                .contains("snapshot-hmac-secret: ${SNAPSHOT_HMAC_SECRET:dev-only-");
        assertThat(read("src/test/resources/application-test.yml")).contains("snapshot-hmac-secret: test-only-");
    }

    @Test
    void F9_aMissingOrShortSecretStopsStartup() {
        for (String secret : new String[] {null, "", "ngắn-quá"}) {
            assertThatThrownBy(() -> new SyncSnapshotService(null, null, null, null, null, Clock.systemUTC(), secret))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("SNAPSHOT_HMAC_SECRET");
        }
    }
}
