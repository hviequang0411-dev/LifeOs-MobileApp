package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** DWP2: guardrail tĩnh cho image/config production; không cần Docker hay network. */
class ProductionDeploymentContractTest {

    private static String read(String relativeToBackend) throws Exception {
        return Files.readString(Path.of(relativeToBackend));
    }

    @Test
    void productionImageIsNonRootUtcAndHasSeparateMigrationTarget() throws Exception {
        String dockerfile = read("Dockerfile");
        assertThat(dockerfile)
                .contains("AS migrations", "AS runtime", "USER ${APP_UID}:${APP_GID}", "TZ=UTC",
                        "-Duser.timezone=UTC", "HEALTHCHECK", "/actuator/health/liveness")
                .doesNotContain("JWT_SECRET=", "DB_PASSWORD=", "OPENAI_API_KEY=");
    }

    @Test
    void productionProfileDisablesRuntimeFlywayAndRequiresPersistentAndProxyConfig() throws Exception {
        String prod = read("src/main/resources/application-prod.yml");
        assertThat(prod)
                .contains("enabled: false", "username: ${SMTP_USER}", "password: ${SMTP_PASSWORD}",
                        "internal-proxies: ${TRUSTED_PROXIES}", "dir: ${EXPORT_DIR}");
    }

    @Test
    void composeUsesManagedDatabaseLeastPrivilegeAndFailFastVariables() throws Exception {
        for (String name : new String[] {"compose.production.yml", "compose.staging.yml"}) {
            String compose = read("../deploy/" + name);
            assertThat(compose)
                    .contains("MIGRATION_DB_USER:?", "DB_USER:?", "SPRING_FLYWAY_ENABLED: \"false\"",
                            "read_only: true", "cap_drop: [ALL]", "no-new-privileges:true",
                            "127.0.0.1:${BACKEND_PORT", "lifeos-exports:/var/lib/lifeos/exports")
                    .doesNotContain("POSTGRES_PASSWORD: lifeos", "image: postgres:", "latest");
        }
    }
}
