package com.firemonitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegisterValidationTest {
    private final UserService service = mock(UserService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PublicController(service))
                .setControllerAdvice(new GlobalException()).build();
    }

    private ObjectNode validBody() {
        return new ObjectMapper().createObjectNode()
                .put("username", "register_test").put("password", "Test@12345")
                .put("fullName", "Nguyen An").put("phone", "0912345678")
                .put("cccd", "001234567890").put("role", "MEMBER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "password", "fullName", "cccd", "role"})
    void rejectsMissingAndNullRequiredFields(String field) throws Exception {
        ObjectNode body = validBody();
        body.remove(field);
        expectValidationError(body, field);
        body.putNull(field);
        expectValidationError(body, field);
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("username", "   "), Arguments.of("username", "a".repeat(51)),
                Arguments.of("password", "a".repeat(7)), Arguments.of("password", "a".repeat(101)),
                Arguments.of("fullName", "   "), Arguments.of("fullName", "a".repeat(101)),
                Arguments.of("cccd", "   "), Arguments.of("cccd", "1".repeat(21)),
                Arguments.of("cccd", "00123456789"), Arguments.of("cccd", "0012345678901"),
                Arguments.of("cccd", "001234567892a"), Arguments.of("cccd", "00123456789a"),
                Arguments.of("cccd", "00123456789!"), Arguments.of("cccd", "001234567890\n"),
                Arguments.of("phone", ""), Arguments.of("phone", "1912345678"),
                Arguments.of("phone", "091234567"), Arguments.of("phone", "09123456789"),
                Arguments.of("phone", "091234567a"));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidFieldWithFieldSpecificError(String field, String value) throws Exception {
        expectValidationError(validBody().put(field, value), field);
        verifyNoInteractions(service);
    }

    @Test
    void acceptsOmittedAndNullOptionalPhone() throws Exception {
        when(service.register(any())).thenReturn("register successfully");
        ObjectNode body = validBody();
        body.remove("phone");
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk());
        body.putNull("phone");
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk());
        verify(service, times(2)).register(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "null"})
    void rejectsMissingOrMalformedBody(String body) throws Exception {
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private void expectValidationError(ObjectNode body, String field) throws Exception {
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + field).isNotEmpty())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void phoneErrorContainsOnlyPhoneMessage() throws Exception {
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().put("phone", "098765431").toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {"phone":"Phone must contain 10 digits and start with 0"}
                        """, true));
        verifyNoInteractions(service);
    }
}
