package com.firemonitoring.api;

import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserUpdateValidationTest {
    @ParameterizedTest
    @ValueSource(strings = {"00123456789", "0012345678901", "001234567892a", "00123456789!"})
    void rejectsInvalidCccdOnUpdate(String cccd) throws Exception {
        UserService service = mock(UserService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalException()).build();
        String body = new ObjectMapper().writeValueAsString(Map.of(
                "fullName", "Nguyen An", "phone", "0912345678", "cccd", cccd));
        mvc.perform(put("/users/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"cccd":"CCCD must contain exactly 12 digits"}
                        """, true));
        verifyNoInteractions(service);
    }
}
