package com.firemonitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firemonitoring.entity.House;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.enums.HouseStatus;
import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.service.impl.HouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercises controller, validation, mapping and persistence; security filters are excluded.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class HouseIntegrationTest {
    @Autowired
    private HouseRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new HouseController(
                        new HouseServiceImpl(repository, new ModelMapper(), new SearchBuilderConverter())))
                .setControllerAdvice(new GlobalException()).build();
    }

    private ObjectNode body() {
        return new ObjectMapper().createObjectNode().put("houseName", "Nha A")
                .put("city", "Ha Noi").put("ward", "Ba Dinh")
                .put("addressDetail", "12 Nguyen Thai Hoc");
    }

    @Test
    void createPersistsHouseWithNormalStatusAndListsIt() throws Exception {
        String response = mvc.perform(post("/houses/createHouse").contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("status", "FIRE").put("id", 99999).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.houseName").value("Nha A"))
                .andExpect(jsonPath("$.city").value("Ha Noi"))
                .andExpect(jsonPath("$.ward").value("Ba Dinh"))
                .andExpect(jsonPath("$.addressDetail").value("12 Nguyen Thai Hoc"))
                .andExpect(jsonPath("$.status").value("NORMAL"))
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(response).get("id").asLong();
        assertNotEquals(99999, id);
        repository.flush();
        assertEquals(HouseStatus.NORMAL, repository.findById(id).orElseThrow().getStatus());
        mvc.perform(get("/houses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"houseName", "city", "ward", "addressDetail"})
    void rejectsMissingBlankAndOverlongFields(String field) throws Exception {
        ObjectNode missing = body();
        missing.remove(field);
        for (ObjectNode invalid : new ObjectNode[]{missing, body().put(field, "  "),
                body().put(field, "x".repeat(field.equals("addressDetail") ? 256 : 101))}) {
            mvc.perform(post("/houses/createHouse").contentType(MediaType.APPLICATION_JSON)
                            .content(invalid.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$." + field).exists());
        }
        assertEquals(0, repository.count());
    }

    @Test
    void paginatesInAscendingIdOrder() throws Exception {
        Long firstId = null;
        Long lastId = null;
        for (int i = 0; i < 21; i++) {
            House house = new House();
            house.setHouseName("Nha " + i);
            house.setCity("Ha Noi");
            house.setWard("Ba Dinh");
            house.setAddressDetail("12 Nguyen Thai Hoc");
            lastId = repository.saveAndFlush(house).getId();
            if (i == 0) firstId = lastId;
        }
        mvc.perform(get("/houses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.content[0].id").value(firstId))
                .andExpect(jsonPath("$.totalElements").value(21));
        mvc.perform(get("/houses").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(lastId));
        mvc.perform(get("/houses").param("page", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void emptyListAndInvalidPage() throws Exception {
        mvc.perform(get("/houses")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        for (String page : new String[]{"-1", "abc"}) {
            mvc.perform(get("/houses").param("page", page)).andExpect(status().isBadRequest());
        }
    }

    private House savedHouse() {
        House house = new House();
        house.setHouseName("Nha A");
        house.setCity("Ha Noi");
        house.setWard("Ba Dinh");
        house.setAddressDetail("12 Nguyen Thai Hoc");
        return repository.saveAndFlush(house);
    }

    @Test
    void searchesCityWardAndDetailWithOr() throws Exception {
        House cityMatch = savedHouse();
        cityMatch.setCity("Da Nang");
        House wardMatch = savedHouse();
        wardMatch.setWard("Hai Chau");
        House detailMatch = savedHouse();
        detailMatch.setAddressDetail("25 Le Loi");
        savedHouse();
        repository.flush();

        mvc.perform(get("/houses/search").param("city", "  da n  ")
                        .param("ward", "HAI").param("detail", "le loi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(cityMatch.getId()))
                .andExpect(jsonPath("$.content[1].id").value(wardMatch.getId()))
                .andExpect(jsonPath("$.content[2].id").value(detailMatch.getId()));
        mvc.perform(get("/houses/search").param("detail", "LE LOI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(detailMatch.getId()));
        mvc.perform(get("/houses/search").param("city", "missing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/houses/search").param("city", " ").param("ward", "").param("detail", " "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void searchEscapesLikeWildcardsAndKeepsPagination() throws Exception {
        House literal = savedHouse();
        literal.setAddressDetail("Block 50%_!");
        savedHouse();
        repository.flush();
        mvc.perform(get("/houses/search").param("detail", "%_!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(literal.getId()));
        mvc.perform(get("/houses/search").param("city", "Ha Noi").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/houses/search").param("city", "Ha Noi").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAllFieldsAndPreservesId() throws Exception {
        long id = savedHouse().getId();
        ObjectNode request = body().put("houseName", "Nha B").put("city", "Da Nang")
                .put("ward", "Hai Chau").put("addressDetail", "25 Le Loi")
                .put("status", "FIRE").put("id", 99999);
        mvc.perform(put("/houses/updateHouse/{houseId}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.houseName").value("Nha B"))
                .andExpect(jsonPath("$.city").value("Da Nang"))
                .andExpect(jsonPath("$.ward").value("Hai Chau"))
                .andExpect(jsonPath("$.addressDetail").value("25 Le Loi"))
                .andExpect(jsonPath("$.status").value("FIRE"));
        repository.flush();
        mvc.perform(get("/houses"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FIRE"));
    }

    @Test
    void updateMissingHouseReturns404() throws Exception {
        mvc.perform(put("/houses/updateHouse/99999").contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("status", "NORMAL").toString()))
                .andExpect(status().isNotFound());
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"houseName", "city", "ward", "addressDetail", "status"})
    void rejectsInvalidUpdateWithoutChangingHouse(String field) throws Exception {
        long id = savedHouse().getId();
        ObjectNode missing = body().put("status", "FIRE");
        missing.remove(field);
        ObjectNode blank = body().put("status", "FIRE").put(field, " ");
        ObjectNode invalid = body().put("status", "FIRE")
                .put(field, "x".repeat(field.equals("addressDetail") ? 256 : 101));
        for (ObjectNode request : new ObjectNode[]{missing, blank, invalid}) {
            mvc.perform(put("/houses/updateHouse/{houseId}", id).contentType(MediaType.APPLICATION_JSON)
                            .content(request.toString()))
                    .andExpect(status().isBadRequest());
        }
        House unchanged = repository.findById(id).orElseThrow();
        assertEquals("Nha A", unchanged.getHouseName());
        assertEquals("Ha Noi", unchanged.getCity());
        assertEquals("Ba Dinh", unchanged.getWard());
        assertEquals("12 Nguyen Thai Hoc", unchanged.getAddressDetail());
        assertEquals(HouseStatus.NORMAL, unchanged.getStatus());
    }
}
