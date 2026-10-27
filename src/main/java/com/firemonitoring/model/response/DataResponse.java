package com.firemonitoring.model.response;

import com.firemonitoring.enums.EventType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataResponse {
    private Long kitId;
    private String deviceUid;
    private float temperature;
    private float humidity;
    private int gas;
    private boolean flame;
    private String location;
    private EventType status;
    private double confidence;
}
