package com.firemonitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.firemonitoring.entity.House;
import com.firemonitoring.entity.Location;
import com.firemonitoring.entity.Kit;
import com.firemonitoring.repository.LocationRepository;
import com.firemonitoring.repository.KitRepository;
import com.firemonitoring.mqtt.MqttCommandPublisher;
import com.firemonitoring.config.ModelMapperConfig;
import com.firemonitoring.exception.ServiceUnavailableException;
import com.firemonitoring.websocket.HouseDataPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.firemonitoring.mqtt.MqttDataListener;
import com.firemonitoring.service.FirePredictionService;
import com.firemonitoring.model.response.PredictionResult;
import com.firemonitoring.enums.EventType;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.enums.HouseStatus;
import com.firemonitoring.exception.GlobalException;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.HouseMemberRepository;
import com.firemonitoring.entity.HouseMember;
import java.util.List;
import com.firemonitoring.security.CurrentUserProvider;
import com.firemonitoring.service.impl.HouseServiceImpl;
import com.firemonitoring.service.impl.KitServiceImpl;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercises controller, validation, mapping and persistence; security filters are excluded.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class HouseIntegrationTest {
    @org.junit.jupiter.api.AfterEach
    void clearKitAuthentication() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private void authenticateKitAdmin() {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void commandApiResolvesDatabaseIdToDeviceUid() throws Exception {
        House house = saveDataHouse("Command house");
        Kit kit = saveDataKit(saveDataLocation(house, "Room"), "ESP32-ABC");
        mvc.perform(put("/kits/{kitId}/command", kit.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buzz\":true,\"pump\":false,\"led\":true}"))
                .andExpect(status().isAccepted());
        verify(commandPublisher).sendCommand(eq("ESP32-ABC"), any());
    }

    @Test
    void numericDatabaseIdIsNotUsedAsTelemetryFallback() {
        House house = saveDataHouse("No fallback");
        Kit kit = saveDataKit(saveDataLocation(house, "Room"), "ESP32-NON-NUMERIC");
        sendReading(kit.getId().toString(), 123);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void rejectsTopicWildcardsInDeviceUid() throws Exception {
        authenticateKitAdmin();
        House house = saveDataHouse("Invalid device");
        long before = locationRepository.count();
        for (String uid : List.of("a/b", "a+b", "a#b", "a b")) {
            mvc.perform(post("/houses/{houseId}/kits", house.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"location\":\"Room\",\"deviceUid\":\"" + uid + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(before, locationRepository.count());
    }

    @Test
    void addsKitAndNewLocationToRequestedHouse() throws Exception {
        authenticateKitAdmin();
        House house = saveDataHouse("New kit house");
        mvc.perform(post("/houses/{houseId}/kits", house.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\" Kitchen \",\"deviceUid\":\" KIT-001 \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("deviceUid").value("KIT-001"))
                .andExpect(jsonPath("location").value("Kitchen"))
                .andExpect(jsonPath("status").value("OFFLINE"));
        Kit saved = kitRepository.findByDeviceUid("KIT-001").orElseThrow();
        assertEquals(house.getId(), saved.getLocation().getHouse().getId());
        assertEquals("Kitchen", saved.getLocation().getLocationName());
        verifyNoInteractions(commandPublisher);
    }

    @Test
    void rejectsDuplicateDeviceOrLocationWithoutAddingRows() throws Exception {
        authenticateKitAdmin();
        House house = saveDataHouse("Duplicate kit house");
        saveDataKit(saveDataLocation(house, "Kitchen"), "existing-device");
        long locationsBefore = locationRepository.count();
        long kitsBefore = kitRepository.count();
        for (String body : List.of(
                "{\"location\":\"Bedroom\",\"deviceUid\":\"existing-device\"}",
                "{\"location\":\"Kitchen\",\"deviceUid\":\"new-device\"}")) {
            mvc.perform(post("/houses/{houseId}/kits", house.getId())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isConflict());
        }
        assertEquals(locationsBefore, locationRepository.count());
        assertEquals(kitsBefore, kitRepository.count());
    }

    @Test
    void rejectsInvalidKitInputAndMissingHouse() throws Exception {
        authenticateKitAdmin();
        for (String body : List.of("{}", "{\"location\":\" \",\"deviceUid\":\"kit\"}",
                "{\"location\":\"Room\",\"deviceUid\":\"" + "x".repeat(101) + "\"}")) {
            mvc.perform(post("/houses/1/kits").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/houses/{houseId}/kits", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Room\",\"deviceUid\":\"kit\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/houses/0/kits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Room\",\"deviceUid\":\"kit\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNonAdminAddingKit() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "member", "unused", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER"))));
        long before = locationRepository.count();
        mvc.perform(post("/houses/1/kits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Room\",\"deviceUid\":\"kit\"}"))
                .andExpect(status().isForbidden());
        assertEquals(before, locationRepository.count());
    }
    @Autowired
    private HouseRepository repository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private KitRepository kitRepository;
    private MqttCommandPublisher commandPublisher;
    private HouseDataPublisher houseDataPublisher;
    private SimpMessagingTemplate messagingTemplate;
    private KitServiceImpl kitService;
    private MockMvc mvc;
    private HouseMemberRepository members;

    @BeforeEach
    void setup() {
        commandPublisher = mock(MqttCommandPublisher.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        houseDataPublisher = new HouseDataPublisher(messagingTemplate);
        members = mock(HouseMemberRepository.class);
        CurrentUserProvider currentUser = () -> 7L;
        FirePredictionService predictionService = data -> new PredictionResult(EventType.NORMAL, 0, 0.95);
        kitService = new KitServiceImpl(repository, locationRepository, kitRepository, houseDataPublisher,
                commandPublisher, new ModelMapperConfig().modelMapper(), members, currentUser, predictionService);
        mvc = MockMvcBuilders.standaloneSetup(new HouseController(
                        new HouseServiceImpl(repository, new ModelMapper(), new SearchBuilderConverter(),
                                members, mock(com.firemonitoring.repository.UserRepository.class), currentUser)),
                        new KitController(kitService))
                .setControllerAdvice(new GlobalException()).build();
    }

    @Test
    void publishesMqttReadingsToTheMatchingHouseDestination() {
        House house = saveDataHouse("Stream house");
        Kit kit = saveDataKit(saveDataLocation(house, "Kitchen"), "stream-device");
        sendReading(kit.getDeviceUid(), 432);
        verify(messagingTemplate).convertAndSend(eq("/topic/houses/" + house.getId() + "/data"),
                org.mockito.ArgumentMatchers.<Object>argThat(value -> value instanceof com.firemonitoring.model.response.DataResponse data
                        && data.getGas() == 432 && data.getTemperature() == 35.5f && data.getHumidity() == 70.2f && data.isFlame() && data.getStatus() == com.firemonitoring.enums.EventType.NORMAL && data.getLocation().equals("Kitchen")));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void adminRequestsOnlySelectedHouseWithoutMembership() throws Exception {
        House house = saveDataHouse("Admin selected house");
        House other = saveDataHouse("Other house");
        Kit kit = saveDataKit(saveDataLocation(house, "Kitchen"), "admin-kit");
        saveDataKit(saveDataLocation(other, "Kitchen"), "other-admin-kit");
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            mvc.perform(post("/house/{houseId}/kit", house.getId()))
                    .andExpect(status().isAccepted())
                    .andExpect(content().json("[" + kit.getId() + "]"));
            mvc.perform(post("/house/999999/kit")).andExpect(status().isNotFound());
            verify(commandPublisher).startTelemetry(kit.getDeviceUid());
            verifyNoMoreInteractions(commandPublisher);
            verifyNoInteractions(members);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_MEMBER", "ROLE_OWNER", "NONE"})
    void rejectsNonAdminsBeforeRequestingTelemetry(String role) throws Exception {
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        if (!role.equals("NONE")) {
            context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "viewer", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))));
        }
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            mvc.perform(post("/house/1/kit")).andExpect(status().isForbidden());
            verifyNoInteractions(commandPublisher, members);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void ignoresUnknownKit() {
        sendReading("unknown-device", 123);
        verifyNoInteractions(messagingTemplate);
    }
    private ObjectNode body() {
        return new ObjectMapper().createObjectNode().put("houseName", "Nha A")
                .put("city", "Ha Noi").put("ward", "Ba Dinh")
                .put("addressDetail", "12 Nguyen Thai Hoc");
    }

    @Test
    void requestsTelemetryOnlyFromKitsOfRequestedHouse() throws Exception {
        House house = saveDataHouse("Requested house");
        House other = saveDataHouse("Other house");
        Kit kitchen = saveDataKit(saveDataLocation(house, "Kitchen"), "kitchen-kit");
        Kit bedroom = saveDataKit(saveDataLocation(house, "Bedroom"), "bedroom-kit");
        saveDataLocation(house, "Empty location");
        saveDataKit(saveDataLocation(other, "Other room"), "other-kit");

        belongsTo(house);
        mvc.perform(post("/me/house/kit").param("houseId", other.getId().toString()).param("userId", "99"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(
                        kitchen.getId().intValue(), bedroom.getId().intValue())));
        verify(commandPublisher).startTelemetry(kitchen.getDeviceUid());
        verify(commandPublisher).startTelemetry(bedroom.getDeviceUid());
        verifyNoMoreInteractions(commandPublisher);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void returnsNoKitIdsForEmptyHouseAndNotFoundForMissingHouse() throws Exception {
        House house = saveDataHouse("Empty house");
        belongsTo(house);
        mvc.perform(post("/me/house/kit"))
                .andExpect(status().isAccepted()).andExpect(content().json("[]"));
        mvc.perform(post("/me/house")).andExpect(status().isNotFound());
        mvc.perform(post("/houses/{houseId}/data", house.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/me/house/kit")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(commandPublisher);
    }

    @Test
    void returnsUnavailableWhenMqttCommandFails() throws Exception {
        House house = saveDataHouse("Unavailable house");
        Kit kit = saveDataKit(saveDataLocation(house, "Kitchen"), "unavailable-kit");
        doThrow(new ServiceUnavailableException("MQTT unavailable"))
                .when(commandPublisher).startTelemetry(kit.getDeviceUid());
        belongsTo(house);
        mvc.perform(post("/me/house/kit"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rejectsNonMemberBeforeSendingCommandsEvenWithSpoofedUserId() throws Exception {
        House house = saveDataHouse("Private house");
        saveDataKit(saveDataLocation(house, "Kitchen"), "private-kit");
        mvc.perform(post("/me/house/kit").param("houseId", house.getId().toString()).param("userId", "99"))
                .andExpect(status().isNotFound());
        verify(members).findByUserId(7L);
        verifyNoInteractions(commandPublisher);
    }

    private void belongsTo(House... houses) {
        List<HouseMember> memberships = java.util.Arrays.stream(houses).map(house -> {
            HouseMember member = new HouseMember();
            member.setHouse(house);
            return member;
        }).toList();
        when(members.findByUserId(7L)).thenReturn(memberships);
    }

    @Test
    void requestsAllAssociatedHousesWithoutDuplicateCommands() throws Exception {
        House first = saveDataHouse("First house");
        House second = saveDataHouse("Second house");
        Kit firstKit = saveDataKit(saveDataLocation(first, "Kitchen"), "first-kit");
        Kit secondKit = saveDataKit(saveDataLocation(second, "Kitchen"), "second-kit");
        belongsTo(first, second, first);
        mvc.perform(post("/me/house/kit"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder(
                        firstKit.getId().intValue(), secondKit.getId().intValue())));
        verify(commandPublisher).startTelemetry(firstKit.getDeviceUid());
        verify(commandPublisher).startTelemetry(secondKit.getDeviceUid());
        verifyNoMoreInteractions(commandPublisher);
    }
    private House saveDataHouse(String name) {
        House house = new House();
        house.setHouseName(name);
        house.setCity("Ha Noi");
        house.setWard("Ba Dinh");
        house.setAddressDetail("12 Nguyen Thai Hoc");
        return repository.saveAndFlush(house);
    }

    private Location saveDataLocation(House house, String name) {
        Location location = new Location();
        location.setHouse(house);
        location.setLocationName(name);
        return locationRepository.saveAndFlush(location);
    }

    private Kit saveDataKit(Location location, String uid) {
        Kit kit = new Kit();
        kit.setLocation(location);
        kit.setDeviceUid(uid);
        return kitRepository.saveAndFlush(kit);
    }

    private void sendReading(String deviceUid, int gas) {
        new MqttDataListener(new ObjectMapper(), kitService).handle(MessageBuilder
                .withPayload("{\"temperature\":35.5,\"humidity\":70.2,\"gas\":" + gas + ",\"flame\":true}")
                .setHeader(MqttHeaders.RECEIVED_TOPIC, "fire-monitor/kits/" + deviceUid + "/telemetry")
                .build());
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
