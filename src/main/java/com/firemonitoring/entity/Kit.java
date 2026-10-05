package com.firemonitoring.entity;

import jakarta.persistence.*;
import com.firemonitoring.enums.KitStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kits", uniqueConstraints = {
        @UniqueConstraint(name = "uq_kits_device_uid", columnNames = "device_uid"),
        @UniqueConstraint(name = "uq_kits_location", columnNames = "location_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Kit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uid", nullable = false, length = 100)
    private String deviceUid;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_kits_location"))
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KitStatus status = KitStatus.OFFLINE;
}
