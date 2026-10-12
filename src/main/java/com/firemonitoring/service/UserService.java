package com.firemonitoring.service;

import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;
import com.firemonitoring.model.request.userUpdateRequest;
import com.firemonitoring.model.response.userResponse;
import org.springframework.data.domain.Page;
import java.util.Map;

public interface UserService {
    Page<userResponse> findAll(int page);

    Page<userResponse> search(Map<String, ?> params, int page);

    String register(userRequest userRequest);

    String login(loginRequest loginRequest);

    userResponse getInfo(long userId);

    userResponse getMyInfo();

    String updateInfo(userUpdateRequest request);
}
