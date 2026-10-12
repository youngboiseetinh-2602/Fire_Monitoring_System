package com.firemonitoring.builder;

import com.firemonitoring.enums.UserRole;
import lombok.Getter;

/** Immutable search criteria constructed through the nested Builder. */
@Getter
public final class UserSearchBuilder {
    private final Long id;
    private final String username;
    private final String fullName;
    private final String phone;
    private final String cccd;
    private final UserRole role;

    private UserSearchBuilder(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.fullName = builder.fullName;
        this.phone = builder.phone;
        this.cccd = builder.cccd;
        this.role = builder.role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return id == null && username == null && fullName == null
                && phone == null && cccd == null && role == null;
    }

    public static final class Builder {
        private Long id;
        private String username;
        private String fullName;
        private String phone;
        private String cccd;
        private UserRole role;

        public Builder setId(Long id) {
            this.id = id;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setCccd(String cccd) {
            this.cccd = cccd;
            return this;
        }

        public Builder setRole(UserRole role) {
            this.role = role;
            return this;
        }

        public UserSearchBuilder build() {
            return new UserSearchBuilder(this);
        }
    }
}
