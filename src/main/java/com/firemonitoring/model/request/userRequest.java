package com.firemonitoring.model.request;

import com.firemonitoring.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class userRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must contain 8 to 100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "Phone must contain 10 digits and start with 0")
    private String phone;

    @NotBlank(message = "CCCD is required")
    @Size(max = 20, message = "CCCD must not exceed 20 characters")
    private String cccd;

    @NotNull(message = "Role is required")
    private UserRole role;
}
