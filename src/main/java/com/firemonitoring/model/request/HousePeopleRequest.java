package com.firemonitoring.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HousePeopleRequest(@NotNull @Positive Long userId) {
}
