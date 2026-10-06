package com.firemonitoring.service;

import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;

public interface UserService {
    String register(userRequest userRequest);

    String login(loginRequest loginRequest);
}
