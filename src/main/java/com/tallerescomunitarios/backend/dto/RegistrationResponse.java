package com.tallerescomunitarios.backend.dto;

import com.tallerescomunitarios.backend.model.Registration;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long workshopId,
        String workshopName,
        String beneficiaryName,
        String beneficiaryDocument,
        LocalDateTime registrationDate,
        String status,
        boolean attended
) {
    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.getId(), registration.getWorkshop().getId(), registration.getWorkshop().getName(),
                registration.getBeneficiaryName(), registration.getBeneficiaryDocument(),
                registration.getRegistrationDate(), registration.getStatus(), registration.isAttended()
        );
    }
}
