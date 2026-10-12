package com.firemonitoring.repository.specification;

import com.firemonitoring.builder.UserSearchBuilder;
import com.firemonitoring.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecification {
    private UserSpecification() {
    }

    public static Specification<User> from(UserSearchBuilder criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getId() != null) {
                predicates.add(cb.equal(root.get("id"), criteria.getId()));
            }
            if (criteria.getUsername() != null) {
                predicates.add(cb.like(cb.lower(root.get("username")), contains(criteria.getUsername()), '!'));
            }
            if (criteria.getFullName() != null) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), contains(criteria.getFullName()), '!'));
            }
            if (criteria.getPhone() != null) {
                predicates.add(cb.equal(root.get("phone"), criteria.getPhone()));
            }
            if (criteria.getCccd() != null) {
                predicates.add(cb.equal(root.get("cccd"), criteria.getCccd()));
            }
            if (criteria.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.getRole()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
