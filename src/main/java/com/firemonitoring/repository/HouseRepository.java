package com.firemonitoring.repository;

import com.firemonitoring.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface HouseRepository extends JpaRepository<House, Long>, JpaSpecificationExecutor<House> {
    Page<House> findByIdIn(Collection<Long> ids, Pageable pageable);
}
