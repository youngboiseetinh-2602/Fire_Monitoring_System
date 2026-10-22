package com.firemonitoring.websocket;

import com.firemonitoring.model.response.DataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HouseDataPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(long houseId, DataResponse data) {
        messagingTemplate.convertAndSend("/topic/houses/" + houseId + "/data", data);
    }
}
