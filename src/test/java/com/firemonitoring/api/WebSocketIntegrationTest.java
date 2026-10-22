package com.firemonitoring.api;

import com.firemonitoring.config.WebSocketConfig;
import com.firemonitoring.converter.JwtAuthenticationConverter;
import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.model.response.DataResponse;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.HouseMemberRepository;
import com.firemonitoring.security.JwtProvider;
import com.firemonitoring.security.WebSocketAuthInterceptor;
import com.firemonitoring.websocket.HouseDataPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = WebSocketIntegrationTest.App.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"APP_SECURITY_ENABLED=true", "app.cors.allowed-origins=http://localhost:5173"})
@ActiveProfiles("websocket-test")
class WebSocketIntegrationTest {
    @Configuration
    @Profile("websocket-test")
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, SecurityAutoConfiguration.class})
    @Import({WebSocketConfig.class, WebSocketAuthInterceptor.class, JwtAuthenticationConverter.class, HouseDataPublisher.class})
    static class App {
        @Bean
        HouseMemberRepository houseMemberRepository() {
            HouseMemberRepository members = mock(HouseMemberRepository.class);
            when(members.existsByHouseIdAndUserId(1L, 1L)).thenReturn(true);
            when(members.existsByHouseIdAndUserId(2L, 1L)).thenReturn(true);
            return members;
        }

        @Bean
        JwtProvider jwtProvider() {
            return new JwtProvider("test-secret-with-at-least-32-bytes", "test", 60);
        }

        @Bean
        HouseRepository houseRepository() {
            HouseRepository repository = mock(HouseRepository.class);
            when(repository.existsById(1L)).thenReturn(true);
            when(repository.existsById(2L)).thenReturn(true);
            return repository;
        }
    }

    @LocalServerPort
    int port;
    @Autowired
    JwtProvider jwtProvider;
    @Autowired
    HouseDataPublisher publisher;

    @Test
    void deliversJsonOverAuthenticatedWebSocketToMatchingSubscribers() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("viewer");
        user.setRole(UserRole.MEMBER);
        StompHeaders headers = new StompHeaders();
        headers.set("Authorization", "Bearer " + jwtProvider.generateToken(user));
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        client.start();
        StompSession session = null;
        try {
            session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                    headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            var first = new LinkedBlockingQueue<DataResponse>();
            var second = new LinkedBlockingQueue<DataResponse>();
            var other = new LinkedBlockingQueue<DataResponse>();
            session.subscribe("/topic/houses/1/data", handler(first));
            session.subscribe("/topic/houses/1/data", handler(second));
            session.subscribe("/topic/houses/2/data", handler(other));
            DataResponse reading = new DataResponse();
            reading.setLocation("Kitchen");
            reading.setGas(432);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                publisher.publish(1L, reading);
                DataResponse firstReading = first.poll(200, TimeUnit.MILLISECONDS);
                DataResponse secondReading = second.poll(200, TimeUnit.MILLISECONDS);
                assertNotNull(firstReading);
                assertNotNull(secondReading);
                assertEquals(432, firstReading.getGas());
                assertEquals("Kitchen", secondReading.getLocation());
            });
            assertNull(other.poll(200, TimeUnit.MILLISECONDS));
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
            client.stop();
        }
    }

    private StompFrameHandler handler(LinkedBlockingQueue<DataResponse> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return DataResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((DataResponse) payload);
            }
        };
    }
}
