package com.firemonitoring.repository.specification;

import com.firemonitoring.builder.HouseSearchBuilder;
import com.firemonitoring.entity.House;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HouseSpecification {
    private HouseSpecification() {
    }

    public static Specification<House> from(HouseSearchBuilder criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getCity() != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), contains(criteria.getCity()), '!'));
            }
            if (criteria.getWard() != null) {
                predicates.add(cb.like(cb.lower(root.get("ward")), contains(criteria.getWard()), '!'));
            }
            if (criteria.getDetail() != null) {
                predicates.add(cb.like(cb.lower(root.get("addressDetail")), contains(criteria.getDetail()), '!'));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static String contains(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
