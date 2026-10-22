package com.firemonitoring.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firemonitoring.service.KitService;
import com.firemonitoring.model.request.DataRequest;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttDataListenerTest {
    private static final String TOPIC = "fire-monitor/kits/ESP32-001/telemetry";
    private static final String PAYLOAD =
            "{\"temperature\":35.5,\"humidity\":70.2,\"gas\":620,\"flame\":false}";
    private final ObjectMapper mapper = new ObjectMapper();
    private final KitService kitService = mock(KitService.class);
    private final MqttDataListener listener = new MqttDataListener(mapper, kitService);

    @Test
    void forwardsOnlyValidReadingsToService() {
        listener.handle(MessageBuilder.withPayload(PAYLOAD).setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
        verify(kitService).updateData("ESP32-001", new DataRequest(35.5f, 70.2f, 620, false));
        listener.handle(MessageBuilder.withPayload(PAYLOAD.replace("620", "700"))
                .setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
        listener.handle(MessageBuilder.withPayload("{}").setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
        verify(kitService).updateData("ESP32-001", new DataRequest(35.5f, 70.2f, 700, false));
        verifyNoMoreInteractions(kitService);
    }

    @Test
    void readsKitIdAndSensorValues() {
        var telemetry = listener.read(TOPIC, PAYLOAD).orElseThrow();
        assertEquals("ESP32-001", telemetry.deviceUid());
        assertEquals(35.5f, telemetry.data().temperature());
        assertEquals(70.2f, telemetry.data().humidity());
        assertEquals(620, telemetry.data().gas());
        assertFalse(telemetry.data().flame());
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "humidity", "gas", "flame"})
    void rejectsMissingOrNullFields(String field) throws Exception {
        ObjectNode json = (ObjectNode) mapper.readTree(PAYLOAD);
        json.remove(field);
        assertTrue(listener.read(TOPIC, json.toString()).isEmpty());
        json.putNull(field);
        assertTrue(listener.read(TOPIC, json.toString()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "fire-monitor/kits//telemetry", "fire-monitor/kits/+/telemetry",
            "fire-monitor/kits/#/telemetry", "fire-monitor/kits/bad uid/telemetry",
            "fire-monitor/kits/1/telemetry/extra", "other/kits/1/telemetry"
    })
    void rejectsInvalidTopics(String topic) {
        assertTrue(listener.read(topic, PAYLOAD).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{", "[]", "null", "{}",
            "{\"temperature\":\"35.5\",\"humidity\":70,\"gas\":620,\"flame\":false}",
            "{\"temperature\":1e100,\"humidity\":70,\"gas\":620,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":-1,\"gas\":620,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":101,\"gas\":620,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":70,\"gas\":-1,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":70,\"gas\":1.5,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":70,\"gas\":2147483648,\"flame\":false}",
            "{\"temperature\":35,\"humidity\":70,\"gas\":620,\"flame\":\"false\"}",
            "{\"temperature\":35,\"humidity\":70,\"gas\":620,\"flame\":0}"
    })
    void rejectsMalformedOrInvalidSensorData(String payload) {
        assertTrue(listener.read(TOPIC, payload).isEmpty());
    }

    @Test
    void rejectsTrailingJson() {
        assertTrue(listener.read(TOPIC, PAYLOAD + " {}").isEmpty());
    }

    @Test
    void acceptsBoundaryValuesAndNegativeTemperature() {
        for (int humidity : new int[]{0, 100}) {
            var telemetry = listener.read(TOPIC,
                    "{\"temperature\":-10,\"humidity\":" + humidity + ",\"gas\":0,\"flame\":true}")
                    .orElseThrow();
            assertEquals(-10f, telemetry.data().temperature());
            assertEquals(humidity, telemetry.data().humidity());
            assertTrue(telemetry.data().flame());
        }
    }

    @Test
    void invalidMessageDoesNotPreventNextValidMessage() {
        assertDoesNotThrow(() -> {
            listener.handle(MessageBuilder.withPayload("{").setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
            listener.handle(MessageBuilder.withPayload(PAYLOAD).build());
            listener.handle(MessageBuilder.withPayload(new byte[]{1}).setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
            listener.handle(MessageBuilder.withPayload(PAYLOAD).setHeader(MqttHeaders.RECEIVED_TOPIC, TOPIC).build());
        });
        assertTrue(listener.read(TOPIC, PAYLOAD).isPresent());
    }
}
