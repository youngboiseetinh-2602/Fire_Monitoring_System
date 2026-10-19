package com.firemonitoring.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class houseRequest {
    @NotBlank(message = "House name is required")
    @Size(max = 100, message = "House name must not exceed 100 characters")
    private String houseName;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "Ward is required")
    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;

    @NotBlank(message = "Address detail is required")
    @Size(max = 255, message = "Address detail must not exceed 255 characters")
    private String addressDetail;
}
