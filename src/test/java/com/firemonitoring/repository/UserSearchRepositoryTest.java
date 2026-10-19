package com.firemonitoring.repository;

import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.entity.User;
import com.firemonitoring.enums.UserRole;
import com.firemonitoring.repository.specification.UserSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserSearchRepositoryTest {
    @Autowired
    private UserRepository repository;

    @Test
    void matchesAnyFilterAndPaginatesWithoutDuplicates() {
        User usernameMatch = save("an", "Le Minh", UserRole.MEMBER, "123456789012", "091");
        User nameMatch = save("binh", "Tran Binh", UserRole.MEMBER, "223456789012", "092");
        User cccdMatch = save("cuong", "Le Cuong", UserRole.MEMBER, "123001145678", "093");
        User allMatch = save("anh", "Tran Anh", UserRole.MEMBER, "001123456789", "094");
        save("dung", "Le Dung", UserRole.MEMBER, "323456789012", "095");

        var criteria = UserSearchBuilder.builder().setFullName("TRAN")
                .setUsername("AN").setCccd("0011").build();
        var specification = UserSpecification.from(criteria);
        var result = repository.findAll(specification);
        assertEquals(4, result.size());
        assertEquals(java.util.Set.of(usernameMatch.getId(), nameMatch.getId(), cccdMatch.getId(), allMatch.getId()),
                result.stream().map(User::getId).collect(java.util.stream.Collectors.toSet()));
        var page = repository.findAll(specification, PageRequest.of(0, 2));
        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(2, page.getNumberOfElements());
        assertEquals(0, repository.findAll(specification, PageRequest.of(2, 2)).getNumberOfElements());
    }

    @Test
    void emptyCriteriaReturnsAllUsers() {
        save("an", "Nguyen An", UserRole.MEMBER, "001234567890", "091");
        save("binh", "Tran Binh", UserRole.MEMBER, "001234567891", "092");
        assertEquals(2, repository.findAll(UserSpecification.from(UserSearchBuilder.builder().build())).size());
    }

    @Test
    void treatsLikeWildcardsAsLiteralCharacters() {
        save("user_%!", "An", UserRole.MEMBER, "003", "093");
        save("userABC", "Binh", UserRole.MEMBER, "004", "094");
        var criteria = UserSearchBuilder.builder().setUsername("_%!").build();
        var result = repository.findAll(UserSpecification.from(criteria));
        assertEquals(1, result.size());
        assertEquals("user_%!", result.getFirst().getUsername());
    }

    @Test
    void findsCccdContainingDigitsAnywhere() {
        User first = save("first", "An", UserRole.MEMBER, "001123456789", "095");
        User middle = save("middle", "Binh", UserRole.MEMBER, "123001145678", "096");
        User last = save("last", "Cuong", UserRole.MEMBER, "123456780011", "097");
        save("other", "Dung", UserRole.MEMBER, "123456789012", "098");

        var criteria = UserSearchBuilder.builder().setCccd("0011").build();
        var result = repository.findAll(UserSpecification.from(criteria));
        assertEquals(java.util.Set.of(first.getId(), middle.getId(), last.getId()),
                result.stream().map(User::getId).collect(java.util.stream.Collectors.toSet()));

        var wildcard = UserSearchBuilder.builder().setCccd("%").build();
        assertEquals(0, repository.findAll(UserSpecification.from(wildcard)).size());
    }

    private User save(String username, String name, UserRole role, String cccd, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("test-password");
        user.setFullName(name);
        user.setRole(role);
        user.setCccd(cccd);
        user.setPhone(phone);
        return repository.saveAndFlush(user);
    }
}
