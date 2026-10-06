package com.firemonitoring.service.impl;

import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;
import com.firemonitoring.repository.UserRepository;
import com.firemonitoring.security.JwtProvider;
import com.firemonitoring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

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
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("user was not found"));

        return jwtProvider.generateToken(userDetails, user.getId());
    }
}
