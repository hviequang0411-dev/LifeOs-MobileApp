package com.lifeos.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.common.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Con trỏ phân trang mờ (D59) và giới hạn limit (D79). */
class CursorsTest {

    @Test
    void roundTripKeepsSortKeyAndId() {
        UUID id = UUID.randomUUID();
        String cursor = Cursors.encode(new Cursors.Position("createdAt:desc", "2026-11-06T10:00:00Z", id));

        assertThat(cursor).doesNotContain("createdAt").doesNotContain("=");
        assertThat(Cursors.decode(cursor, "createdAt:desc"))
                .isEqualTo(new Cursors.Position("createdAt:desc", "2026-11-06T10:00:00Z", id));
    }

    @Test
    void nullKeyRoundTrips() {
        UUID id = UUID.randomUUID();
        String cursor = Cursors.encode(new Cursors.Position("dueDate:asc", null, id));
        assertThat(Cursors.decode(cursor, "dueDate:asc").key()).isNull();
    }

    @Test
    void missingCursorMeansFirstPage() {
        assertThat(Cursors.decode(null, "x")).isNull();
        assertThat(Cursors.decode("  ", "x")).isNull();
    }

    @Test
    void cursorForAnotherSortOrGarbageIsRejected() {
        String other = Cursors.encode(new Cursors.Position("dueDate:asc", "2026-01-01", UUID.randomUUID()));
        String twoParts = Base64.getUrlEncoder().encodeToString("a\nb".getBytes(StandardCharsets.UTF_8));
        String badUuid = Base64.getUrlEncoder().encodeToString("s\nk\nnot-uuid".getBytes(StandardCharsets.UTF_8));
        for (String cursor : new String[] {other, "%%%", twoParts, badUuid}) {
            assertThatThrownBy(() -> Cursors.decode(cursor, "createdAt:desc"))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).getProperties().toString())
                            .contains("field=cursor"));
        }
    }

    @Test
    void limitDefaultsTo50AndMustBeWithin1To200() {
        assertThat(Cursors.limit(null)).isEqualTo(50);
        assertThat(Cursors.limit(1)).isEqualTo(1);
        assertThat(Cursors.limit(200)).isEqualTo(200);
        for (int bad : new int[] {0, -5, 201}) {
            assertThatThrownBy(() -> Cursors.limit(bad)).isInstanceOf(ValidationException.class);
        }
    }
}
