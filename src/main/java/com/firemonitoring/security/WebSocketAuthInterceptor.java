package com.firemonitoring.security;

import com.firemonitoring.converter.JwtAuthenticationConverter;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.HouseMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private static final Pattern HOUSE_TOPIC = Pattern.compile("/topic/houses/([1-9][0-9]*)/data");
    private final JwtProvider jwtProvider;
    private final JwtAuthenticationConverter converter;
    private final HouseRepository houseRepository;
    private final HouseMemberRepository houseMemberRepository;
    private final boolean securityEnabled;

    public WebSocketAuthInterceptor(JwtProvider jwtProvider, JwtAuthenticationConverter converter,
                                    HouseRepository houseRepository, HouseMemberRepository houseMemberRepository,
                                    @Value("${APP_SECURITY_ENABLED:true}") boolean securityEnabled) {
        this.jwtProvider = jwtProvider;
        this.converter = converter;
        this.houseRepository = houseRepository;
        this.houseMemberRepository = houseMemberRepository;
        this.securityEnabled = securityEnabled;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new AccessDeniedException("STOMP headers are required");
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            if (securityEnabled) {
                String authorization = accessor.getFirstNativeHeader("Authorization");
                if (authorization == null || !authorization.startsWith("Bearer ")) {
                    throw new AccessDeniedException("Bearer token is required");
                }
                accessor.setUser(converter.convert(jwtProvider.getJwtDecoder().decode(authorization.substring(7))));
            }
        } else if (command == StompCommand.SUBSCRIBE) {
            if (securityEnabled && (!(accessor.getUser() instanceof Authentication auth) || !auth.isAuthenticated())) {
                throw new AccessDeniedException("Authentication is required");
            }
            String destination = accessor.getDestination();
            var matcher = HOUSE_TOPIC.matcher(destination == null ? "" : destination);
            if (!matcher.matches()) {
                throw new AccessDeniedException("Invalid house destination");
            }
            long houseId;
            try {
                houseId = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ex) {
                throw new AccessDeniedException("Invalid house id");
            }
            if (securityEnabled) {
                if (!(accessor.getUser() instanceof JwtAuthenticationToken jwt)) {
                    throw new AccessDeniedException("JWT authentication is required");
                }
                Long userId;
                try {
                    userId = Long.valueOf(jwt.getToken().getClaimAsString("userId"));
                } catch (IllegalArgumentException ex) {
                    throw new AccessDeniedException("Invalid userId claim");
                }
                boolean admin = jwt.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                if (!admin && (userId == null
                        || !houseMemberRepository.existsByHouseIdAndUserId(houseId, userId))) {
                    throw new AccessDeniedException("You are not a member of this house");
                }
            }
            if (!houseRepository.existsById(houseId)) {
                throw new AccessDeniedException("House not found");
            }
        } else if (command != null && command != StompCommand.UNSUBSCRIBE && command != StompCommand.DISCONNECT) {
            // Sensor readings may only originate from the backend MQTT flow.
            throw new AccessDeniedException("Client publishing is not supported");
        }
        return message;
    }
}
