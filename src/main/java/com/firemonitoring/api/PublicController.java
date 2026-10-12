package com.firemonitoring.api;

import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;
import com.firemonitoring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid userRequest request) {
        String message = userService.register(request);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid loginRequest loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }

}
