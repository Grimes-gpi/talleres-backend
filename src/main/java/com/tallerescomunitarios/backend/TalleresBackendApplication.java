package com.tallerescomunitarios.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// La autenticación usa JWT propio: se omite el usuario en memoria que Spring Security crea por defecto.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TalleresBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(TalleresBackendApplication.class, args);
    }
}
