package com.firemonitoring.api;

import com.firemonitoring.model.request.loginRequest;
import com.firemonitoring.model.request.userRequest;
import com.firemonitoring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicController {

    private final UserService userService;

    @PostMapping(value ="/public/register")
    public ResponseEntity<String> resgister (@RequestBody @Valid userRequest userRequest){
        return ResponseEntity.ok(userService.register(userRequest));

    }

    @PostMapping(value = "/public/login")
    public ResponseEntity<String> login(@RequestBody @Valid loginRequest loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }
}
