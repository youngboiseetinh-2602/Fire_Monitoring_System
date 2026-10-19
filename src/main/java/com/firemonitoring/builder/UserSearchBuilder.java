package com.firemonitoring.builder;


import lombok.Getter;

/** Immutable search criteria constructed through the nested Builder. */
@Getter
public final class UserSearchBuilder {
    private final String username;
    private final String fullName;
    private final String cccd;

    private UserSearchBuilder(Builder builder) {
        this.username = builder.username;
        this.fullName = builder.fullName;
        this.cccd = builder.cccd;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return username == null && fullName == null
                && cccd == null;
    }

    public static final class Builder {
        private String username;
        private String fullName;
        private String cccd;

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setFullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder setCccd(String cccd) {
            this.cccd = cccd;
            return this;
        }

        public UserSearchBuilder build() {
            return new UserSearchBuilder(this);
        }
    }
}
