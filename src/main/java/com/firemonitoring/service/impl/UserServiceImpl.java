package com.firemonitoring.service.impl;

import com.firemonitoring.entity.User;
import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.converter.SearchBuilderConverter;
import com.firemonitoring.repository.specification.UserSpecification;
import java.util.Map;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.exception.DataNotFoundException;
import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;
import com.firemonitoring.model.request.userUpdateRequest;
import com.firemonitoring.model.response.userResponse;
import com.firemonitoring.repository.UserRepository;
import com.firemonitoring.security.CurrentUserProvider;
import com.firemonitoring.security.JwtProvider;
import com.firemonitoring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final CurrentUserProvider currentUserProvider;
    private final SearchBuilderConverter searchBuilderConverter;

    @Override
    @Transactional(readOnly = true)
    public Page<userResponse> search(Map<String, ?> params, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        UserSearchBuilder userSearchBuilder = searchBuilderConverter.toUserSearchBuilder(params);
        if (userSearchBuilder.isEmpty()) {
            return findAll(page);
        }

        // Co dieu kien thi tao truy van loc va lay ket qua theo trang.
        Specification<User> specification = UserSpecification.from(userSearchBuilder);
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("id").ascending());
        Page<User> users = userRepository.findAll(specification, pageable);

        return users.map(user -> modelMapper.map(user, userResponse.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<userResponse> findAll(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative");
        }
        PageRequest pageable = PageRequest.of(page, 20, Sort.by("id").ascending());
        return userRepository.findAll(pageable)
                .map(user -> modelMapper.map(user, userResponse.class));
    }

    @Override
    public String register(userRequest userRequest) {
        userRequest.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        User user = modelMapper.map(userRequest, User.class);
        user.setRole(UserRole.MEMBER);
        userRepository.save(user);
        return "register successfully";
    }

    @Override
    public String login(loginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("user was not found"));

        return jwtProvider.generateToken(user);
    }

    @Override
    public userResponse getInfo(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("user not found"));
        return modelMapper.map(user, userResponse.class);
    }

    @Override
    public userResponse getMyInfo() {
        return getInfo(currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional
    public String updateInfo(userUpdateRequest request) {
        User user = userRepository.findById(currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new DataNotFoundException("user not found"));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setCccd(request.getCccd());
        userRepository.save(user);
        return "update sucessfull";
    }


}
