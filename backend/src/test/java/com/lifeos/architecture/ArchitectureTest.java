package com.lifeos.architecture;

import static com.lifeos.architecture.ArchitectureRules.ALLOWED_DEPENDENCIES;
import static com.lifeos.architecture.ArchitectureRules.MODULES;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * Chạy toàn bộ luật A1–A12 trên mã production. Luật chạy như test thường trong CI; không tắt, không thêm ngoại lệ.
 */
@AnalyzeClasses(packages = "com.lifeos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule a1 = ArchitectureRules.a1ControllerDoesNotTouchRepositoryOrEntity();

    @ArchTest
    static final ArchRule a2 = ArchitectureRules.a2ServiceDoesNotDependOnWeb();

    @ArchTest
    static void a3(JavaClasses classes) {
        for (String module : MODULES) {
            ArchitectureRules.a3RepositoryOnlyUsedBySameModuleService(module).check(classes);
        }
    }

    @ArchTest
    static void a4(JavaClasses classes) {
        for (String from : MODULES) {
            for (String to : MODULES) {
                if (!from.equals(to)) {
                    ArchitectureRules.a4OnlyApiPackageCrossModule(from, to).check(classes);
                }
            }
        }
    }

    @ArchTest
    static final ArchRule a5 = ArchitectureRules.a5NoCyclesBetweenModules();

    @ArchTest
    static void a6(JavaClasses classes) {
        for (String from : MODULES) {
            for (String to : MODULES) {
                boolean allowed = ALLOWED_DEPENDENCIES.getOrDefault(from, List.of()).contains(to);
                if (!from.equals(to) && !allowed) {
                    ArchitectureRules.a6ForbiddenModuleDependency(from, to).check(classes);
                }
            }
        }
    }

    @ArchTest
    static final ArchRule a7 = ArchitectureRules.a7BusinessModulesDoNotImportSync();

    @ArchTest
    static final ArchRule a8 = ArchitectureRules.a8ControllerDoesNotReturnEntity();

    @ArchTest
    static final ArchRule a9 = ArchitectureRules.a9CommandServicesDependOnChangeRecorder();

    @ArchTest
    static final ArchRule a10Class = ArchitectureRules.a10TransactionalOnlyInService();

    @ArchTest
    static final ArchRule a10Method = ArchitectureRules.a10TransactionalMethodsOnlyInService();

    @ArchTest
    static final ArchRule a11 = ArchitectureRules.a11NoJpaRelationsOrEagerFetch();

    @ArchTest
    static final ArchRule a12 = ArchitectureRules.a12CommonDoesNotDependOnModules();

    @ArchTest
    static final ArchRule a13 = ArchitectureRules.a13SyncAdaptersDependOnlyOnServiceDtoAndCommon();

    @ArchTest
    static final ArchRule a14 = ArchitectureRules.a14FinanceHasNoFloatingPoint();

    @ArchTest
    static final ArchRule a15 = ArchitectureRules.a15AiUsesOnlyOtherModuleApi();
}
