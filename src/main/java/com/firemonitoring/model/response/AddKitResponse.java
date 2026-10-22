package com.firemonitoring.model.response;

import com.firemonitoring.enums.KitStatus;

public record AddKitResponse(Long id, String deviceUid, Long houseId,
                             Long locationId, String location, KitStatus status) {
}
