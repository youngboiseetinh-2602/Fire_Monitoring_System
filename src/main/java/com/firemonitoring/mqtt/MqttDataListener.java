package com.firemonitoring.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.service.KitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MqttDataListener {
    private static final Logger log = LoggerFactory.getLogger(MqttDataListener.class);
    private static final Pattern TELEMETRY_TOPIC =
            Pattern.compile("fire-monitor/kits/([1-9][0-9]*)/telemetry");

    private final ObjectReader jsonReader;
    private final KitService kitService;

    public MqttDataListener(ObjectMapper objectMapper, KitService kitService) {
        this.kitService = kitService;
        this.jsonReader = objectMapper.reader()
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    public void handle(Message<?> message) {
        Object topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        if (!(topic instanceof String topicText) || !(message.getPayload() instanceof String payload)) {
            log.warn("Ignoring MQTT telemetry: expected a topic and a text payload");
            return;
        }

        read(topicText, payload).ifPresent(telemetry -> {
            kitService.updateData(telemetry.kitId(), telemetry.data());
            log.debug("Validated MQTT telemetry for kit {}", telemetry.kitId());
        });
    }

    public Optional<Telemetry> read(String topic, String payload) {
        try {
            Matcher matcher = TELEMETRY_TOPIC.matcher(topic == null ? "" : topic);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid telemetry topic");
            }
            long kitId = Long.parseLong(matcher.group(1));
            JsonNode json = jsonReader.readTree(payload);
            if (json == null || !json.isObject()) {
                throw new IllegalArgumentException("Payload must be a JSON object");
            }

            float temperature = requiredFloat(json, "temperature");
            float humidity = requiredFloat(json, "humidity");
            if (humidity < 0 || humidity > 100) {
                throw new IllegalArgumentException("Humidity must be between 0 and 100");
            }
            JsonNode gas = json.get("gas");
            if (gas == null || !gas.isIntegralNumber() || !gas.canConvertToInt() || gas.intValue() < 0) {
                throw new IllegalArgumentException("Gas must be a non-negative integer within int range");
            }
            JsonNode flame = json.get("flame");
            if (flame == null || !flame.isBoolean()) {
                throw new IllegalArgumentException("Flame must be a boolean");
            }

            DataRequest data = new DataRequest(temperature, humidity, gas.intValue(), flame.booleanValue());
            return Optional.of(new Telemetry(kitId, data));
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            // Do not log raw payloads or parser messages containing untrusted input.
            log.warn("Ignoring invalid MQTT telemetry: malformed topic or sensor data");
            return Optional.empty();
        }
    }

    private float requiredFloat(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isNumber() || !Float.isFinite(value.floatValue())) {
            throw new IllegalArgumentException(field + " must be a finite number within float range");
        }
        return value.floatValue();
    }

    public record Telemetry(long kitId, DataRequest data) {}
}
