package co.edu.unbosque.marketplace.listing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequestValidationAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void unicodePasswordsRespectBcryptByteLimitOnRegistrationAndLogin() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String exactLimit = "á".repeat(36);
        String tooLong = exactLimit + "á";
        String badRegistration = mapper.writeValueAsString(Map.of("name", "Usuario", "email", email,
                "password", tooLong, "role", "GUEST"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(badRegistration))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        String goodRegistration = mapper.writeValueAsString(Map.of("name", "Usuario", "email", email,
                "password", exactLimit, "role", "GUEST"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(goodRegistration))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", exactLimit))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", tooLong))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidNestedValuesAndPricesReturnBadRequestWithoutChangingListing() throws Exception {
        String host = register("HOST");
        String guest = register("GUEST");
        String base = "\"title\":\"Casa\",\"description\":\"Desc\",\"city\":\"Bogotá\","
                + "\"location\":\"Centro\",\"type\":\"ROOM\",\"capacity\":2,";
        for (String fields : new String[] {
                "\"pricePerNight\":10000000000",
                "\"pricePerNight\":100.001",
                "\"pricePerNight\":100000,\"amenityIds\":[null]",
                "\"pricePerNight\":100000,\"amenityIds\":[\"\" ]",
                "\"pricePerNight\":100000,\"imageReferences\":[null]",
                "\"pricePerNight\":100000,\"imageReferences\":[\"" + "x".repeat(1001) + "\"]",
                "\"pricePerNight\":100000,\"extraOptions\":[null]",
                "\"pricePerNight\":100000,\"extraOptions\":[{\"name\":\"Servicio\",\"unitPrice\":10000000000}]",
                "\"pricePerNight\":100000,\"cancellationPolicy\":{\"freeRefundDays\":7,\"partialRefundDays\":2,\"partialRefundPercentage\":50.001}"
        }) {
            mvc.perform(post("/api/listings").header("Authorization", bearer(host))
                            .contentType(MediaType.APPLICATION_JSON).content("{" + base + fields + "}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        String listing = mvc.perform(post("/api/listings").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content("{" + base + "\"pricePerNight\":100000}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(listing).get("id").asText();
        mvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"periods\":[null]}"))
                .andExpect(status().isBadRequest());
        String tomorrow = LocalDate.now(java.time.ZoneId.of("America/Bogota")).plusDays(1).toString();
        String checkout = LocalDate.now(java.time.ZoneId.of("America/Bogota")).plusDays(2).toString();
        String availability = "{\"periods\":[{\"startDate\":\"" + tomorrow + "\",\"endDate\":\"" + checkout
                + "\",\"type\":\"AVAILABLE\"}]}";
        mvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content(availability))
                .andExpect(status().isOk());
        mvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/listings/" + id + "/availability").header("Authorization", bearer(host)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].startDate").value(tomorrow));

        String dates = "\"checkIn\":\"" + tomorrow + "\",\"checkOut\":\"" + checkout
                + "\",\"guests\":2,\"extras\":[null]";
        mvc.perform(post("/api/listings/" + id + "/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content("{" + dates + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations").header("Authorization", bearer(guest))
                        .header("Idempotency-Key", "validation-test").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":\"" + id + "\"," + dates + "}"))
                .andExpect(status().isBadRequest());
    }

    private String register(String role) throws Exception {
        String body = mapper.writeValueAsString(Map.of("name", "Usuario", "email", UUID.randomUUID() + "@example.test",
                "password", "safe-password-123", "role", role));
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }
}
