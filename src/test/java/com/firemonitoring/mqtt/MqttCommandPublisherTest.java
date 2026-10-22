package com.firemonitoring.mqtt;

import com.firemonitoring.exception.ServiceUnavailableException;
import com.firemonitoring.model.request.kitCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttCommandPublisherTest {
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();

    @Test
    void publishesNonRetainedStartCommandToSelectedKit() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any())).thenReturn(true);
        beans.registerSingleton("mqttCommandChannel", channel);
        new MqttCommandPublisher(beans.getBeanProvider(MessageChannel.class)).startTelemetry("ESP32-012");
        verify(channel).send(argThat(message ->
                "{\"command\":\"START_TELEMETRY\"}".equals(message.getPayload())
                        && "fire-monitor/kits/ESP32-012/commands".equals(message.getHeaders().get(MqttHeaders.TOPIC))
                        && Integer.valueOf(1).equals(message.getHeaders().get(MqttHeaders.QOS))
                        && Boolean.FALSE.equals(message.getHeaders().get(MqttHeaders.RETAINED))));
    }

    @Test
    void publishesControlCommandToSelectedKit() {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any())).thenReturn(true);
        beans.registerSingleton("mqttCommandChannel", channel);
        kitCommand command = new kitCommand();
        command.setBuzz(true);
        command.setPump(false);
        command.setLed(true);

        new MqttCommandPublisher(beans.getBeanProvider(MessageChannel.class)).sendCommand("ESP32-007", command);

        verify(channel).send(argThat(message ->
                "{\"buzz\":true,\"pump\":false,\"led\":true}".equals(message.getPayload())
                        && "fire-monitor/kits/ESP32-007/commands".equals(message.getHeaders().get(MqttHeaders.TOPIC))
                        && Integer.valueOf(1).equals(message.getHeaders().get(MqttHeaders.QOS))
                        && Boolean.FALSE.equals(message.getHeaders().get(MqttHeaders.RETAINED))));
    }

    @Test
    void failsExplicitlyWhenMqttDisabled() {
        var publisher = new MqttCommandPublisher(beans.getBeanProvider(MessageChannel.class));
        assertThrows(ServiceUnavailableException.class, () -> publisher.startTelemetry("ESP32-001"));
    }

    @Test
    void reportsBothRejectedAndFailedSends() {
        MessageChannel channel = mock(MessageChannel.class);
        beans.registerSingleton("mqttCommandChannel", channel);
        var publisher = new MqttCommandPublisher(beans.getBeanProvider(MessageChannel.class));
        when(channel.send(any())).thenReturn(false);
        assertThrows(ServiceUnavailableException.class, () -> publisher.startTelemetry("ESP32-001"));
        when(channel.send(any())).thenThrow(new MessageDeliveryException("Broker disconnected"));
        assertThrows(ServiceUnavailableException.class, () -> publisher.startTelemetry("ESP32-001"));
    }
}
