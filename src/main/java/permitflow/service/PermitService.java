package permitflow.service;

import permitflow.domain.AuditEvent;
import permitflow.domain.Permit;
import permitflow.domain.PermitStatus;
import permitflow.security.TenantPrincipal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class PermitService {
    private static final RowMapper<Permit> PERMIT_MAPPER = (rs, rowNum) -> mapPermit(rs);
    private final JdbcTemplate jdbc;
    private final TenantTransaction tenantTransaction;

    public PermitService(JdbcTemplate jdbc, TenantTransaction tenantTransaction) {
        this.jdbc = jdbc;
        this.tenantTransaction = tenantTransaction;
    }

    public Permit create(TenantPrincipal principal, String reference, String applicantName) {
        return tenantTransaction.inTenant(principal.tenantId(), () -> {
            UUID id = UUID.randomUUID();
            jdbc.update(
                "insert into permits (id, tenant_id, reference, applicant_name, status) values (?, ?, ?, ?, 'DRAFT')",
                id, principal.tenantId(), reference, applicantName
            );
            audit(principal, "permit.created", id);
            return findById(id).orElseThrow();
        });
    }

    public List<Permit> list(TenantPrincipal principal) {
        return tenantTransaction.inTenant(principal.tenantId(), () -> jdbc.query(
            "select id, reference, applicant_name, status, created_at, updated_at from permits order by created_at desc",
            PERMIT_MAPPER
        ));
    }

    public java.util.Optional<Permit> get(TenantPrincipal principal, UUID id) {
        return tenantTransaction.inTenant(principal.tenantId(), () -> findById(id));
    }

    public List<AuditEvent> auditEvents(TenantPrincipal principal, UUID id) {
        return tenantTransaction.inTenant(principal.tenantId(), () -> jdbc.query(
            "select id, action, resource_id, occurred_at from audit_events where resource_id = ? order by occurred_at",
            (rs, rowNum) -> new AuditEvent(
                UUID.fromString(rs.getString("id")),
                rs.getString("action"),
                UUID.fromString(rs.getString("resource_id")),
                rs.getTimestamp("occurred_at").toInstant()
            ),
            id
        ));
    }

    private java.util.Optional<Permit> findById(UUID id) {
        return jdbc.query(
            "select id, reference, applicant_name, status, created_at, updated_at from permits where id = ?",
            PERMIT_MAPPER,
            id
        ).stream().findFirst();
    }

    private void audit(TenantPrincipal principal, String action, UUID resourceId) {
        jdbc.update(
            "insert into audit_events (id, tenant_id, actor_subject, action, resource_id) values (?, ?, ?, ?, ?)",
            UUID.randomUUID(), principal.tenantId(), principal.subject(), action, resourceId
        );
    }

    private static Permit mapPermit(ResultSet rs) throws SQLException {
        return new Permit(
            UUID.fromString(rs.getString("id")),
            rs.getString("reference"),
            rs.getString("applicant_name"),
            PermitStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
        );
    }
}
