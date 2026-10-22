package com.firemonitoring.service;

import com.firemonitoring.model.request.DataRequest;
import com.firemonitoring.model.request.kitCommand;

import java.util.List;

public interface KitService {
    List<Long> getMyHouseKits();
    List<Long> getHouseKit(long houseId);
    void updateData(long kitId, DataRequest data);
    String sendCommand(long kitId, kitCommand command);
}
