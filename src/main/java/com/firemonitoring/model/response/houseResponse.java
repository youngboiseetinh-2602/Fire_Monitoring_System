package com.firemonitoring.model.response;

import com.firemonitoring.enums.HouseStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class houseResponse {
    private Long id;
    private String houseName;
    private String city;
    private String ward;
    private String addressDetail;
    private HouseStatus status;
}
