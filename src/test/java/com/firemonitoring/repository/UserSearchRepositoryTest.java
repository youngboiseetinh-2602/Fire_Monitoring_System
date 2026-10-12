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
    void combinesFiltersAndPaginatesMatchingUsers() {
        User member = save("an", "Nguyen An", UserRole.MEMBER, "001", "091");
        save("owner", "Nguyen An", UserRole.OWNER, "002", "092");
        var criteria = UserSearchBuilder.builder().setFullName("NGUYEN")
                .setRole(UserRole.MEMBER).setPhone("091").setCccd("001").setId(member.getId()).build();
        var result = repository.findAll(UserSpecification.from(criteria), PageRequest.of(0, 20));
        assertEquals(1, result.getTotalElements());
        assertEquals(member.getId(), result.getContent().getFirst().getId());
        assertEquals(0, repository.findAll(UserSpecification.from(criteria), PageRequest.of(1, 20)).getNumberOfElements());
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
