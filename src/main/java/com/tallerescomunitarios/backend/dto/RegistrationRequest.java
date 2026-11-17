package com.tallerescomunitarios.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un instructor envía nombre y documento de la persona. Un estudiante solo envía el taller:
 * el backend usa los datos de su cuenta, por eso nombre y documento son opcionales aquí.
 */
public record RegistrationRequest(
        @NotNull(message = "El identificador del taller es obligatorio") Long workshopId,
        @Size(max = 255, message = "El nombre es demasiado largo") String beneficiaryName,
        @Size(max = 255, message = "El documento es demasiado largo") String beneficiaryDocument
) { }
