package com.firemonitoring.api;

import com.firemonitoring.model.request.kitCommand;
import com.firemonitoring.service.KitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/house/{houseId}/kit")
    public ResponseEntity<List<Long>> getHouseKit(@PathVariable("houseId") long houseId) {
        return ResponseEntity.accepted().body(kitService.getHouseKit(houseId));
    }

    @PostMapping("/me/house/kit")
    public ResponseEntity<List<Long>> getMyHouseKits() {
        return ResponseEntity.accepted().body(kitService.getMyHouseKits());
    }

    @PutMapping("/kits/{kitId}/command")
    public ResponseEntity<String> sendCommand(
            @PathVariable long kitId,
            @RequestBody kitCommand command) {
        return ResponseEntity.accepted().body(kitService.sendCommand(kitId, command));
    }
}
