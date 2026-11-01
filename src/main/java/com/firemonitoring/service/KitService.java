package com.firemonitoring.service;

import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.request.AddKitRequest;
import com.firemonitoring.model.request.kitCommand;
import com.firemonitoring.model.response.KitResponse;

import java.util.List;

public interface KitService {
    String addKit(long houseId, AddKitRequest request);
    List<KitResponse> getHouseKits(long houseId);
    List<Long> startMyHouseTelemetry();
    List<Long> startHouseTelemetry(long houseId);
    void updateData(String deviceUid, DataRequest data);
    String sendCommand(long kitId, kitCommand command);
}
