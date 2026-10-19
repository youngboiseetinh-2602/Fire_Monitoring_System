package com.firemonitoring.builder;

import lombok.Getter;

@Getter
public final class HouseSearchBuilder {
    private final String city;
    private final String ward;
    private final String detail;

    private HouseSearchBuilder(Builder builder) {
        this.city = builder.city;
        this.ward = builder.ward;
        this.detail = builder.detail;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return city == null && ward == null && detail == null;
    }

    public static final class Builder {
        private String city;
        private String ward;
        private String detail;

        public Builder setCity(String city) {
            this.city = normalize(city);
            return this;
        }

        public Builder setWard(String ward) {
            this.ward = normalize(ward);
            return this;
        }

        public Builder setDetail(String detail) {
            this.detail = normalize(detail);
            return this;
        }

        public HouseSearchBuilder build() {
            return new HouseSearchBuilder(this);
        }

        private static String normalize(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
