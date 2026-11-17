package com.tallerescomunitarios.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerescomunitarios.backend.repository.RegistrationRepository;
import com.tallerescomunitarios.backend.repository.UserRepository;
import com.tallerescomunitarios.backend.repository.WorkshopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkshopControllerTest {
    private static final String WORKSHOP_JSON = """
            {"name":"Informática básica","description":"Uso inicial del computador", "instructor":"Ana Pérez", "category":"Tecnología", "maxCapacity":1, "dateTime":"2026-10-22T09:00:00"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private WorkshopRepository workshopRepository;
    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        registrationRepository.deleteAll();
        workshopRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsSearchesRegistersAndProtectsCapacity() throws Exception {
        String instructor = bearer(register("ana@talleres.co", "INSTRUCTOR", null, "TEST-CODE"));

        MvcResult creation = mockMvc.perform(post("/api/workshops").header(HttpHeaders.AUTHORIZATION, instructor)
                        .contentType(MediaType.APPLICATION_JSON).content(WORKSHOP_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableSlots").value(1))
                .andReturn();
        long workshopId = objectMapper.readTree(creation.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/workshops").param("name", "informática").header(HttpHeaders.AUTHORIZATION, instructor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Informática básica"));

        String updatedWorkshop = """
                {"name":"Informática práctica","description":"Uso del computador e internet", "instructor":"Ana Pérez", "category":"Tecnología", "maxCapacity":1, "dateTime":"2026-10-22T10:00:00"}
                """;
        mockMvc.perform(put("/api/workshops/{id}", workshopId).header(HttpHeaders.AUTHORIZATION, instructor)
                        .contentType(MediaType.APPLICATION_JSON).content(updatedWorkshop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Informática práctica"));

        mockMvc.perform(post("/api/registrations").header(HttpHeaders.AUTHORIZATION, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workshopId":%d,"beneficiaryName":"Carlos Gómez","beneficiaryDocument":"100200300"}
                                """.formatted(workshopId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/registrations").header(HttpHeaders.AUTHORIZATION, instructor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workshopId":%d,"beneficiaryName":"Laura Díaz","beneficiaryDocument":"999888777"}
                                """.formatted(workshopId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No hay cupos disponibles en este taller"));

        mockMvc.perform(delete("/api/workshops/{id}", workshopId).header(HttpHeaders.AUTHORIZATION, instructor))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsAnonymousRequestsAndWrongInstructorCode() throws Exception {
        mockMvc.perform(get("/api/workshops")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("intruso@talleres.co", "INSTRUCTOR", null, "codigo-falso")))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentsEnrollThemselvesButCannotManageWorkshops() throws Exception {
        String instructor = bearer(register("ana@talleres.co", "INSTRUCTOR", null, "TEST-CODE"));
        String student = bearer(register("luis@talleres.co", "STUDENT", "555666", null));

        mockMvc.perform(post("/api/workshops").header(HttpHeaders.AUTHORIZATION, student)
                        .contentType(MediaType.APPLICATION_JSON).content(WORKSHOP_JSON))
                .andExpect(status().isForbidden());

        MvcResult creation = mockMvc.perform(post("/api/workshops").header(HttpHeaders.AUTHORIZATION, instructor)
                        .contentType(MediaType.APPLICATION_JSON).content(WORKSHOP_JSON))
                .andExpect(status().isCreated()).andReturn();
        long workshopId = objectMapper.readTree(creation.getResponse().getContentAsString()).get("id").asLong();

        // El cuerpo intenta inscribir a otra persona: el backend usa los datos de la cuenta.
        mockMvc.perform(post("/api/registrations").header(HttpHeaders.AUTHORIZATION, student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workshopId":%d,"beneficiaryName":"Otra persona","beneficiaryDocument":"1"}
                                """.formatted(workshopId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.beneficiaryDocument").value("555666"));

        mockMvc.perform(get("/api/me/registrations").header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workshopId").value(workshopId));

        mockMvc.perform(get("/api/workshops/{id}/registrations", workshopId).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/workshops/{id}/registrations", workshopId).param("document", "555666")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private JsonNode register(String email, String role, String document, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, role, document, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String registerJson(String email, String role, String document, String code) throws Exception {
        var body = objectMapper.createObjectNode()
                .put("fullName", "Persona de prueba")
                .put("email", email)
                .put("password", "clave-segura-123")
                .put("role", role);
        if (document != null) body.put("document", document);
        if (code != null) body.put("instructorCode", code);
        return objectMapper.writeValueAsString(body);
    }

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.get("token").asText();
    }
}
