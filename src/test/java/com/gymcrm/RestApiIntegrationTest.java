package com.gymcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.domain.TrainingType;
import com.gymcrm.facade.GymFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("local")
class RestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GymFacade facade;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private JsonNode register(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String login(JsonNode credentials) throws Exception {
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(credentials.get("username").asText(), credentials.get("password").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(response).get("token").asText();
    }

    private static String registrationToken(JsonNode registration) {
        return "Bearer " + registration.get("token").asText();
    }

    @Test
    void loginReturnsTokenThatAuthorizesRequestsToOwnProfileOnly() throws Exception {
        JsonNode credentials = register("/trainees",
                "{\"firstName\":\"Rest\",\"lastName\":\"Trainee\",\"address\":\"Street 1\"}");
        String username = credentials.get("username").asText();
        JsonNode other = register("/trainees", "{\"firstName\":\"Rest\",\"lastName\":\"Other\"}");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "wrong")))
                .andExpect(status().isUnauthorized());

        String token = login(credentials);
        mockMvc.perform(get("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Rest"));
        mockMvc.perform(get("/trainees/" + other.get("username").asText()).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/trainees/" + username).header("Authorization", token + "tampered"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token signature is invalid"));
        mockMvc.perform(get("/trainees/" + username))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void onlyRegistrationAndLoginArePublic() throws Exception {
        String[] protectedGets = {"/trainees/Someone", "/trainees/Someone/trainings",
                "/trainees/Someone/unassigned-trainers", "/trainers/Someone", "/trainers/Someone/trainings",
                "/training-types", "/actuator/metrics"};
        for (String path : protectedGets) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(containsString("Authentication required")));
        }
        mockMvc.perform(put("/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"oldPassword\":\"b\",\"newPassword\":\"c\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/trainings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/trainees/Someone")).andExpect(status().isUnauthorized());

        register("/trainees", "{\"firstName\":\"Public\",\"lastName\":\"Trainee\"}");
        register("/trainers", "{\"firstName\":\"Public\",\"lastName\":\"Trainer\",\"specialization\":\"YOGA\"}");
    }

    @Test
    void registrationReturnsGeneratedCredentialsAndUsableToken() throws Exception {
        JsonNode registration = register("/trainers",
                "{\"firstName\":\"Fresh\",\"lastName\":\"Coach\",\"specialization\":\"YOGA\"}");
        String username = registration.get("username").asText();

        assertEquals("Fresh.Coach", username);
        assertEquals(10, registration.get("password").asText().length());
        assertEquals("Bearer", registration.get("tokenType").asText());
        mockMvc.perform(get("/trainers/" + username).header("Authorization", registrationToken(registration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Fresh"));
    }

    @Test
    void passwordsAreStoredAsSaltedHashes() throws Exception {
        JsonNode first = register("/trainees", "{\"firstName\":\"Hash\",\"lastName\":\"Check\"}");
        String rawPassword = first.get("password").asText();
        String storedHash = facade.getTrainee(first.get("username").asText()).orElseThrow().getPassword();

        assertNotEquals(rawPassword, storedHash);
        assertTrue(storedHash.startsWith("$2a$"));
        assertTrue(passwordEncoder.matches(rawPassword, storedHash));
        assertNotEquals(passwordEncoder.encode(rawPassword), passwordEncoder.encode(rawPassword));
    }

    @Test
    void passwordsAreNoLongerAcceptedAsRequestAuthentication() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Basic\",\"lastName\":\"Trainee\"}");
        String username = credentials.get("username").asText();
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + credentials.get("password").asText()).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/trainees/" + username).header("Authorization", basic))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void threeFailedLoginsBlockTheUserForFiveMinutes() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Brute\",\"lastName\":\"Force\"}");
        String username = credentials.get("username").asText();
        JsonNode bystander = register("/trainees", "{\"firstName\":\"Brute\",\"lastName\":\"Bystander\"}");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(username, "wrong" + i)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, credentials.get("password").asText())))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "300"))
                .andExpect(content().string("Too many failed login attempts. Try again in 5 minute(s)"));
        login(bystander);
    }

    @Test
    void successfulLoginResetsFailedAttempts() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Reset\",\"lastName\":\"Attempts\"}");
        String username = credentials.get("username").asText();

        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 2; i++) {
                mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(username, "wrong")))
                        .andExpect(status().isUnauthorized());
            }
            login(credentials);
        }
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Log\",\"lastName\":\"Out\"}");
        String username = credentials.get("username").asText();
        String token = login(credentials);
        String otherSession = login(credentials);

        mockMvc.perform(get("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/logout").header("Authorization", token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Token has been revoked"));
        mockMvc.perform(post("/auth/logout").header("Authorization", token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/trainees/" + username).header("Authorization", otherSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/trainees/" + username).header("Authorization", login(credentials)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Authentication required")));
    }

    @Test
    void corsPreflightIsAllowedForConfiguredOriginsOnly() throws Exception {
        mockMvc.perform(options("/trainees/Someone")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));

        mockMvc.perform(options("/trainees/Someone")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsHeadersAreAddedToActualRequests() throws Exception {
        mockMvc.perform(post("/trainees").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .content("{\"firstName\":\"Cors\",\"lastName\":\"Trainee\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void traineeCanAddTrainingWithTokenButTrainerTokenCannot() throws Exception {
        JsonNode trainer = register("/trainers",
                "{\"firstName\":\"Token\",\"lastName\":\"Coach\",\"specialization\":\"YOGA\"}");
        JsonNode trainee = register("/trainees", "{\"firstName\":\"Token\",\"lastName\":\"Student\"}");
        String body = "{\"traineeUsername\":\"" + trainee.get("username").asText() + "\",\"trainerUsername\":\""
                + trainer.get("username").asText() + "\",\"trainingName\":\"Morning\","
                + "\"trainingDate\":\"2030-01-01\",\"trainingDuration\":45}";

        mockMvc.perform(post("/trainings").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", login(trainer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/trainings").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", login(trainee)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/trainees/" + trainee.get("username").asText() + "/trainings")
                        .header("Authorization", login(trainee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Morning"));
    }

    @Test
    void traineeProfileCanBeUpdatedWithPut() throws Exception {
        JsonNode credentials = register("/trainees",
                "{\"firstName\":\"Put\",\"lastName\":\"Trainee\",\"address\":\"Old\"}");
        String username = credentials.get("username").asText();
        String token = login(credentials);

        mockMvc.perform(put("/trainees/" + username).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"firstName\":\"Putty\",\"lastName\":\"Trainee\","
                                + "\"address\":\"New\",\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Putty"))
                .andExpect(jsonPath("$.address").value("New"));
        mockMvc.perform(get("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Putty"))
                .andExpect(jsonPath("$.address").value("New"));
    }

    @Test
    void updatingTraineeWithPutKeepsTrainingsAndTrainers() throws Exception {
        JsonNode trainer = register("/trainers",
                "{\"firstName\":\"Keep\",\"lastName\":\"Coach\",\"specialization\":\"YOGA\"}");
        JsonNode trainee = register("/trainees", "{\"firstName\":\"Keep\",\"lastName\":\"Student\"}");
        String username = trainee.get("username").asText();
        String trainerUsername = trainer.get("username").asText();
        String token = login(trainee);

        mockMvc.perform(post("/trainings").contentType(MediaType.APPLICATION_JSON).header("Authorization", token)
                        .content("{\"traineeUsername\":\"" + username + "\",\"trainerUsername\":\"" + trainerUsername
                                + "\",\"trainingName\":\"Kept\",\"trainingDate\":\"2030-01-01\",\"trainingDuration\":30}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/trainees/" + username + "/trainers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("[\"" + trainerUsername + "\"]"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/trainees/" + username).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"firstName\":\"Keeper\",\"lastName\":\"Student\","
                                + "\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[0].username").value(trainerUsername));
        mockMvc.perform(get("/trainees/" + username + "/trainings").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value("Kept"));
    }

    @Test
    void trainerProfileCanBeUpdatedWithPut() throws Exception {
        JsonNode credentials = register("/trainers",
                "{\"firstName\":\"Put\",\"lastName\":\"Trainer\",\"specialization\":\"YOGA\"}");
        String username = credentials.get("username").asText();
        String token = login(credentials);

        mockMvc.perform(put("/trainers/" + username).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"firstName\":\"Putty\",\"lastName\":\"Trainer\","
                                + "\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Putty"));
        mockMvc.perform(get("/trainers/" + username).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Putty"));
    }

    @Test
    void traineeTrainersListCanBeUpdatedWithPut() throws Exception {
        JsonNode trainer = register("/trainers",
                "{\"firstName\":\"List\",\"lastName\":\"Coach\",\"specialization\":\"BOXING\"}");
        JsonNode trainee = register("/trainees", "{\"firstName\":\"List\",\"lastName\":\"Student\"}");
        String traineeUsername = trainee.get("username").asText();
        String token = login(trainee);

        mockMvc.perform(put("/trainees/" + traineeUsername + "/trainers").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + trainer.get("username").asText() + "\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainer.get("username").asText()));
    }

    @Test
    void passwordCanBeChangedWithPutForTraineeAndTrainer() throws Exception {
        JsonNode trainee = register("/trainees", "{\"firstName\":\"Pw\",\"lastName\":\"Trainee\"}");
        JsonNode trainer = register("/trainers",
                "{\"firstName\":\"Pw\",\"lastName\":\"Trainer\",\"specialization\":\"YOGA\"}");

        for (JsonNode user : new JsonNode[]{trainee, trainer}) {
            String username = user.get("username").asText();
            mockMvc.perform(put("/auth/password").contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", registrationToken(user))
                            .content("{\"username\":\"" + username + "\",\"oldPassword\":\""
                                    + user.get("password").asText() + "\",\"newPassword\":\"brandNew123\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(username, "brandNew123")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void passwordOfAnotherUserCannotBeChanged() throws Exception {
        JsonNode attacker = register("/trainees", "{\"firstName\":\"Pw\",\"lastName\":\"Attacker\"}");
        JsonNode victim = register("/trainees", "{\"firstName\":\"Pw\",\"lastName\":\"Victim\"}");

        mockMvc.perform(put("/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", registrationToken(attacker))
                        .content("{\"username\":\"" + victim.get("username").asText() + "\",\"oldPassword\":\""
                                + victim.get("password").asText() + "\",\"newPassword\":\"stolen123\"}"))
                .andExpect(status().isForbidden());
        login(victim);
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Gone\",\"lastName\":\"Soon\"}");
        String username = credentials.get("username").asText();
        String token = login(credentials);

        mockMvc.perform(delete("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/trainees/" + username).header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void trainingTypesRequireToken() throws Exception {
        JsonNode credentials = register("/trainers",
                "{\"firstName\":\"Rest\",\"lastName\":\"Trainer\",\"specialization\":\"YOGA\"}");

        mockMvc.perform(get("/training-types")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/training-types").header("Authorization", login(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingType").value("Yoga"));
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/trainees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/trainees").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointReportsCustomIndicators() throws Exception {
        register("/trainees", "{\"firstName\":\"Health\",\"lastName\":\"Check\"}");

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.gymDatabase.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingCatalog.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingCatalog.details.trainingTypes").value(TrainingType.values().length));
    }

    @Test
    void prometheusEndpointExposesCustomMetrics() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Metric\",\"lastName\":\"Check\"}");
        login(credentials);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gym_registrations_total")))
                .andExpect(content().string(containsString("gym_authentications_total")))
                .andExpect(content().string(containsString("application=\"gym-crm\"")));
    }

    @Test
    void unknownResourceReturnsNotFoundForAuthenticatedUser() throws Exception {
        JsonNode credentials = register("/trainees", "{\"firstName\":\"Lost\",\"lastName\":\"User\"}");

        mockMvc.perform(get("/does-not-exist").header("Authorization", registrationToken(credentials)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isUnauthorized());
    }
}
