package com.firemonitoring.api;

import com.firemonitoring.model.request.userUpdateRequest;
import com.firemonitoring.model.response.userResponse;
import com.firemonitoring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<userResponse>> search(
            @RequestParam Map<String, String> params,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        // Nhan cac query param tu URL va chuyen cho service xu ly.
        Page<userResponse> users = userService.search(params, page);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/me")
    public ResponseEntity<String> updateInfo(@RequestBody @Valid userUpdateRequest request) {
        return ResponseEntity.ok(userService.updateInfo(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<userResponse> getInfo(@PathVariable("userId") long userId) {
        return ResponseEntity.ok(userService.getInfo(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<userResponse> getMyInfo() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

}
