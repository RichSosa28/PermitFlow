package permitflow.security;

import java.util.UUID;

public record TenantPrincipal(UUID tenantId, String subject) {}
