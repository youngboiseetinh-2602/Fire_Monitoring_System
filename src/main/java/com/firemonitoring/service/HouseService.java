package com.firemonitoring.service;

import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.model.response.houseResponse;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface HouseService {
    Page<houseResponse> findHouse(Map<String, ?> params, int page);

    Page<houseResponse> findAll(int page);

    houseResponse createHouse(houseRequest request);

    houseResponse updateHouse(long houseId, houseUpdateRequest request);
}
