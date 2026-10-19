package com.firemonitoring.converter;

import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.builder.HouseSearchBuilder;

import com.firemonitoring.util.MapUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchBuilderConverter {

    public HouseSearchBuilder toHouseSearchBuilder(Map<String, ?> params) {
        return HouseSearchBuilder.builder()
                .setCity(MapUtil.getObject(params, "city", String.class))
                .setWard(MapUtil.getObject(params, "ward", String.class))
                .setDetail(MapUtil.getObject(params, "detail", String.class))
                .build();
    }

    public UserSearchBuilder toUserSearchBuilder(Map<String, ?> params) {
        // Lay tung tham so tu input va chuyen ve dung kieu du lieu.
        String username = MapUtil.getObject(params, "username", String.class);
        String fullName = MapUtil.getObject(params, "fullName", String.class);
        String cccd = MapUtil.getObject(params, "cccd", String.class);

        // Gan cac gia tri vao Builder, sau do tao doi tuong tim kiem.
        UserSearchBuilder.Builder builder = new UserSearchBuilder.Builder();
        builder.setUsername(username);
        builder.setFullName(fullName);
        builder.setCccd(cccd);

        return builder.build();
    }
}
