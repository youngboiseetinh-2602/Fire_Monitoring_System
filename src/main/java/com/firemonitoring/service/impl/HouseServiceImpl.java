package com.firemonitoring.service.impl;

import com.firemonitoring.entity.House;
import com.firemonitoring.entity.HouseMember;
import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.model.request.HousePeopleRequest;
import com.firemonitoring.repository.HouseMemberRepository;
import com.firemonitoring.repository.UserRepository;
import com.firemonitoring.security.CurrentUserProvider;
import com.firemonitoring.exception.ConflictException;
import org.springframework.security.access.AccessDeniedException;
import com.firemonitoring.builder.HouseSearchBuilder;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.repository.specification.HouseSpecification;
import java.util.Map;
import java.util.List;
import com.firemonitoring.enums.HouseStatus;
import com.firemonitoring.model.request.houseRequest;
import com.firemonitoring.model.request.houseUpdateRequest;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.response.houseResponse;
import com.firemonitoring.model.response.userResponse;
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
    private final HouseMemberRepository houseMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public String addPeople(HousePeopleRequest request) {
        Long ownerId = currentUserProvider.getCurrentUserId();
        House house = findOwnerHouse(ownerId);
        User target = findTarget(request);
        if (houseMemberRepository.existsByHouseIdAndUserId(house.getId(), target.getId())) {
            throw new ConflictException("User already belongs to this house");
        }
        HouseMember member = new HouseMember();
        member.setHouse(house);
        member.setUser(target);
        houseMemberRepository.saveAndFlush(member);
        return "Person added successfully";
    }

    @Override
    @Transactional
    public String removePeople(HousePeopleRequest request) {
        Long ownerId = currentUserProvider.getCurrentUserId();
        House house = findOwnerHouse(ownerId);
        User target = findTarget(request);
        if (ownerId.equals(target.getId())) {
            throw new ConflictException("You cannot remove yourself from your house");
        }
        HouseMember member = houseMemberRepository.findByHouseIdAndUserId(house.getId(), target.getId())
                .orElseThrow(() -> new DataNotFoundException("User does not belong to this house"));
        houseMemberRepository.delete(member);
        return "Person removed successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public List<userResponse> getMember() {
        Long userId = currentUserProvider.getCurrentUserId();
        House house = houseMemberRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(HouseMember::getHouse)
                .orElseThrow(() -> new DataNotFoundException("You do not belong to any house"));

        return houseMemberRepository.findByHouseId(house.getId()).stream()
                .map(member -> modelMapper.map(member.getUser(), userResponse.class))
                .toList();
    }

    private House findOwnerHouse(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new DataNotFoundException("Current user not found"));
        if (owner.getRole() != UserRole.OWNER) {
            throw new AccessDeniedException("Only owners can manage house members");
        }
        var houses = houseMemberRepository.findByUserId(ownerId).stream()
                .map(HouseMember::getHouse)
                .collect(java.util.stream.Collectors.toMap(House::getId, house -> house, (first, second) -> first));
        if (houses.isEmpty()) {
            throw new DataNotFoundException("You do not belong to any house");
        }
        if (houses.size() != 1) {
            throw new ConflictException("Multiple houses found; cannot determine which house to manage");
        }
        return houses.values().iterator().next();
    }

    private User findTarget(HousePeopleRequest request) {
        if (request == null || request.userId() == null || request.userId() <= 0) {
            throw new IllegalArgumentException("Provide a positive user ID");
        }
        return userRepository.findById(request.userId())
                .orElseThrow(() -> new DataNotFoundException("User not found"));
    }

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
    public String createHouse(houseRequest request) {
        House house = modelMapper.map(request, House.class);
        house.setStatus(HouseStatus.NORMAL);
        House savedHouse = houseRepository.save(house);

        User currentUser = userRepository.findById(currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new DataNotFoundException("Current user not found"));
        currentUser.setRole(UserRole.OWNER);
        userRepository.save(currentUser);

        HouseMember ownerMembership = new HouseMember();
        ownerMembership.setHouse(savedHouse);
        ownerMembership.setUser(currentUser);
        houseMemberRepository.save(ownerMembership);

        return "Tạo nhà thành công";
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
