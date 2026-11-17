package com.tallerescomunitarios.backend.controller;

import com.tallerescomunitarios.backend.dto.RegistrationRequest;
import com.tallerescomunitarios.backend.dto.RegistrationResponse;
import com.tallerescomunitarios.backend.dto.WorkshopRequest;
import com.tallerescomunitarios.backend.dto.WorkshopResponse;
import com.tallerescomunitarios.backend.model.Registration;
import com.tallerescomunitarios.backend.model.Workshop;
import com.tallerescomunitarios.backend.repository.RegistrationRepository;
import com.tallerescomunitarios.backend.repository.WorkshopRepository;
import com.tallerescomunitarios.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Reglas de acceso:
 * <ul>
 *   <li>Cualquier usuario autenticado consulta talleres.</li>
 *   <li>Solo INSTRUCTOR crea talleres y ve inscritos; edita, cancela y gestiona inscripciones solo de sus
 *       propios talleres (los talleres heredados sin dueño los gestiona cualquier instructor).</li>
 *   <li>STUDENT se inscribe a sí mismo con los datos de su cuenta y solo cancela su propia inscripción.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class WorkshopController {
    private static final String INSTRUCTOR = "hasRole('INSTRUCTOR')";

    private final WorkshopRepository workshopRepository;
    private final RegistrationRepository registrationRepository;

    public WorkshopController(WorkshopRepository workshopRepository, RegistrationRepository registrationRepository) {
        this.workshopRepository = workshopRepository;
        this.registrationRepository = registrationRepository;
    }

    @GetMapping("/workshops")
    public List<WorkshopResponse> listWorkshops(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasCategory = category != null && !category.isBlank();
        List<Workshop> workshops;
        if (hasName && hasCategory) {
            workshops = workshopRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(name, category);
        } else if (hasName) {
            workshops = workshopRepository.findByNameContainingIgnoreCase(name);
        } else if (hasCategory) {
            workshops = workshopRepository.findByCategoryIgnoreCase(category);
        } else {
            workshops = workshopRepository.findAll();
        }
        return workshops.stream().map(this::toWorkshopResponse).toList();
    }

    @GetMapping("/workshops/{id}")
    public WorkshopResponse getWorkshop(@PathVariable Long id) {
        return toWorkshopResponse(findWorkshop(id));
    }

    @PostMapping("/workshops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(INSTRUCTOR)
    public WorkshopResponse createWorkshop(@Valid @RequestBody WorkshopRequest request,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Workshop workshop = new Workshop();
        copyWorkshopData(request, workshop);
        workshop.setOwnerId(user.id());
        return toWorkshopResponse(workshopRepository.save(workshop));
    }

    @PutMapping("/workshops/{id}")
    @Transactional
    @PreAuthorize(INSTRUCTOR)
    public WorkshopResponse updateWorkshop(@PathVariable Long id, @Valid @RequestBody WorkshopRequest request,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Workshop workshop = findManagedWorkshop(id, user);
        long registered = registrationRepository.countByWorkshop_IdAndStatus(workshop.getId(), "ACTIVE");
        if (request.maxCapacity() < registered) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cupo máximo no puede ser menor que las inscripciones confirmadas (" + registered + ")");
        }
        copyWorkshopData(request, workshop);
        return toWorkshopResponse(workshopRepository.saveAndFlush(workshop));
    }

    @DeleteMapping("/workshops/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(INSTRUCTOR)
    public void cancelWorkshop(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        Workshop workshop = findManagedWorkshop(id, user);
        workshop.setStatus("CANCELLED");
        workshopRepository.save(workshop);
    }

    @PostMapping("/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse createRegistration(@Valid @RequestBody RegistrationRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser user) {
        Workshop workshop = user.isInstructor() ? findManagedWorkshop(request.workshopId(), user)
                : findWorkshop(request.workshopId());
        String beneficiaryName;
        String beneficiaryDocument;
        if (user.isInstructor()) {
            beneficiaryName = trimToNull(request.beneficiaryName());
            beneficiaryDocument = trimToNull(request.beneficiaryDocument());
            if (beneficiaryName == null || beneficiaryDocument == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escribe el nombre y el documento de la persona a inscribir");
            }
        } else {
            // El estudiante solo puede inscribirse a sí mismo: se ignoran nombre/documento del cuerpo.
            beneficiaryName = user.fullName();
            beneficiaryDocument = trimToNull(user.document());
            if (beneficiaryDocument == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tu cuenta no tiene documento registrado");
            }
        }
        if (!"ACTIVE".equals(workshop.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El taller está cancelado");
        }
        if (registrationRepository.existsByWorkshop_IdAndBeneficiaryDocumentAndStatus(
                workshop.getId(), beneficiaryDocument, "ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La persona ya está inscrita en este taller");
        }
        long registered = registrationRepository.countByWorkshop_IdAndStatus(workshop.getId(), "ACTIVE");
        if (registered >= workshop.getMaxCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay cupos disponibles en este taller");
        }
        Registration registration = new Registration();
        registration.setWorkshop(workshop);
        registration.setBeneficiaryName(beneficiaryName);
        registration.setBeneficiaryDocument(beneficiaryDocument);
        if (!user.isInstructor()) registration.setUserId(user.id());
        return RegistrationResponse.from(registrationRepository.save(registration));
    }

    @GetMapping("/workshops/{id}/registrations")
    @PreAuthorize(INSTRUCTOR)
    public List<RegistrationResponse> listRegistrations(@PathVariable Long id) {
        findWorkshop(id);
        return registrationRepository.findByWorkshop_IdOrderByRegistrationDateDesc(id)
                .stream().map(RegistrationResponse::from).toList();
    }

    /** Inscripciones de la persona autenticada (por documento, incluye las que hizo un instructor). */
    @GetMapping("/me/registrations")
    public List<RegistrationResponse> myRegistrations(@AuthenticationPrincipal AuthenticatedUser user) {
        String document = trimToNull(user.document());
        if (document == null) return List.of();
        return registrationRepository.findByBeneficiaryDocumentOrderByRegistrationDateDesc(document)
                .stream().map(RegistrationResponse::from).toList();
    }

    @DeleteMapping("/workshops/{id}/registrations")
    @Transactional
    public RegistrationResponse cancelRegistration(
            @PathVariable Long id,
            @RequestParam String document,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (document == null || document.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe el documento de la inscripción");
        }
        if (user.isInstructor()) {
            findManagedWorkshop(id, user);
        } else {
            findWorkshop(id);
            if (!document.trim().equals(trimToNull(user.document()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes cancelar tu propia inscripción");
            }
        }
        Registration registration = registrationRepository
                .findFirstByWorkshop_IdAndBeneficiaryDocumentOrderByIdDesc(id, document.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No encontramos una inscripción para ese documento en este taller"));
        if ("CANCELLED".equals(registration.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La inscripción ya estaba cancelada");
        }
        if (!"ACTIVE".equals(registration.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta inscripción no se puede cancelar");
        }
        if (registration.isAttended()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cancelar una inscripción con asistencia registrada");
        }
        registration.setStatus("CANCELLED");
        return RegistrationResponse.from(registrationRepository.saveAndFlush(registration));
    }

    @PatchMapping("/registrations/{id}/attendance")
    @Transactional
    @PreAuthorize(INSTRUCTOR)
    public RegistrationResponse markAttendance(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));
        ensureCanManage(registration.getWorkshop(), user);
        if (!"ACTIVE".equals(registration.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede marcar asistencia a una inscripción activa");
        }
        registration.setAttended(true);
        return RegistrationResponse.from(registrationRepository.saveAndFlush(registration));
    }

    private Workshop findWorkshop(Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado"));
    }

    private Workshop findManagedWorkshop(Long id, AuthenticatedUser user) {
        Workshop workshop = findWorkshop(id);
        ensureCanManage(workshop, user);
        return workshop;
    }

    private void ensureCanManage(Workshop workshop, AuthenticatedUser user) {
        boolean legacyWithoutOwner = workshop.getOwnerId() == null;
        if (!user.isInstructor() || !(legacyWithoutOwner || workshop.getOwnerId().equals(user.id()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el instructor a cargo puede gestionar este taller");
        }
    }

    private WorkshopResponse toWorkshopResponse(Workshop workshop) {
        long registered = registrationRepository.countByWorkshop_IdAndStatus(workshop.getId(), "ACTIVE");
        return WorkshopResponse.from(workshop, registered);
    }

    private void copyWorkshopData(WorkshopRequest request, Workshop workshop) {
        workshop.setName(request.name());
        workshop.setDescription(request.description());
        workshop.setInstructor(request.instructor());
        workshop.setCategory(request.category().trim());
        workshop.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
        workshop.setMaxCapacity(request.maxCapacity());
        workshop.setDateTime(request.dateTime());
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
