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

    public void startTelemetry(long kitId) {
        publish(kitId, "{\"command\":\"START_TELEMETRY\"}");
    }

    public void sendCommand(long kitId, kitCommand command) {
        String payload = String.format(
                "{\"buzz\":%s,\"pump\":%s,\"led\":%s}",
                command.isBuzz(), command.isPump(), command.isLed());
        publish(kitId, payload);
    }

    private void publish(long kitId, String payload) {
        MessageChannel channel = commandChannel.getIfAvailable();
        if (channel == null) {
            throw new ServiceUnavailableException("MQTT is disabled; cannot communicate with kit");
        }
        try {
            boolean sent = channel.send(MessageBuilder.withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, "fire-monitor/kits/" + kitId + "/commands")
                    .setHeader(MqttHeaders.QOS, 1)
                    .setHeader(MqttHeaders.RETAINED, false)
                    .build());
            if (!sent) {
                throw new ServiceUnavailableException("MQTT command was not accepted for kit " + kitId);
            }
        } catch (MessagingException ex) {
            throw new ServiceUnavailableException("Cannot send MQTT command for kit " + kitId, ex);
        }
    }
}
