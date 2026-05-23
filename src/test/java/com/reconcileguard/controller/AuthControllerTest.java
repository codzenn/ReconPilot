package com.reconcileguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-test;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void signUpLoginAndAccessProtectedApi() throws Exception {
        String signUpBody = """
                {
                  "fullName": "Demo Operator",
                  "email": "demo@reconpilot.dev",
                  "password": "StrongPass1!",
                  "acceptTerms": true
                }
                """;

        String signUpResponse = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = objectMapper.readTree(signUpResponse).get("userId").asText();
        assertThat(userId).isNotBlank();

        String loginBody = """
                { "email": "demo@reconpilot.dev", "password": "StrongPass1!" }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String jwt = objectMapper.readTree(loginResponse).get("accessToken").asText();
        assertThat(jwt).isNotBlank();

        mockMvc.perform(get("/api/summary")
                        .header("Authorization", "Bearer " + jwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").isNumber());

        mockMvc.perform(get("/api/summary").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

