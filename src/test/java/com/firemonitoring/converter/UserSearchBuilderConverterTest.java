package com.firemonitoring.converter;

import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserSearchBuilderConverterTest {
    private final SearchBuilderConverter converter = new SearchBuilderConverter();

    @Test
    void convertsQueryParametersAndPreservesLeadingZeros() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of(
                "id", "42", "username", " an ", "fullName", "Nguyen An",
                "phone", "0912345678", "cccd", "001234567890", "role", "member"));

        assertEquals(42L, result.getId());
        assertEquals("an", result.getUsername());
        assertEquals("Nguyen An", result.getFullName());
        assertEquals("0912345678", result.getPhone());
        assertEquals("001234567890", result.getCccd());
        assertEquals(UserRole.MEMBER, result.getRole());
    }

    @Test
    void missingAndBlankFiltersAreAbsent() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of("id", " ", "role", ""));
        assertNull(result.getId());
        assertNull(result.getRole());
        assertNull(result.getUsername());
    }

    @Test
    void rejectsInvalidOrOverflowingIdsAndUnknownRoles() {
        for (String id : new String[]{"abc", "1.5", "9223372036854775808"}) {
            BadRequestException error = assertThrows(BadRequestException.class,
                    () -> converter.toUserSearchBuilder(Map.of("id", id)));
            assertTrue(error.getMessage().contains("'id'"));
        }
        assertThrows(BadRequestException.class,
                () -> converter.toUserSearchBuilder(Map.of("role", "unknown")));
    }

    @Test
    void acceptsTypedValues() {
        UserSearchBuilder result = converter.toUserSearchBuilder(Map.of("id", 42L, "role", UserRole.OWNER));
        assertEquals(42L, result.getId());
        assertEquals(UserRole.OWNER, result.getRole());
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
