package com.organics.products.config;

import com.organics.products.entity.Admin;
import com.organics.products.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtil {

    public static Optional<Long> getCurrentUserId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof Admin admin) {
            return Optional.of(admin.getId());
        }

        if (principal instanceof User user) {
            return Optional.of(user.getId());
        }

        return Optional.empty();
    }

    public static boolean hasRole(String role) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}

