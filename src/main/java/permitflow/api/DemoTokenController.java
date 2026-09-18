package permitflow.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Demo-only token issuer. Production accepts tokens from the configured identity provider. */
@Profile("demo")
@RestController
public class DemoTokenController {
    private static final Map<String, String> PERSONAS = Map.of(
        "alice-city", "11111111-1111-1111-1111-111111111111",
        "bob-county", "22222222-2222-2222-2222-222222222222"
    );
    private final String secret;
    private final ObjectMapper json;

    public DemoTokenController(@Value("${spring.security.oauth2.resourceserver.jwt.secret}") String secret, ObjectMapper json) {
        this.secret = secret;
        this.json = json;
    }

    @GetMapping("/demo/tokens/{persona}")
    public Map<String, Object> issue(@PathVariable String persona) throws Exception {
        String tenantId = PERSONAS.get(persona);
        if (tenantId == null) throw new NotFoundException();
        Instant now = Instant.now();
        String header = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64(payload(persona, tenantId, now));
        String signed = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Map.of("access_token", signed + "." + base64(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8))), "persona", persona, "expires_in", 900);
    }

    private byte[] payload(String persona, String tenantId, Instant now) throws JsonProcessingException {
        return json.writeValueAsBytes(Map.of("sub", persona, "tenant_id", tenantId, "iat", now.getEpochSecond(), "exp", now.plusSeconds(900).getEpochSecond()));
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
