package permitflow.domain;

import java.time.Instant;
import java.util.UUID;

public record Permit(
    UUID id,
    String reference,
    String applicantName,
    PermitStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
