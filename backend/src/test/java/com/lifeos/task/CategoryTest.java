package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.task.entity.Category;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Hành vi miền của Category: đổi tên báo có thay đổi thật (đổi riêng kiểu chữ cũng là thay đổi). */
class CategoryTest {

    @Test
    void renameReportsWhetherTheNameChanged() {
        Category category = Category.create(UUID.randomUUID(), UUID.randomUUID(), "Work");
        assertThat(category.rename("Work")).isFalse();
        assertThat(category.rename("work")).isTrue();
        assertThat(category.getName()).isEqualTo("work");
        assertThat(category.isNew()).isTrue();
        assertThat(category.getVersion()).isZero();
    }
}
