package com.firemonitoring.util;

import com.firemonitoring.exception.BadRequestException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MapUtil {
    private MapUtil() {
    }

    /** Missing, null and blank parameters represent an absent filter. */
    public static <T> T getObject(Map<String, ?> params, String field, Class<T> targetType) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Object value = params.get(field);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (targetType == String.class) {
            return targetType.cast(text);
        }
        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }
        try {
            if (targetType == Long.class) {
                return targetType.cast(Long.valueOf(text));
            }
            if (targetType == Integer.class) {
                return targetType.cast(Integer.valueOf(text));
            }
            if (targetType == Boolean.class) {
                if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("Expected true or false");
                }
                return targetType.cast(Boolean.valueOf(text));
            }
            if (targetType.isEnum()) {
                for (T constant : targetType.getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals(text.toUpperCase(Locale.ROOT))) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException("Unknown enum value");
            }
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid parameter '" + field
                    + "': expected " + targetType.getSimpleName());
        }
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }
}
