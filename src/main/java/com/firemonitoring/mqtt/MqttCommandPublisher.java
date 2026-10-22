package com.firemonitoring.mqtt;

import com.firemonitoring.exception.ServiceUnavailableException;
import com.firemonitoring.model.request.kitCommand;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class MqttCommandPublisher {
    private final ObjectProvider<MessageChannel> commandChannel;

    public MqttCommandPublisher(@Qualifier("mqttCommandChannel") ObjectProvider<MessageChannel> commandChannel) {
        this.commandChannel = commandChannel;
    }

    public void startTelemetry(String deviceUid) {
        publish(deviceUid, "{\"command\":\"START_TELEMETRY\"}");
    }

    public void sendCommand(String deviceUid, kitCommand command) {
        String payload = String.format(
                "{\"buzz\":%s,\"pump\":%s,\"led\":%s}",
                command.isBuzz(), command.isPump(), command.isLed());
        publish(deviceUid, payload);
    }

    private void publish(String deviceUid, String payload) {
        if (deviceUid == null || !deviceUid.matches("[^/+#\\s\\x00]{1,100}")) {
            throw new IllegalArgumentException("Invalid MQTT device UID");
        }
        MessageChannel channel = commandChannel.getIfAvailable();
        if (channel == null) {
            throw new ServiceUnavailableException("MQTT is disabled; cannot communicate with kit");
        }
        try {
            boolean sent = channel.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, "fire-monitor/kits/" + deviceUid + "/commands")
                    .setHeader(MqttHeaders.QOS, 1)
                    .setHeader(MqttHeaders.RETAINED, false)
                    .build());
            if (!sent) {
                throw new ServiceUnavailableException("MQTT command was not accepted for kit " + deviceUid);
            }
        } catch (MessagingException ex) {
            throw new ServiceUnavailableException("Cannot send MQTT command for kit " + deviceUid, ex);
        }
    }
}
