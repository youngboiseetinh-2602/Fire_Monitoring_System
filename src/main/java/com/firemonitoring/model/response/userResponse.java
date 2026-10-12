package com.firemonitoring.model.response;

import com.firemonitoring.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class userResponse {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String cccd;
    private UserRole role;
}
