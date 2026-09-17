package permitflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermitRequest(
    @NotBlank @Size(max = 64) String reference,
    @NotBlank @Size(max = 160) String applicantName
) {}
