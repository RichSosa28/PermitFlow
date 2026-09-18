package permitflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import permitflow.domain.Permit;
import permitflow.security.TenantPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class TenantIsolationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
    new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("permitflow_test")
        .withInitScript("db/test-init.sql");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "permitflow_app");
        registry.add("spring.datasource.password", () -> "permitflow_test_password");
    }

    @Autowired
    private PermitService permits;

    @Test
    void rlsExcludesAnotherTenantsRowsBeforeTheServiceReceivesThem() {
        TenantPrincipal alice = new TenantPrincipal(UUID.randomUUID(), "alice");
        TenantPrincipal bob = new TenantPrincipal(UUID.randomUUID(), "bob");

        Permit created = permits.create(alice, "A-100", "Alice Applicant");

        assertThat(permits.list(alice)).extracting(Permit::reference).containsExactly("A-100");
        assertThat(permits.list(bob)).isEmpty();
        assertThat(permits.get(bob, created.id())).isEmpty();
    }
}
