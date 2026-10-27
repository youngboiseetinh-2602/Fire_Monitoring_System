package com.firemonitoring.api;

import com.firemonitoring.model.request.kitCommand;
import com.firemonitoring.model.request.AddKitRequest;
import jakarta.validation.Valid;
import com.firemonitoring.service.KitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KitController {
    private final KitService kitService;

    @PostMapping(value = "/houses/{houseId}/kits", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> addKit(
            @PathVariable long houseId,
            @Valid @RequestBody AddKitRequest request) {
        return ResponseEntity.status(201).body(kitService.addKit(houseId, request));
    }

    @PostMapping("/houses/{houseId}/telemetry/start")
    public ResponseEntity<List<Long>> startHouseTelemetry(@PathVariable("houseId") long houseId) {
        return ResponseEntity.accepted().body(kitService.startHouseTelemetry(houseId));
    }

    @PostMapping("/me/houses/telemetry/start")
    public ResponseEntity<List<Long>> startMyHouseTelemetry() {
        return ResponseEntity.accepted().body(kitService.startMyHouseTelemetry());
    }

    @PutMapping("/kits/{kitId}/command")
    @PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
    public ResponseEntity<String> sendCommand(
            @PathVariable long kitId,
            @RequestBody kitCommand command) {
        return ResponseEntity.accepted().body(kitService.sendCommand(kitId, command));
    }
}
