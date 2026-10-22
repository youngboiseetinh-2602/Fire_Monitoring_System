package com.firemonitoring.model.request;

public record DataRequest(
        float temperature,
        float humidity,
        int gas,
        boolean flame
) {}
