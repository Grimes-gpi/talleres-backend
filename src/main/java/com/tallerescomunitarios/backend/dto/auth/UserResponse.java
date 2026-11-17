package com.tallerescomunitarios.backend.dto.auth;

import com.tallerescomunitarios.backend.model.AppUser;
import com.tallerescomunitarios.backend.model.Role;

public record UserResponse(Long id, String fullName, String email, Role role, String document) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getDocument());
    }
}
