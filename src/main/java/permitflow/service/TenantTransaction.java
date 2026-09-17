package permitflow.service;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Establishes database tenant context only after Spring Security validates the JWT. */
@Component
public class TenantTransaction {
    private final TransactionTemplate transactions;
    private final JdbcTemplate jdbc;

    public TenantTransaction(TransactionTemplate transactions, JdbcTemplate jdbc) {
        this.transactions = transactions;
        this.jdbc = jdbc;
    }

    public <T> T inTenant(UUID tenantId, Supplier<T> work) {
        return Objects.requireNonNull(transactions.execute(status -> {
            jdbc.queryForObject("select set_config('app.tenant_id', ?, true)", String.class, tenantId.toString());
            return work.get();
        }), "Tenant transaction returned no result");
    }
}
