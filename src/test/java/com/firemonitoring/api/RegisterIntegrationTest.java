package com.firemonitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.repository.UserRepository;
import com.firemonitoring.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises controller, validation, service and real H2 constraints; security filters are excluded. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class RegisterIntegrationTest {
    @Autowired
    private UserRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        UserServiceImpl service = new UserServiceImpl(repository, new ModelMapper(), encoder,
                null, null, null, new SearchBuilderConverter());
        mvc = MockMvcBuilders.standaloneSetup(new PublicController(service))
                .setControllerAdvice(new GlobalException()).build();
    }

    private ObjectNode body() {
        return new ObjectMapper().createObjectNode()
                .put("username", "register_test").put("password", "Test@12345")
                .put("fullName", "Nguyen An").put("phone", "0912345678")
                .put("cccd", "001234567890").put("role", "MEMBER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEMBER", "ADMIN", "OWNER"})
    void storesHashedPasswordAndAlwaysAssignsMember(String requestedRole) throws Exception {
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("role", requestedRole).toString()))
                .andExpect(status().isOk()).andExpect(content().string("register successfully"));
        var user = repository.findByUsername("register_test").orElseThrow();
        assertNotNull(user.getId());
        assertEquals(UserRole.MEMBER, user.getRole());
        assertEquals("Nguyen An", user.getFullName());
        assertEquals("0912345678", user.getPhone());
        assertEquals("001234567890", user.getCccd());
        assertNotEquals("Test@12345", user.getPassword());
        assertTrue(encoder.matches("Test@12345", user.getPassword()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "phone", "cccd"})
    void duplicateUniqueFieldReturns409(String field) throws Exception {
        ObjectNode first = body();
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(first.toString()))
                .andExpect(status().isOk());
        ObjectNode second = body().put("username", "another_user")
                .put("phone", "0912345679").put("cccd", "001234567891");
        second.put(field, first.get(field).asText());
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON).content(second.toString()))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationDoesNotCreateUser() throws Exception {
        mvc.perform(post("/public/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("password", "123").toString()))
                .andExpect(status().isBadRequest());
        assertFalse(repository.existsByUsername("register_test"));
    }
}
