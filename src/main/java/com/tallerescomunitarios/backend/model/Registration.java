package com.tallerescomunitarios.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "registrations")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;
    private String beneficiaryName;
    private String beneficiaryDocument;
    private LocalDateTime registrationDate = LocalDateTime.now();
    private String status = "ACTIVE";
    private boolean attended = false;
    /** Cuenta del estudiante que se inscribió por sí mismo. Nulo si lo inscribió un instructor. */
    private Long userId;

    public Registration() { }

    public Long getId() { return id; }
    public Workshop getWorkshop() { return workshop; }
    public void setWorkshop(Workshop workshop) { this.workshop = workshop; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getBeneficiaryDocument() { return beneficiaryDocument; }
    public void setBeneficiaryDocument(String beneficiaryDocument) { this.beneficiaryDocument = beneficiaryDocument; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
