package com.lifeos.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.privacy.UserDataCatalog;
import com.lifeos.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * HAB-12 (PHASE_2 §9; Phase 7 INT-15; BACKEND_MASTER_PLAN §3.6): MỌI bảng có cột user_id phải nằm trong
 * {@link UserDataCatalog} (danh mục xuất/xóa tài khoản). Thêm bảng mới mà quên khai báo → test này đỏ.
 */
class UserDataCatalogIT extends IntegrationTestBase {

    @Autowired
    JdbcClient jdbc;

    @Test
    void Hab_12_everyTableWithUserIdIsInTheExportDeleteCatalog() {
        List<String> tables = jdbc.sql("""
                SELECT table_name FROM information_schema.columns
                WHERE table_schema = 'public' AND column_name = 'user_id'""").query(String.class).list();
        assertThat(UserDataCatalog.TABLES).as("danh mục xuất/xóa = mọi bảng có user_id")
                .containsExactlyInAnyOrderElementsOf(tables);
        assertThat(UserDataCatalog.TABLES).doesNotHaveDuplicates();
    }
}
