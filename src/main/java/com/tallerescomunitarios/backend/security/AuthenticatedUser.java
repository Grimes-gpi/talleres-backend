package com.tallerescomunitarios.backend.security;

import com.tallerescomunitarios.backend.model.Role;

/** Identidad extraída del JWT. Se inyecta en los controladores con {@code @AuthenticationPrincipal}. */
public record AuthenticatedUser(Long id, String email, String fullName, Role role, String document) {
    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }
}
