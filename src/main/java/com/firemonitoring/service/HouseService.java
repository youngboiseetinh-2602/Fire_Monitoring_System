package com.firemonitoring.service;

import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.model.response.houseResponse;
import com.firemonitoring.model.response.HouseMemberResponse;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface HouseService {
    String addPeople(com.firemonitoring.model.request.HousePeopleRequest request);

    String removePeople(com.firemonitoring.model.request.HousePeopleRequest request);

    List<HouseMemberResponse> getPeople();

    Page<houseResponse> findHouse(Map<String, ?> params, int page);

    Page<houseResponse> findAll(int page);

    houseResponse createHouse(houseRequest request);

    houseResponse updateHouse(long houseId, houseUpdateRequest request);
}
