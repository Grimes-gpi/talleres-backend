package com.tallerescomunitarios.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record WorkshopRequest(
        @NotBlank(message = "El nombre es obligatorio") String name,
        @NotBlank(message = "La descripción es obligatoria") String description,
        @NotBlank(message = "El nombre del instructor es obligatorio") String instructor,
        @NotBlank(message = "La categoría es obligatoria") String category,
        String imageUrl,
        @NotNull(message = "El cupo máximo es obligatorio") @Positive(message = "El cupo debe ser mayor que cero") Integer maxCapacity,
        @NotNull(message = "La fecha y hora son obligatorias") LocalDateTime dateTime
) { }
