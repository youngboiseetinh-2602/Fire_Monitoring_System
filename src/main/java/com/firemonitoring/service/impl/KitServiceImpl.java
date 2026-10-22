package com.firemonitoring.service.impl;

import com.firemonitoring.entity.Location;
import com.firemonitoring.repository.HouseMemberRepository;
import com.firemonitoring.security.CurrentUserProvider;
import com.firemonitoring.enums.EventType;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.response.DataResponse;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.KitRepository;
import com.firemonitoring.repository.LocationRepository;
import com.firemonitoring.mqtt.MqttCommandPublisher;
import org.modelmapper.ModelMapper;
import com.firemonitoring.service.KitService;
import com.firemonitoring.websocket.HouseDataPublisher;
import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.request.kitCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KitServiceImpl implements KitService {
    private final HouseRepository houseRepository;
    private final LocationRepository locationRepository;
    private final KitRepository kitRepository;
    private final HouseDataPublisher houseDataPublisher;
    private final MqttCommandPublisher commandPublisher;
    private final ModelMapper modelMapper;
    private final HouseMemberRepository houseMemberRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public void updateData(long kitId, DataRequest data) {
        kitRepository.findById(kitId).ifPresent(kit -> {
            Location location = kit.getLocation();
            houseDataPublisher.publish(location.getHouse().getId(), toResponse(location, data));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyHouseKits() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<Long> houseIds = houseMemberRepository.findByUserId(userId).stream()
                .map(member -> member.getHouse().getId())
                .distinct()
                .toList();
        if (houseIds.isEmpty()) {
            throw new DataNotFoundException("You do not belong to any house");
        }
        List<Long> kitIds = new ArrayList<>();
        for (Long houseId : houseIds) {
            kitIds.addAll(getData(houseId));
        }
        return kitIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getHouseKit(long houseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Only admins can request kits by house ID");
        }
        return getData(houseId);
    }

    private List<Long> getData(long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw new DataNotFoundException("House not found");
        }
        List<Long> kitIds = new ArrayList<>();
        for (Location location : locationRepository.findByHouseId(houseId)) {
            kitRepository.findByLocationId(location.getId())
                    .ifPresent(kit -> kitIds.add(kit.getId()));
        }
        for (Long kitId : kitIds) {
            commandPublisher.startTelemetry(kitId);
        }
        return kitIds;
    }

    @Override
    @Transactional(readOnly = true)
    public String sendCommand(long kitId, kitCommand command) {
        if (!kitRepository.existsById(kitId)) {
            throw new DataNotFoundException("Kit not found");
        }
        commandPublisher.sendCommand(kitId, command);
        return "Command sent successfully";
    }

    private DataResponse toResponse(Location location, DataRequest data) {
        DataResponse response = modelMapper.map(data, DataResponse.class);
        response.setLocation(location.getLocationName());
        // Placeholder status until detection rules are implemented.
        response.setStatus(EventType.NORMAL);
        return response;
    }
}
