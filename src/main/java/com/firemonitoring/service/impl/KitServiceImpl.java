package com.firemonitoring.service.impl;

import com.firemonitoring.entity.Location;
import com.firemonitoring.entity.Kit;
import com.firemonitoring.exception.ConflictException;
import com.firemonitoring.model.request.AddKitRequest;
import com.firemonitoring.repository.HouseMemberRepository;
import com.firemonitoring.security.CurrentUserProvider;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.response.DataResponse;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.repository.KitRepository;
import com.firemonitoring.repository.LocationRepository;
import com.firemonitoring.mqtt.MqttCommandPublisher;
import org.modelmapper.ModelMapper;
import com.firemonitoring.service.KitService;
import com.firemonitoring.service.FirePredictionService;
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
    private final FirePredictionService firePredictionService;

    @Override
    @Transactional
    public String addKit(long houseId, AddKitRequest request) {
        if (houseId <= 0 || request == null || request.location() == null
                || request.deviceUid() == null || request.location().isBlank()
                || request.deviceUid().isBlank() || request.location().length() > 100
                || request.deviceUid().length() > 100) {
            throw new IllegalArgumentException("Provide a positive house ID, location and device UID (max 100 characters)");
        }
        var house = houseRepository.findById(houseId)
                .orElseThrow(() -> new DataNotFoundException("House not found"));
        String locationName = request.location().strip();
        String deviceUid = request.deviceUid().strip();
        if (!deviceUid.matches("[^/+#\\s\\x00]{1,100}")) {
            throw new IllegalArgumentException("Invalid MQTT device UID");
        }
        if (kitRepository.findByDeviceUid(deviceUid).isPresent()) {
            throw new ConflictException("Device UID already exists");
        }
        if (locationRepository.existsByHouseIdAndLocationName(houseId, locationName)) {
            throw new ConflictException("Location already exists in this house");
        }
        AddKitRequest normalizedRequest = new AddKitRequest(locationName, deviceUid);
        Location location = modelMapper.map(normalizedRequest, Location.class);
        location.setHouse(house);
        location = locationRepository.saveAndFlush(location);
        Kit kit = modelMapper.map(normalizedRequest, Kit.class);
        kit.setLocation(location);
        kitRepository.saveAndFlush(kit);
        return "Thêm kit thành công";
    }

    @Override
    @Transactional(readOnly = true)
    public void updateData(String deviceUid, DataRequest data) {
        kitRepository.findByDeviceUid(deviceUid).filter(kit -> deviceUid.equals(kit.getDeviceUid())).ifPresent(kit -> {
            Location location = kit.getLocation();
            houseDataPublisher.publish(location.getHouse().getId(), toResponse(kit, data));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> startMyHouseTelemetry() {
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
    public List<Long> startHouseTelemetry(long houseId) {
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
                    .ifPresent(kit -> {
                        kitIds.add(kit.getId());
                        commandPublisher.startTelemetry(kit.getDeviceUid());
                    });
        }
        return kitIds;
    }

    @Override
    @Transactional(readOnly = true)
    public String sendCommand(long kitId, kitCommand command) {
        Kit kit = kitRepository.findById(kitId)
                .orElseThrow(() -> new DataNotFoundException("Kit not found"));
        Long userId = currentUserProvider.getCurrentUserId();
        Long houseId = kit.getLocation().getHouse().getId();
        if (!houseMemberRepository.existsByHouseIdAndUserId(houseId, userId)) {
            throw new AccessDeniedException("You are not a member of this kit's house");
        }
        commandPublisher.sendCommand(kit.getDeviceUid(), command);
        return "Command sent successfully";
    }

    private DataResponse toResponse(Kit kit, DataRequest data) {
        var prediction = firePredictionService.predict(data);
        DataResponse response = modelMapper.map(data, DataResponse.class);
        response.setKitId(kit.getId());
        response.setDeviceUid(kit.getDeviceUid());
        response.setLocation(kit.getLocation().getLocationName());
        response.setStatus(prediction.label());
        response.setConfidence(prediction.confidence());
        return response;
    }
}
