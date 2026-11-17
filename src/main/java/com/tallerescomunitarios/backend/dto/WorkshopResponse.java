package com.tallerescomunitarios.backend.dto;

import com.tallerescomunitarios.backend.model.Workshop;

import java.time.LocalDateTime;

public record WorkshopResponse(
        Long id,
        String name,
        String description,
        String instructor,
        String category,
        String imageUrl,
        Integer maxCapacity,
        LocalDateTime dateTime,
        String status,
        long registeredPeople,
        long availableSlots,
        Long ownerId
) {
    public static WorkshopResponse from(Workshop workshop, long registeredPeople) {
        return new WorkshopResponse(
                workshop.getId(), workshop.getName(), workshop.getDescription(), workshop.getInstructor(),
                workshop.getCategory(), workshop.getImageUrl(),
                workshop.getMaxCapacity(), workshop.getDateTime(), workshop.getStatus(), registeredPeople,
                Math.max(0, workshop.getMaxCapacity() - registeredPeople),
                workshop.getOwnerId()
        );
    }
}
