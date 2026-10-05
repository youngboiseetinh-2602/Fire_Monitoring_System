package com.firemonitoring.repository;

import com.firemonitoring.entity.FireEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FireEventRepository extends JpaRepository<FireEvent, Long> {
    List<FireEvent> findByLocationIdOrderByDetectedAtDesc(Long locationId);
}
