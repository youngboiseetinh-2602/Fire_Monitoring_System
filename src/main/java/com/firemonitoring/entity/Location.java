package com.firemonitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations", uniqueConstraints =
        @UniqueConstraint(name = "uq_location_name", columnNames = {"house_id", "location_name"}))
@Getter
@Setter
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_locations_house"))
    private House house;

    @Column(name = "location_name", nullable = false, length = 100)
    private String locationName;
}
