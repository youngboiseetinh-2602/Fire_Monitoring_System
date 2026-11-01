package com.firemonitoring.model.response;

import com.firemonitoring.enums.KitStatus;

public record KitResponse(Long id, String deviceUid, String location, KitStatus status) {
}
