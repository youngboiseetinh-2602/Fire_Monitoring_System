package com.firemonitoring.repository;

import com.firemonitoring.entity.Kit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KitRepository extends JpaRepository<Kit, Long> {
    Optional<Kit> findByDeviceUid(String deviceUid);

    Optional<Kit> findByLocationId(Long locationId);
}
