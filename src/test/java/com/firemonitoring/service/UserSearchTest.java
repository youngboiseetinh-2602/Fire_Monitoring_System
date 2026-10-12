package com.firemonitoring.service;

import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.entity.User;
import com.firemonitoring.repository.UserRepository;
import com.firemonitoring.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSearchTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserServiceImpl service = new UserServiceImpl(repository, new ModelMapper(),
            null, null, null, null, new SearchBuilderConverter());

    @Test
    void absentAndBlankFiltersUseFindAll() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        service.search(Map.of("page", "0"), 0);
        service.search(Map.of("fullName", " ", "role", ""), 0);
        verify(repository, times(2)).findAll(any(Pageable.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void suppliedFilterUsesSpecification() {
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        service.search(Map.of("role", "MEMBER"), 0);
        verify(repository).findAll(org.mockito.ArgumentMatchers.<Specification<User>>any(), any(Pageable.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void negativePageIsRejectedBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () -> service.search(Map.of(), -1));
        verifyNoInteractions(repository);
    }
}
