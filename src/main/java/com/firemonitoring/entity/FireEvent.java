package com.firemonitoring.entity;

import jakarta.persistence.*;
import com.firemonitoring.enums.EventType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class FireEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_events_location"))
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal temperature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal smoke;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gas;

    @Column(nullable = false)
    private Byte infrared;

    @Column(name = "detected_at", nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime detectedAt = LocalDateTime.now();
}
