package co.edu.unbosque.marketplace.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HostListingAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void onlyHostCanListOwnDraftsAndPaginationIsValidated() throws Exception {
        String host = register("HOST");
        String other = register("HOST");
        String guest = register("GUEST");
        String owned = createListing(host);
        createListing(other);

        mvc.perform(get("/api/host/listings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/host/listings").header("Authorization", bearer(guest))).andExpect(status().isForbidden());
        mvc.perform(get("/api/host/listings").header("Authorization", bearer(host)).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/host/listings").header("Authorization", bearer(host)).param("page", "-1"))
                .andExpect(status().isBadRequest());
        String response = mvc.perform(get("/api/host/listings").header("Authorization", bearer(host)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode result = mapper.readTree(response);
        assertEquals(1, result.get("totalElements").asInt());
        assertEquals(owned, result.get("content").get(0).get("id").asText());
        assertEquals("DRAFT", result.get("content").get(0).get("status").asText());
        String empty = mvc.perform(get("/api/host/listings").header("Authorization", bearer(host))
                        .param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(mapper.readTree(empty).get("content").isEmpty());
    }

    @Test
    void onlyHostCanCreateListingAndIsRecordedAsItsOwner() throws Exception {
        String host = register("HOST");
        String guest = register("GUEST");
        String admin = register("ADMIN");
        String body = mapper.writeValueAsString(Map.of("title", "Casa de prueba", "description", "Alojamiento de prueba",
                "city", "Bogotá", "location", "Chapinero", "type", "APARTMENT", "capacity", 4,
                "pricePerNight", 100000, "amenityIds", List.of("WIFI")));

        mvc.perform(post("/api/listings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/listings").header("Authorization", bearer(guest))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/listings").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        String profile = mvc.perform(get("/api/profile/me").header("Authorization", bearer(host)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String created = mvc.perform(post("/api/listings").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode listing = mapper.readTree(created);
        assertEquals(mapper.readTree(profile).get("id").asText(), listing.get("hostId").asText());
        String updated = mvc.perform(patch("/api/listings/" + listing.get("id").asText())
                        .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(listing.get("hostId").asText(), mapper.readTree(updated).get("hostId").asText());
    }

    private String register(String role) throws Exception {
        String body = mapper.writeValueAsString(Map.of("name", "Usuario", "email", UUID.randomUUID() + "@example.test",
                "password", "safe-password-123", "role", role));
        String response = mvc.perform(post("/api/auth/register").header("X-Admin-Bootstrap", "bootstrap-key-for-tests")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }

    private String createListing(String token) throws Exception {
        String body = mapper.writeValueAsString(Map.of("title", "Casa de prueba", "description", "Alojamiento de prueba",
                "city", "Bogotá", "location", "Chapinero", "type", "APARTMENT", "capacity", 4,
                "pricePerNight", 100000, "amenityIds", List.of("WIFI")));
        String response = mvc.perform(post("/api/listings").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("id").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
