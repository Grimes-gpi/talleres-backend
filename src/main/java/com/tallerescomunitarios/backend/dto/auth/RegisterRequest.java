package com.tallerescomunitarios.backend.dto.auth;

import com.tallerescomunitarios.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre es demasiado largo") String fullName,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Escribe un correo válido") String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
        @NotNull(message = "Elige un rol") Role role,
        @Size(max = 30, message = "El documento es demasiado largo") String document,
        String instructorCode
) { }
