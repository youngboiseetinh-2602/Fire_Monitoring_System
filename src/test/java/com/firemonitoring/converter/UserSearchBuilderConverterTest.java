package com.firemonitoring.converter;

import com.firemonitoring.builder.UserSearchBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserSearchBuilderConverterTest {
    private final SearchBuilderConverter converter = new SearchBuilderConverter();

    @Test
    void convertsQueryParametersAndPreservesLeadingZeros() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of(
                "username", " an ", "fullName", "Nguyen An", "cccd", "001234567890"));

        assertEquals("an", result.getUsername());
        assertEquals("Nguyen An", result.getFullName());
        assertEquals("001234567890", result.getCccd());
        assertFalse(result.isEmpty());
    }

    @Test
    void missingAndBlankFiltersAreAbsent() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of("fullName", " ", "cccd", ""));
        assertNull(result.getFullName());
        assertNull(result.getCccd());
        assertNull(result.getUsername());
        assertTrue(result.isEmpty());
    }

    @Test
    void ignoresFieldsOutsideSupportedSearchInput() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of(
                "id", "abc", "phone", "0912345678", "role", "MEMBER", "page", "0"));
        assertTrue(result.isEmpty());
    }

    @Test
    void reusingBuilderDoesNotChangePreviouslyBuiltCriteria() {
        UserSearchBuilder.Builder builder = UserSearchBuilder.builder().setUsername("an");
        UserSearchBuilder first = builder.build();
        UserSearchBuilder second = builder.setUsername("binh").build();
        assertEquals("an", first.getUsername());
        assertEquals("binh", second.getUsername());
    }
}
