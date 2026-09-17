package permitflow.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import permitflow.service.PermitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PermitController.class)
class PermitControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PermitService permits;

    @Test
    void rejectsInvalidRequestBeforeReachingTheService() throws Exception {
        mvc.perform(post("/v1/permits")
                .with(jwt().jwt(token -> token.subject("demo").claim("tenant_id", "e4d09552-f4ae-4154-99e1-5c0b7ff6e863")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reference\":\"\",\"applicantName\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
