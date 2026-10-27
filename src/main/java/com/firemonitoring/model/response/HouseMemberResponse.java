package com.firemonitoring.model.response;

import com.firemonitoring.enums.UserRole;

public record HouseMemberResponse(Long id, String username, String fullName, UserRole role) {
}
