package com.firemonitoring.security;

import com.firemonitoring.converter.JwtAuthenticationConverter;
import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.HouseMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorTest {
    private final JwtProvider provider = new JwtProvider("test-secret-with-at-least-32-bytes", "test", 60);
    private final HouseRepository houses = mock(HouseRepository.class);
    private final HouseMemberRepository members = mock(HouseMemberRepository.class);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(
            provider, new JwtAuthenticationConverter(), houses, members, true);

    private StompHeaderAccessor authenticated() {
        return authenticated(UserRole.MEMBER);
    }

    private StompHeaderAccessor authenticated(UserRole role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("viewer");
        user.setRole(role);
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization", "Bearer " + provider.generateToken(user));
        send(connect);
        return connect;
    }

    private void send(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), null);
    }

    @Test
    void authenticatesConnectAndAllowsExistingHouse() {
        var connect = authenticated();
        assertEquals("viewer", connect.getUser().getName());
        when(houses.existsById(1L)).thenReturn(true);
        when(members.existsByHouseIdAndUserId(1L, 1L)).thenReturn(true);
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(connect.getUser());
        subscribe.setDestination("/topic/houses/1/data");
        assertDoesNotThrow(() -> send(subscribe));
    }

    @Test
    void rejectsNonMemberOfExistingHouse() {
        when(houses.existsById(1L)).thenReturn(true);
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(authenticated().getUser());
        subscribe.setDestination("/topic/houses/1/data");
        assertThrows(AccessDeniedException.class, () -> send(subscribe));
        verify(members).existsByHouseIdAndUserId(1L, 1L);
    }

    @Test
    void adminCanSubscribeWithoutMembershipButHouseMustExist() {
        when(houses.existsById(1L)).thenReturn(true);
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(authenticated(UserRole.ADMIN).getUser());
        subscribe.setDestination("/topic/houses/1/data");
        assertDoesNotThrow(() -> send(subscribe));
        subscribe.setDestination("/topic/houses/999/data");
        assertThrows(AccessDeniedException.class, () -> send(subscribe));
        verifyNoInteractions(members);
    }

    @Test
    void rejectsMissingAndInvalidToken() {
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(AccessDeniedException.class, () -> send(connect));
        connect.setNativeHeader("Authorization", "Bearer invalid");
        assertThrows(JwtException.class, () -> send(connect));
    }

    @Test
    void rejectsUnauthenticatedSubscription() {
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setDestination("/topic/houses/1/data");
        assertThrows(AccessDeniedException.class, () -> send(subscribe));
        verifyNoInteractions(houses);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/houses/*/data", "/topic/houses/0/data", "/topic/other",
            "/topic/houses/9223372036854775808/data", "/topic/houses/999/data"})
    void rejectsInvalidOrMissingHouse(String destination) {
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setUser(authenticated().getUser());
        subscribe.setDestination(destination);
        assertThrows(AccessDeniedException.class, () -> send(subscribe));
    }

    @Test
    void rejectsClientPublishingEvenWhenAuthenticated() {
        var publish = StompHeaderAccessor.create(StompCommand.SEND);
        publish.setUser(authenticated().getUser());
        publish.setDestination("/topic/houses/1/data");
        assertThrows(AccessDeniedException.class, () -> send(publish));
    }
}
