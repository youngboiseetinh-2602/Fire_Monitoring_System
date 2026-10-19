package com.firemonitoring.model.request;

import com.firemonitoring.enums.HouseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class houseUpdateRequest extends houseRequest {
    @NotNull(message = "Status is required")
    private HouseStatus status;
}
