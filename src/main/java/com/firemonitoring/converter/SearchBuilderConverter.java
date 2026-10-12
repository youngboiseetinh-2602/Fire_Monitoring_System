package com.firemonitoring.converter;

import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.util.MapUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SearchBuilderConverter {

    public UserSearchBuilder toUserSearchBuilder(Map<String, ?> params) {
        // Lay tung tham so tu input va chuyen ve dung kieu du lieu.
        Long id = MapUtil.getObject(params, "id", Long.class);
        String username = MapUtil.getObject(params, "username", String.class);
        String fullName = MapUtil.getObject(params, "fullName", String.class);
        String phone = MapUtil.getObject(params, "phone", String.class);
        String cccd = MapUtil.getObject(params, "cccd", String.class);
        UserRole role = MapUtil.getObject(params, "role", UserRole.class);

        // Gan cac gia tri vao Builder, sau do tao doi tuong tim kiem.
        UserSearchBuilder.Builder builder = new UserSearchBuilder.Builder();
        builder.setId(id);
        builder.setUsername(username);
        builder.setFullName(fullName);
        builder.setPhone(phone);
        builder.setCccd(cccd);
        builder.setRole(role);

        return builder.build();
    }
}
