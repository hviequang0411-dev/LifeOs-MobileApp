package com.lifeos.common.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** INT-12 (phần đơn vị, D204): CSV chống formula injection và đúng RFC 4180; danh mục xuất phủ kín (INT-15). */
class UserDataExporterTest {

    @Test
    void Int_12_csvCellsAreEscapedAgainstFormulaInjection() {
        assertThat(UserDataExporter.csvLine(List.of("=1+1", "+cmd", "-2", "@SUM(A1)", "\tx", "bình thường")))
                .isEqualTo("'=1+1,'+cmd,'-2,'@SUM(A1),'\tx,bình thường\r\n");
        assertThat(UserDataExporter.csvLine(List.of("a,b", "nói \"chào\"", "dòng\nhai", "")))
                .isEqualTo("\"a,b\",\"nói \"\"chào\"\"\",\"dòng\nhai\",\r\n");
        assertThat(UserDataExporter.csvLine(List.of("=HYPERLINK(\"x\",\"y\")")))
                .isEqualTo("\"'=HYPERLINK(\"\"x\"\",\"\"y\"\")\"\r\n");
    }

    @Test
    void Int_15_everyCatalogTableIsEitherExportedOrExcludedWithAReason() {
        assertThat(UserDataCatalog.EXPORT_EXCLUDED.keySet()).isSubsetOf(UserDataCatalog.TABLES);
        assertThat(UserDataCatalog.EXPORT_EXCLUDED.values()).allSatisfy(reason -> assertThat(reason).isNotBlank());
        assertThat(UserDataCatalog.exportedTables().size() + UserDataCatalog.EXPORT_EXCLUDED.size())
                .isEqualTo(UserDataCatalog.TABLES.size());
        assertThat(UserDataCatalog.exportedTables()).contains("tasks", "notes", "financial_transactions", "reviews",
                "user_settings").doesNotContain("refresh_tokens", "change_logs", "sync_mutations",
                "device_push_tokens");
    }
}
