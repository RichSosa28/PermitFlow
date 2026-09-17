package permitflow.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Converts a validated JWT into the only tenant identity used by application code. */
public final class TenantContext {
    private TenantContext() {}

    public static TenantPrincipal from(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("JWT authentication required");
        }
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null) {
            throw new IllegalArgumentException("tenant_id claim is required");
        }
        return new TenantPrincipal(UUID.fromString(tenantId), jwt.getSubject());
    }
}
