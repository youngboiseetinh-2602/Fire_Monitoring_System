package com.firemonitoring.service;

import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.request.AddKitRequest;
import com.firemonitoring.model.response.AddKitResponse;
import com.firemonitoring.model.request.kitCommand;

import java.util.List;

public interface KitService {
    AddKitResponse addKit(long houseId, AddKitRequest request);
    List<Long> getMyHouseKits();
    List<Long> getHouseKit(long houseId);
    void updateData(String deviceUid, DataRequest data);
    String sendCommand(long kitId, kitCommand command);
}
