package com.lifeos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Các luật kiến trúc A1–A15 (SYSTEM_ARCHITECTURE.md §3.9, PHASE_8). Mỗi luật là một phương thức để test có thể
 * kiểm tra từng luật riêng. Không thêm ngoại lệ để làm test xanh (CODE_GUIDELINES.md §16): sửa cấu trúc.
 */
final class ArchitectureRules {

    static final String ROOT = "com.lifeos";
    /** Module nghiệp vụ + module hạ tầng đồng bộ (không tính common). */
    static final List<String> MODULES = List.of("identity", "project", "task", "habit", "routine", "finance",
            "calendar", "notification", "goal", "note", "analytics", "sync", "ai");
    static final List<String> BUSINESS_MODULES = List.of("identity", "project", "task", "habit", "routine",
            "finance", "calendar", "notification", "goal", "note", "analytics");
    /**
     * Cặp phụ thuộc module được phép (A6), luôn chỉ qua gói api/ (A4): task → project; calendar → task (time block,
     * PHASE_4 D128); notification → identity, task, calendar, habit, routine (reminder/người nhận, D120, D129) [WP4];
     * goal → task (gỡ liên kết/tiến độ, D185, D186), note → task, goal (note_link, D189) [WP6]. Task kiểm liên kết Goal
     * qua common.goal.GoalLinks (đảo phụ thuộc, không có task → goal). analytics → task, habit, finance, calendar, goal
     * (chỉ API thống kê đọc, PHASE_7 D197, INT-19) [WP7].
     */
    static final Map<String, List<String>> ALLOWED_DEPENDENCIES = Map.of("task", List.of("project"),
            "calendar", List.of("task"),
            "notification", List.of("identity", "task", "calendar", "habit", "routine"),
            "goal", List.of("task"),
            "note", List.of("task", "goal"),
            "analytics", List.of("task", "habit", "finance", "calendar", "goal"),
            "ai", List.of("task", "calendar", "habit", "goal", "note", "finance"));

    private ArchitectureRules() {
    }

    private static String pkg(String module) {
        return ROOT + "." + module + "..";
    }

    // ---- A1
    static ArchRule a1ControllerDoesNotTouchRepositoryOrEntity() {
        return noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..entity..")
                .as("A1: controller không phụ thuộc repository/entity");
    }

    // ---- A2
    static ArchRule a2ServiceDoesNotDependOnWeb() {
        return noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("jakarta.servlet.http.HttpServletRequest")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.http.ResponseEntity")
                .as("A2: service không phụ thuộc controller, HttpServletRequest, ResponseEntity");
    }

    // ---- A3 (theo từng module)
    static ArchRule a3RepositoryOnlyUsedBySameModuleService(String module) {
        String base = ROOT + "." + module;
        return classes().that().resideInAPackage(base + ".repository..")
                .should().onlyHaveDependentClassesThat().resideInAnyPackage(base + ".service..", base + ".repository..")
                .as("A3: repository của " + module + " chỉ được dùng bởi service cùng module");
    }

    // ---- A4 (theo từng cặp module): chỉ được import api/ và common của module khác
    static ArchRule a4OnlyApiPackageCrossModule(String from, String to) {
        DescribedPredicate<JavaClass> nonApiOfTarget =
                resideInAPackage(pkg(to)).and(not(resideInAPackage(ROOT + "." + to + ".api..")));
        return noClasses().that().resideInAPackage(pkg(from))
                .should().dependOnClassesThat(nonApiOfTarget)
                .as("A4: " + from + " chỉ được import " + to + ".api (không import phần nội bộ)");
    }

    // ---- A5
    static ArchRule a5NoCyclesBetweenModules() {
        return slices().matching(ROOT + ".(*)..").should().beFreeOfCycles()
                .as("A5: không có vòng phụ thuộc giữa module");
    }

    // ---- A6 (theo từng cặp module không được phép)
    static ArchRule a6ForbiddenModuleDependency(String from, String to) {
        return noClasses().that().resideInAPackage(pkg(from))
                .should().dependOnClassesThat().resideInAPackage(pkg(to))
                .as("A6: " + from + " không được phụ thuộc " + to + " (ngoài ma trận cho phép)");
    }

    // ---- A7
    static ArchRule a7BusinessModulesDoNotImportSync() {
        return noClasses().that().resideInAnyPackage(
                        BUSINESS_MODULES.stream().map(ArchitectureRules::pkg).toArray(String[]::new))
                .should().dependOnClassesThat().resideInAPackage(pkg("sync"))
                .as("A7: module nghiệp vụ không import com.lifeos.sync..");
    }

    // ---- A8. Kiểm MỌI method của controller (không chỉ public): method xử lý request của dự án là package-private, nên
    // bản cũ chỉ lọc public không khớp method nào và chưa từng kiểm tra gì (lộ ra khi bật failOnEmptyShould, Giai đoạn 2).
    static ArchRule a8ControllerDoesNotReturnEntity() {
        return methods().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                .should(new ArchCondition<JavaMethod>("không trả về class trong ..entity..") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        for (JavaClass type : method.getReturnType().getAllInvolvedRawTypes()) {
                            if (type.getPackageName().contains(".entity")) {
                                events.add(SimpleConditionEvent.violated(method,
                                        method.getFullName() + " trả về entity " + type.getName()));
                            }
                        }
                    }
                })
                .as("A8: controller không trả về entity");
    }

    // ---- A9
    static ArchRule a9CommandServicesDependOnChangeRecorder() {
        return classes().that().haveSimpleNameEndingWith("CommandService")
                .should().dependOnClassesThat().haveFullyQualifiedName("com.lifeos.common.sync.ChangeRecorder")
                .as("A9: *CommandService phụ thuộc ChangeRecorder");
    }

    // ---- A10
    static ArchRule a10TransactionalOnlyInService() {
        return noClasses().that().resideOutsideOfPackage("..service..")
                .should().beAnnotatedWith(Transactional.class)
                .as("A10: @Transactional trên class chỉ ở ..service..");
    }

    static ArchRule a10TransactionalMethodsOnlyInService() {
        return methods().that().areDeclaredInClassesThat().resideOutsideOfPackage("..service..")
                .should().notBeAnnotatedWith(Transactional.class)
                .as("A10: @Transactional trên method chỉ ở ..service..");
    }

    // ---- A11 (viết lại ở Giai đoạn 2, Q16): chọn MỌI field của entity (không rỗng), cấm quan hệ JPA và EAGER.
    static final List<String> JPA_RELATIONS = List.of("jakarta.persistence.ManyToOne", "jakarta.persistence.OneToOne",
            "jakarta.persistence.OneToMany", "jakarta.persistence.ManyToMany", "jakarta.persistence.ElementCollection");

    static ArchRule a11NoJpaRelationsOrEagerFetch() {
        return fields().that().areDeclaredInClassesThat().resideInAPackage("..entity..")
                .should(new ArchCondition<JavaField>("không dùng quan hệ JPA (D3: tham chiếu bằng UUID) và không dùng "
                        + "fetch = EAGER") {
                    @Override
                    public void check(JavaField field, ConditionEvents events) {
                        for (String relation : JPA_RELATIONS) {
                            if (field.isAnnotatedWith(relation)) {
                                events.add(SimpleConditionEvent.violated(field, field.getFullName()
                                        + " dùng quan hệ JPA @" + relation.substring(relation.lastIndexOf('.') + 1)
                                        + "; tham chiếu bằng cột UUID"));
                            }
                        }
                        boolean eager = field.getAnnotations().stream()
                                .map(a -> a.tryGetExplicitlyDeclaredProperty("fetch"))
                                .flatMap(Optional::stream)
                                .anyMatch(v -> v instanceof JavaEnumConstant constant && "EAGER".equals(constant.name()));
                        if (eager) {
                            events.add(SimpleConditionEvent.violated(field,
                                    field.getFullName() + " khai báo fetch = EAGER"));
                        }
                    }
                })
                .as("A11: entity không dùng quan hệ JPA, không dùng FetchType.EAGER");
    }

    // ---- A12
    static ArchRule a12CommonDoesNotDependOnModules() {
        return noClasses().that().resideInAPackage(ROOT + ".common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        MODULES.stream().map(ArchitectureRules::pkg).toArray(String[]::new))
                .as("A12: common không phụ thuộc module nghiệp vụ");
    }
    // ---- A13 (D95): adapter sync trong module nghiệp vụ chỉ dịch, không chạm repository/entity hay module khác.
    static final List<String> A13_EXTERNAL_PREFIXES = List.of("java.", "org.springframework.", "tools.jackson.",
            "com.fasterxml.jackson.", ROOT + ".common.");

    static ArchRule a13SyncAdaptersDependOnlyOnServiceDtoAndCommon() {
        return classes().that().resideInAnyPackage(
                        BUSINESS_MODULES.stream().map(m -> ROOT + "." + m + ".sync..").toArray(String[]::new))
                .should(new ArchCondition<JavaClass>("chỉ phụ thuộc common.., service/dto cùng module, Spring, Jackson") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        String module = item.getPackageName().substring(ROOT.length() + 1).split("\\.")[0];
                        String own = ROOT + "." + module + ".";
                        item.getDirectDependenciesFromSelf().forEach(dependency -> {
                            JavaClass target = dependency.getTargetClass().getBaseComponentType();
                            String name = target.getPackageName();
                            boolean allowed = target.isPrimitive() || name.isEmpty()
                                    || A13_EXTERNAL_PREFIXES.stream().anyMatch(prefix -> (name + ".").startsWith(prefix))
                                    || (name + ".").startsWith(own + "service.") || (name + ".").startsWith(own + "dto.")
                                    || (name + ".").startsWith(own + "sync.");
                            if (!allowed) {
                                events.add(SimpleConditionEvent.violated(item, dependency.getDescription()));
                            }
                        });
                    }
                })
                .as("A13: com.lifeos.<module nghiệp vụ>.sync.. chỉ phụ thuộc common.., service/dto cùng module,"
                        + " Spring, Jackson");
    }

    // ---- A14 (PHASE_3 FIN-01, D100): module finance không dùng số thực dấu phẩy động ở bất kỳ đâu.
    static final java.util.Set<String> FLOATING = java.util.Set.of("double", "float", "java.lang.Double",
            "java.lang.Float");

    static ArchRule a14FinanceHasNoFloatingPoint() {
        return classes().that().resideInAPackage(ROOT + ".finance..")
                .should(new ArchCondition<JavaClass>("không dùng double/float (field, tham số, kiểu trả về, lời gọi)") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        item.getFields().forEach(f -> {
                            if (FLOATING.contains(f.getRawType().getName())) {
                                events.add(SimpleConditionEvent.violated(item, f.getFullName() + " là số thực"));
                            }
                        });
                        item.getCodeUnits().forEach(unit -> {
                            if (FLOATING.contains(unit.getRawReturnType().getName())
                                    || unit.getRawParameterTypes().stream()
                                            .anyMatch(t -> FLOATING.contains(t.getName()))) {
                                events.add(SimpleConditionEvent.violated(item, unit.getFullName()
                                        + " nhận/trả số thực"));
                            }
                        });
                        java.util.stream.Stream.<com.tngtech.archunit.core.domain.JavaCall<?>>concat(
                                item.getMethodCallsFromSelf().stream(), item.getConstructorCallsFromSelf().stream())
                                .forEach(call -> {
                            String owner = call.getTargetOwner().getName();
                            boolean floatingOwner = owner.equals("java.lang.Double") || owner.equals("java.lang.Float");
                            boolean floatingParam = call.getTarget().getRawParameterTypes().stream()
                                    .anyMatch(t -> FLOATING.contains(t.getName()));
                            boolean floatingResult = call.getTarget() instanceof com.tngtech.archunit.core.domain
                                    .AccessTarget.MethodCallTarget target
                                    && FLOATING.contains(target.getRawReturnType().getName());
                            if (floatingOwner || floatingParam || floatingResult) {
                                events.add(SimpleConditionEvent.violated(item, call.getDescription()));
                            }
                        });
                    }
                })
                .as("A14: com.lifeos.finance.. không dùng double/float (tiền là BigDecimal, D100)");
    }

    static ArchRule a15AiUsesOnlyOtherModuleApi() {
        return noClasses().that().resideInAPackage(ROOT + ".ai..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".task.repository..", ROOT + ".task.entity..",
                        ROOT + ".calendar.repository..", ROOT + ".calendar.entity..",
                        ROOT + ".habit.repository..", ROOT + ".habit.entity..",
                        ROOT + ".goal.repository..", ROOT + ".goal.entity..",
                        ROOT + ".note.repository..", ROOT + ".note.entity..",
                        ROOT + ".finance.repository..", ROOT + ".finance.entity..")
                .as("A15: AI chỉ đọc module khác qua *AiQueryApi, không chạm repository/entity");
    }
}
