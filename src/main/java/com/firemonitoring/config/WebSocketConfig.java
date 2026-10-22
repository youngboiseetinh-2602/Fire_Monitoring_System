package com.firemonitoring.config;

import com.firemonitoring.security.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import jakarta.annotation.PreDestroy;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor authInterceptor;
    private final String[] allowedOrigins;
    private final ThreadPoolTaskScheduler heartbeatScheduler;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                           @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = allowedOrigins;
        heartbeatScheduler = new ThreadPoolTaskScheduler();
        heartbeatScheduler.setThreadNamePrefix("websocket-heartbeat-");
        heartbeatScheduler.initialize();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setTaskScheduler(heartbeatScheduler)
                .setHeartbeatValue(new long[]{10000, 10000});
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @PreDestroy
    public void stopScheduler() {
        heartbeatScheduler.shutdown();
    }
}
