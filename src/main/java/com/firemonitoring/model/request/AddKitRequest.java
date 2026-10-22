package com.firemonitoring.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddKitRequest(
        @NotBlank @Size(max = 100) String location,
        @NotBlank @Size(max = 100) String deviceUid) {
}
