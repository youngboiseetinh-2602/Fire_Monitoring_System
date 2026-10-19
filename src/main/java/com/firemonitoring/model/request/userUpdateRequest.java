package com.firemonitoring.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class userUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "Phone must contain 10 digits and start with 0")
    private String phone;

    @NotBlank(message = "CCCD is required")
    @Pattern(regexp = "[0-9]{12}", message = "CCCD must contain exactly 12 digits")
    private String cccd;
}
