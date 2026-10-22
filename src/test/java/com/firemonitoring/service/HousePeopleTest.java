package com.firemonitoring.service;

import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.entity.*;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.exception.ConflictException;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.request.HousePeopleRequest;
import com.firemonitoring.repository.*;
import com.firemonitoring.service.impl.HouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HousePeopleTest {
    private final HouseMemberRepository members = mock(HouseMemberRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final HouseServiceImpl service = new HouseServiceImpl(mock(HouseRepository.class),
            new ModelMapper(), new SearchBuilderConverter(), members, users, () -> 7L);
    private User owner;
    private User target;
    private House house;
    private HouseMember ownerMembership;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setId(7L);
        owner.setRole(UserRole.OWNER);
        target = new User();
        target.setId(8L);
        house = new House();
        house.setId(10L);
        ownerMembership = new HouseMember();
        ownerMembership.setUser(owner);
        ownerMembership.setHouse(house);
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        when(users.findById(8L)).thenReturn(Optional.of(target));
        when(members.findByUserId(7L)).thenReturn(List.of(ownerMembership));
    }

    @Test
    void addsByIdToHouseResolvedFromCurrentUser() {
        service.addPeople(new HousePeopleRequest(8L));
        verify(members).saveAndFlush(argThat(member -> member.getHouse() == house && member.getUser() == target));
    }

    @Test
    void removesOnlyMembershipInOwnersHouse() {
        HouseMember membership = new HouseMember();
        membership.setHouse(house);
        membership.setUser(target);
        when(members.findByHouseIdAndUserId(10L, 8L)).thenReturn(Optional.of(membership));
        service.removePeople(new HousePeopleRequest(8L));
        verify(members).delete(membership);
        verify(users, never()).delete(any(User.class));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"MEMBER", "ADMIN"})
    void onlyOwnerCanAddOrRemove(UserRole role) {
        owner.setRole(role);
        assertThrows(AccessDeniedException.class, () -> service.addPeople(new HousePeopleRequest(8L)));
        assertThrows(AccessDeniedException.class, () -> service.removePeople(new HousePeopleRequest(8L)));
        verifyNoInteractions(members);
    }

    @Test
    void rejectsDuplicateMissingMembershipAndSelfRemoval() {
        when(members.existsByHouseIdAndUserId(10L, 8L)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.addPeople(new HousePeopleRequest(8L)));
        assertThrows(DataNotFoundException.class, () -> service.removePeople(new HousePeopleRequest(8L)));
        assertThrows(ConflictException.class, () -> service.removePeople(new HousePeopleRequest(7L)));
        verify(members, never()).saveAndFlush(any());
        verify(members, never()).delete(any());
    }

    @Test
    void rejectsMissingOrAmbiguousHouse() {
        when(members.findByUserId(7L)).thenReturn(List.of());
        assertThrows(DataNotFoundException.class, () -> service.addPeople(new HousePeopleRequest(8L)));
        House second = new House();
        second.setId(11L);
        HouseMember other = new HouseMember();
        other.setHouse(second);
        when(members.findByUserId(7L)).thenReturn(List.of(ownerMembership, other));
        assertThrows(ConflictException.class, () -> service.removePeople(new HousePeopleRequest(8L)));
    }

    @Test
    void rejectsInvalidOrUnknownTarget() {
        for (HousePeopleRequest request : List.of(new HousePeopleRequest(null),
                new HousePeopleRequest(0L), new HousePeopleRequest(-1L))) {
            assertThrows(IllegalArgumentException.class, () -> service.addPeople(request));
        }
        assertThrows(DataNotFoundException.class, () -> service.addPeople(new HousePeopleRequest(999L)));
        verify(members, never()).saveAndFlush(any());
    }
}
