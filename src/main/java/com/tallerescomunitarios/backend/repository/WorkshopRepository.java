package com.tallerescomunitarios.backend.repository;

import com.tallerescomunitarios.backend.model.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    List<Workshop> findByNameContainingIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    java.util.Optional<Workshop> findByNameIgnoreCase(String name);
    List<Workshop> findByCategoryIgnoreCase(String category);
    List<Workshop> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);
}
