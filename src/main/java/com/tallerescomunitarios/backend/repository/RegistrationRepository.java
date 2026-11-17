package com.tallerescomunitarios.backend.repository;

import com.tallerescomunitarios.backend.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    long countByWorkshop_IdAndStatus(Long workshopId, String status);
    boolean existsByWorkshop_IdAndBeneficiaryDocumentAndStatus(Long workshopId, String document, String status);
    Optional<Registration> findFirstByWorkshop_IdAndBeneficiaryDocumentOrderByIdDesc(Long workshopId, String document);
    List<Registration> findByWorkshop_IdOrderByRegistrationDateDesc(Long workshopId);
    List<Registration> findByBeneficiaryDocumentOrderByRegistrationDateDesc(String document);
}
