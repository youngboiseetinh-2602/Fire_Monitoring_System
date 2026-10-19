package com.firemonitoring.service.impl;

import com.firemonitoring.entity.House;
import com.firemonitoring.builder.HouseSearchBuilder;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.repository.specification.HouseSpecification;
import java.util.Map;
import com.firemonitoring.enums.HouseStatus;
import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.response.houseResponse;
import com.firemonitoring.repository.HouseRepository;
import com.firemonitoring.service.HouseService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HouseServiceImpl implements HouseService {
    private final HouseRepository houseRepository;
    private final ModelMapper modelMapper;
    private final SearchBuilderConverter searchBuilderConverter;

    @Override
    @Transactional(readOnly = true)
    public Page<houseResponse> findHouse(Map<String, ?> params, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        HouseSearchBuilder criteria = searchBuilderConverter.toHouseSearchBuilder(params);
        if (criteria.isEmpty()) {
            return findAll(page);
        }
        Specification<House> specification = HouseSpecification.from(criteria);
        Pageable pageable = PageRequest.of(page, 20, Sort.by("id").ascending());
        return houseRepository.findAll(specification, pageable)
                .map(house -> modelMapper.map(house, houseResponse.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<houseResponse> findAll(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("id").ascending());
        return houseRepository.findAll(pageable)
                .map(house -> modelMapper.map(house, houseResponse.class));
    }

    @Override
    @Transactional
    public houseResponse createHouse(houseRequest request) {
        House house = modelMapper.map(request, House.class);
        house.setStatus(HouseStatus.NORMAL);
        return modelMapper.map(houseRepository.save(house), houseResponse.class);
    }

    @Override
    @Transactional
    public houseResponse updateHouse(long houseId, houseUpdateRequest request) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> new DataNotFoundException("House not found"));
        house.setHouseName(request.getHouseName());
        house.setCity(request.getCity());
        house.setWard(request.getWard());
        house.setAddressDetail(request.getAddressDetail());
        house.setStatus(request.getStatus());
        return modelMapper.map(houseRepository.save(house), houseResponse.class);
    }
}
