package com.tallerescomunitarios.backend.service;

import com.tallerescomunitarios.backend.dto.auth.AuthResponse;
import com.tallerescomunitarios.backend.dto.auth.LoginRequest;
import com.tallerescomunitarios.backend.dto.auth.RegisterRequest;
import com.tallerescomunitarios.backend.dto.auth.UserResponse;
import com.tallerescomunitarios.backend.model.AppUser;
import com.tallerescomunitarios.backend.model.Role;
import com.tallerescomunitarios.backend.repository.UserRepository;
import com.tallerescomunitarios.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String instructorCode;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       @Value("${app.auth.instructor-code}") String instructorCode) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.instructorCode = instructorCode;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo");
        }
        String document = request.document() == null ? "" : request.document().trim();
        if (request.role() == Role.STUDENT && document.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los estudiantes deben registrar su documento de identidad");
        }
        // Sin este código cualquiera podría registrarse como instructor y administrar talleres.
        if (request.role() == Role.INSTRUCTOR && !matchesSecret(request.instructorCode(), instructorCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El código de instructor no es válido");
        }
        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setDocument(document.isEmpty() ? null : document);
        AppUser saved = users.save(user);
        return new AuthResponse(jwtService.issue(saved), UserResponse.from(saved));
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                // Mismo mensaje para correo inexistente y clave errónea: no revela qué cuentas existen.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }

    public UserResponse profile(Long userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe"));
    }

    private static boolean matchesSecret(String given, String expected) {
        if (given == null || expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(
                given.trim().getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
