package permitflow.api;

import permitflow.domain.AuditEvent;
import permitflow.domain.Permit;
import permitflow.security.TenantContext;
import permitflow.service.PermitService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/permits")
public class PermitController {
    private final PermitService permits;

    public PermitController(PermitService permits) {
        this.permits = permits;
    }

    @Operation(summary = "Create a permit in the caller's verified tenant")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Permit create(@Valid @RequestBody CreatePermitRequest request, Authentication authentication) {
        return permits.create(TenantContext.from(authentication), request.reference(), request.applicantName());
    }

    @GetMapping
    public List<Permit> list(Authentication authentication) {
        return permits.list(TenantContext.from(authentication));
    }

    @GetMapping("/{id}")
    public Permit get(@PathVariable UUID id, Authentication authentication) {
        return permits.get(TenantContext.from(authentication), id).orElseThrow(NotFoundException::new);
    }

    @GetMapping("/{id}/audit-events")
    public List<AuditEvent> audit(@PathVariable UUID id, Authentication authentication) {
        return permits.auditEvents(TenantContext.from(authentication), id);
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException extends RuntimeException {
    NotFoundException() {
        super("Resource not found");
    }
}
