package com.gymcrm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymcrm.domain.TrainingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/trainees/" + username))
                .andExpect(status().isUnauthorized());
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
                            .content("{\"username\":\"" + username + "\",\"oldPassword\":\""
                                    + user.get("password").asText() + "\",\"newPassword\":\"brandNew123\"}"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(username, "brandNew123")))
                    .andExpect(status().isOk());
        }
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
        register("/trainees", "{\"firstName\":\"Metric\",\"lastName\":\"Check\"}");

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gym_registrations_total")))
                .andExpect(content().string(containsString("gym_authentications_total")))
                .andExpect(content().string(containsString("application=\"gym-crm\"")));
    }

    @Test
    void unknownResourceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }
}
