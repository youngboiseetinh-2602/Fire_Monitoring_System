package com.firemonitoring.entity;

import jakarta.persistence.*;
import com.firemonitoring.enums.HouseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "houses")
@Getter
@Setter
@NoArgsConstructor
public class House {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "house_name", nullable = false, length = 100)
    private String houseName;

    @Column(nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseStatus status = HouseStatus.NORMAL;
}
