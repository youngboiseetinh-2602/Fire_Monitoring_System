package com.firemonitoring.repository;

import com.firemonitoring.entity.HouseMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseMemberRepository extends JpaRepository<HouseMember, Long> {
    List<HouseMember> findByHouseId(Long houseId);

    List<HouseMember> findByUserId(Long userId);

    Optional<HouseMember> findFirstByUserIdOrderByIdAsc(Long userId);

    boolean existsByHouseIdAndUserId(Long houseId, Long userId);
    Optional<HouseMember> findByHouseIdAndUserId(Long houseId, Long userId);
}
