package com.firemonitoring.api;

import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.model.response.houseResponse;
import com.firemonitoring.model.response.userResponse;
import com.firemonitoring.service.HouseService;
import com.firemonitoring.model.request.HousePeopleRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;

    @PostMapping("/{houseId}/people")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> addPeople(@PathVariable long houseId, @RequestBody @Valid HousePeopleRequest request) {
        return ResponseEntity.ok(houseService.addPeople(houseId, request));
    }

    @DeleteMapping("/{houseId}/people")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<String> removePeople(@PathVariable long houseId, @RequestBody @Valid HousePeopleRequest request) {
        return ResponseEntity.ok(houseService.removePeople(houseId, request));
    }

    @GetMapping("/{houseId}/people")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<userResponse>> getMember(@PathVariable long houseId) {
        return ResponseEntity.ok(houseService.getMember(houseId));
    }

    @GetMapping
    public ResponseEntity<Page<houseResponse>> findAll(
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(houseService.findAll(page));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<houseResponse>> findHouse(
            @RequestParam Map<String, String> params,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(houseService.findHouse(params, page));
    }

    @PostMapping(value = "/createHouse", produces = "text/plain;charset=UTF-8")
    @PreAuthorize("hasAnyRole('MEMBER', 'OWNER')")
    public ResponseEntity<String> createHouse(@RequestBody @Valid houseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.createHouse(request));
    }

    @PutMapping("/updateHouse/{houseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<houseResponse> updateHouse(
            @PathVariable("houseId") long houseId,
            @RequestBody @Valid houseUpdateRequest request) {
        return ResponseEntity.ok(houseService.updateHouse(houseId, request));
    }
}
