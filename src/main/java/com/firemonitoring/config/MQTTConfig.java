package com.firemonitoring.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import com.firemonitoring.mqtt.MqttDataListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;

@Configuration
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MQTTConfig {
    @Bean
    public DirectChannel mqttCommandChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow mqttCommandFlow(MqttPahoClientFactory mqttClientFactory,
                                           @Value("${mqtt.client-id}") String clientId) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId + "-commands", mqttClientFactory);
        handler.setAsync(false);
        handler.setDefaultQos(1);
        handler.setDefaultRetained(false);
        return IntegrationFlow.from(mqttCommandChannel()).handle(handler).get();
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(
            @Value("${mqtt.broker-url}") String brokerUrl,
            @Value("${mqtt.username:}") String username,
            @Value("${mqtt.password:}") String password,
            @Value("${mqtt.connection-timeout-seconds:10}") int connectionTimeout,
            @Value("${mqtt.keep-alive-seconds:60}") int keepAlive) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(connectionTimeout);
        options.setKeepAliveInterval(keepAlive);
        if (!username.isBlank()) {
            options.setUserName(username);
        }
        if (!password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory mqttClientFactory,
            @Value("${mqtt.client-id}") String clientId,
            @Value("${mqtt.topic}") String topic,
            @Value("${mqtt.qos:1}") int qos) {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory, topic);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        return adapter;
    }

    @Bean
    public IntegrationFlow mqttInboundFlow(MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter,
                                           MqttDataListener mqttDataListener) {
        return IntegrationFlow.from(mqttInboundAdapter)
                .handle(message -> mqttDataListener.handle(message))
                .get();
    }
}
