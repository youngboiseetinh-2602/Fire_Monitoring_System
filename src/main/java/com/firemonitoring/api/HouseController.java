package com.firemonitoring.api;

import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.model.response.houseResponse;
import com.firemonitoring.service.HouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/houses")
@RequiredArgsConstructor
public class HouseController {
    private final HouseService houseService;

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

    @PostMapping("/createHouse")
    public ResponseEntity<houseResponse> createHouse(@RequestBody @Valid houseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.createHouse(request));
    }

    @PutMapping("/updateHouse/{houseId}")
    public ResponseEntity<houseResponse> updateHouse(
            @PathVariable("houseId") long houseId,
            @RequestBody @Valid houseUpdateRequest request) {
        return ResponseEntity.ok(houseService.updateHouse(houseId, request));
    }
}
