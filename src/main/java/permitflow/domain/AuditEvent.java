package permitflow.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(UUID id, String action, UUID resourceId, Instant occurredAt) {}
