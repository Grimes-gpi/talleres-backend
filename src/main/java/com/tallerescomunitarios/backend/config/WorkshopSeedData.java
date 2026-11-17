package com.tallerescomunitarios.backend.config;

import com.tallerescomunitarios.backend.model.Workshop;
import com.tallerescomunitarios.backend.repository.WorkshopRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class WorkshopSeedData implements CommandLineRunner {
    private final WorkshopRepository workshopRepository;

    public WorkshopSeedData(WorkshopRepository workshopRepository) {
        this.workshopRepository = workshopRepository;
    }

    @Override
    public void run(String... args) {
        LocalDate start = LocalDate.now(ZoneId.of("America/Bogota"));
        List<Workshop> examples = List.of(
                workshop("Introducción a informática", "Aprende a usar el computador, organizar archivos y navegar por internet.", "Equipo de formación", "Tecnología", 18, start.plusDays(2), LocalTime.of(9, 0), "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=1000&q=85"),
                workshop("Celular sin miedo", "Practica llamadas, mensajes, videollamadas y trámites digitales desde tu teléfono.", "Equipo de acompañamiento", "Tecnología", 14, start.plusDays(3), LocalTime.of(15, 0), "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=1000&q=85"),
                workshop("Excel para pequeños negocios", "Lleva cuentas sencillas, listas de productos y presupuestos con hojas de cálculo.", "Equipo de formación", "Tecnología", 16, start.plusDays(5), LocalTime.of(10, 0), "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=1000&q=85"),
                workshop("Inteligencia artificial cotidiana", "Conoce usos prácticos de la IA y aprende a verificar la información que produce.", "Facilitador comunitario", "Tecnología", 20, start.plusDays(7), LocalTime.of(14, 0), "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=1000&q=85"),
                workshop("Cocina básica", "Prepara recetas sencillas y seguras con ingredientes económicos.", "Equipo de cocina", "Cocina", 12, start.plusDays(4), LocalTime.of(14, 0), "https://images.unsplash.com/photo-1556911220-bff31c812dba?w=1000&q=85"),
                workshop("Repostería casera", "Aprende técnicas iniciales para hornear panes y postres para compartir o vender.", "Equipo de cocina", "Cocina", 12, start.plusDays(6), LocalTime.of(9, 0), "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=1000&q=85"),
                workshop("Alimentación saludable", "Arma menús equilibrados y conoce alternativas nutritivas para cada día.", "Promotoría de salud", "Cocina", 18, start.plusDays(9), LocalTime.of(11, 0), "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=1000&q=85"),
                workshop("Costura y arreglos", "Practica puntadas básicas y repara prendas de uso diario.", "Equipo de oficios", "Oficios", 10, start.plusDays(8), LocalTime.of(9, 0), "https://images.unsplash.com/photo-1590736969955-71cc94901144?w=1000&q=85"),
                workshop("Huerta urbana", "Inicia una huerta en espacios pequeños y aprende cuidados básicos de las plantas.", "Equipo ambiental", "Medio ambiente", 16, start.plusDays(11), LocalTime.of(8, 30), "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?w=1000&q=85"),
                workshop("Emprender desde casa", "Organiza una idea de negocio, calcula costos y prepara un plan inicial de ventas.", "Equipo de emprendimiento", "Emprendimiento", 20, start.plusDays(13), LocalTime.of(16, 0), "https://images.unsplash.com/photo-1556761175-b413da4baf72?w=1000&q=85")
        );

        for (Workshop example : examples) {
            var existing = workshopRepository.findByNameIgnoreCase(example.getName());
            if (existing.isEmpty()) {
                workshopRepository.save(example);
            } else if (existing.get().getImageUrl() == null || existing.get().getImageUrl().isBlank()) {
                existing.get().setImageUrl(example.getImageUrl());
                workshopRepository.save(existing.get());
            }
        }
    }

    private Workshop workshop(String name, String description, String instructor, String category,
                              int capacity, LocalDate date, LocalTime time, String imageUrl) {
        Workshop workshop = new Workshop();
        workshop.setName(name);
        workshop.setDescription(description);
        workshop.setInstructor(instructor);
        workshop.setCategory(category);
        workshop.setMaxCapacity(capacity);
        workshop.setDateTime(date.atTime(time));
        workshop.setImageUrl(imageUrl);
        return workshop;
    }
}
