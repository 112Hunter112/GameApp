package com.parth.sportsapp.sportsbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test of the authentication flow through the full
 * Spring MVC stack (controllers + security filter chain + services + a real
 * Postgres). Redis/Redisson is mocked so the test needs only Docker for PostGIS.
 *
 * Covers: register, login, protected-endpoint authorization, refresh-token
 * rotation, logout invalidation, duplicate-registration, wrong password,
 * public endpoint access, and malformed-body rejection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthFlowIT {

  @Container
  static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
      DockerImageName.parse("postgis/postgis:15-3.4")
          .asCompatibleSubstituteFor("postgres"))
      .withInitScript("init-postgis.sql");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGIS::getUsername);
    registry.add("spring.datasource.password", POSTGIS::getPassword);
  }

  // No real Redis in the test; Redisson isn't exercised by the auth flow.
  @MockitoBean
  RedissonClient redissonClient;

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  private static final String PW = "Str0ng@Pass";

  @Test
  void registerLoginAccessRefresh_fullHappyPath() throws Exception {
    register("happy@example.com", "happyuser");

    JsonNode login = login("happy@example.com", PW);
    String access = login.get("token").asText();
    String refresh = login.get("refreshToken").asText();

    // Protected endpoint without a token must be rejected.
    mockMvc.perform(get("/api/users/me"))
        .andExpect(status().is4xxClientError());

    // Protected endpoint WITH a valid token succeeds.
    mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
        .andExpect(status().isOk());

    // Refresh returns a brand-new access token.
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken", refresh))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
  }

  @Test
  void logoutInvalidatesRefreshToken() throws Exception {
    register("logout@example.com", "logoutuser");
    JsonNode login = login("logout@example.com", PW);
    String refresh = login.get("refreshToken").asText();

    // Logout revokes the refresh token.
    mockMvc.perform(post("/api/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken", refresh))))
        .andExpect(status().isNoContent());

    // Using the revoked token to refresh must now fail.
    mockMvc.perform(post("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("refreshToken", refresh))))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void duplicateRegistrationIsRejected() throws Exception {
    register("dupe@example.com", "dupeuser");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody("dupe@example.com", "dupeuser2")))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void loginWithWrongPasswordIsRejected() throws Exception {
    register("wrongpw@example.com", "wrongpwuser");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", "wrongpw@example.com", "password", "Wr0ng@Pass"))))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void publicNearbyEndpointIsAccessibleWithoutAuth() throws Exception {
    mockMvc.perform(get("/api/venues/nearby")
            .param("latitude", "41.85")
            .param("longitude", "-87.65")
            .param("radiusMeters", "5000"))
        .andExpect(status().isOk());
  }

  @Test
  void unknownJsonFieldIsRejected() throws Exception {
    // fail-on-unknown-properties = true → smuggling an extra field is a 400.
    String body = json(Map.of(
        "email", "smuggle@example.com",
        "password", PW,
        "confirmPassword", PW,
        "username", "smuggler",
        "firstName", "S", "lastName", "M",
        "isAdmin", true  // not a field on RegisterRequest
    ));
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  // --- helpers -------------------------------------------------------------

  private void register(String email, String username) throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody(email, username)))
        .andExpect(status().isOk());
  }

  private JsonNode login(String email, String password) throws Exception {
    String body = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("email", email, "password", password))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(body);
  }

  private String registerBody(String email, String username) throws Exception {
    return json(Map.of(
        "email", email,
        "password", PW,
        "confirmPassword", PW,
        "username", username,
        "firstName", "Int",
        "lastName", "Test"));
  }

  private String json(Map<String, ?> map) throws Exception {
    return objectMapper.writeValueAsString(map);
  }
}
