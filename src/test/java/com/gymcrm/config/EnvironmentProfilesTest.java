package com.gymcrm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentProfilesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @ParameterizedTest(name = "{0} profile uses {1} with ddl-auto={3}")
    @CsvSource({
            "local, jdbc:h2:mem:gymdb_local, org.h2.Driver, create-drop",
            "dev,   jdbc:postgresql://localhost:5432/gymcrm_dev, org.postgresql.Driver, update",
            "stg,   jdbc:postgresql://stg-db.gymcrm.internal:5432/gymcrm_stg, org.postgresql.Driver, update",
            "prod,  jdbc:postgresql://prod-db.gymcrm.internal:5432/gymcrm_prod, org.postgresql.Driver, validate"
    })
    void eachProfileDefinesItsOwnDatabase(String profile, String urlPrefix, String driver, String ddlAuto) {
        runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            var environment = context.getEnvironment();
            assertTrue(environment.getRequiredProperty("spring.datasource.url").startsWith(urlPrefix));
            assertEquals(driver, environment.getRequiredProperty("spring.datasource.driver-class-name"));
            assertEquals(ddlAuto, environment.getRequiredProperty("spring.jpa.hibernate.ddl-auto"));
        });
    }

    @Test
    void localProfileIsUsedWhenNoProfileIsRequested() {
        runner.run(context -> assertTrue(
                context.getEnvironment().getRequiredProperty("spring.datasource.url").startsWith("jdbc:h2:mem:")));
    }

    @Test
    void databaseSettingsCanBeOverriddenThroughEnvironmentVariables() {
        runner.withPropertyValues("spring.profiles.active=prod",
                        "DB_URL=jdbc:postgresql://db.example:5432/custom",
                        "DB_USERNAME=custom-user")
                .run(context -> {
                    var environment = context.getEnvironment();
                    assertEquals("jdbc:postgresql://db.example:5432/custom",
                            environment.getRequiredProperty("spring.datasource.url"));
                    assertEquals("custom-user", environment.getRequiredProperty("spring.datasource.username"));
                });
    }

    @Test
    void prometheusEndpointIsExposedInEveryEnvironment() {
        runner.withPropertyValues("spring.profiles.active=prod").run(context -> assertTrue(
                context.getEnvironment().getRequiredProperty("management.endpoints.web.exposure.include")
                        .contains("prometheus")));
    }
}
