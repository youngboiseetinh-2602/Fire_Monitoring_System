package com.firemonitoring.api;

import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PublicControllerTest {
    private final UserService service = mock(UserService.class);
    private MockMvc mvc;
    private static final String VALID = """
            {"username":"register_test_01","password":"Test@12345",
             "fullName":"Nguyen Van An","phone":"0912345678",
             "cccd":"001234567890","role":"MEMBER"}
            """;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PublicController(service))
                .setControllerAdvice(new GlobalException()).build();
    }

    @Test
    void acceptsValidRegistration() throws Exception {
        when(service.register(any())).thenReturn("register successfully");
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk()).andExpect(content().string("register successfully"));
        verify(service).register(any());
    }

    @Test
    void rejectsInvalidFieldsBeforeCallingService() throws Exception {
        String[][] cases = {{"register_test_01", ""}, {"Test@12345", "123"},
                {"Nguyen Van An", ""}, {"0912345678", "123"}, {"001234567890", ""}};
        for (String[] item : cases) {
            mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON)
                    .content(VALID.replace(item[0], item[1])))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingOrUnknownRole() throws Exception {
        for (String body : new String[]{VALID.replace("\"MEMBER\"", "null"), VALID.replace("MEMBER", "UNKNOWN")}) {
            mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void translatesDatabaseConflictTo409() throws Exception {
        when(service.register(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordReturnsVietnameseMessage() throws Exception {
        when(service.login(any())).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));
        assertInvalidLoginResponse();
    }

    @Test
    void unknownUsernameReturnsSameMessage() throws Exception {
        when(service.login(any())).thenThrow(
                new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));
        assertInvalidLoginResponse();
    }

    private void assertInvalidLoginResponse() throws Exception {
        mvc.perform(post("/public/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test_user","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Sai tài khoản hoặc mật khẩu"));
    }
}
