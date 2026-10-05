package com.firemonitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "house_members", uniqueConstraints =
        @UniqueConstraint(name = "uq_house_members", columnNames = {"house_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class HouseMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "house_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_house_members_house"))
    private House house;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_house_members_user"))
    private User user;
}
