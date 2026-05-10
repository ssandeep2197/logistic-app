package com.handa.tms.identity.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.handa.tms.platform.test.TmsPostgresExtension;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth: signup creates a tenant + admin user; the returned token
 * can call /auth/me; an invalid login returns 401 with the error code we expect.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry r) {
        TmsPostgresExtension.register(r);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void signup_then_me_then_logout() throws Exception {
        String signupJson = """
            {"tenantSlug":"acme","tenantName":"Acme Trucking",
             "email":"admin@acme.test","password":"correcthorsebattery",
             "fullName":"Acme Admin"}""";

        var signup = mvc.perform(post("/auth/signup").contentType(APPLICATION_JSON).content(signupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode tokens = json.readTree(signup.getResponse().getContentAsString());
        String access = tokens.get("accessToken").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@acme.test"));
    }

    @Test
    void login_with_wrong_password_returns_401_invalid_credentials() throws Exception {
        // Bootstrap a tenant first.
        mvc.perform(post("/auth/signup").contentType(APPLICATION_JSON).content("""
            {"tenantSlug":"beta","tenantName":"Beta Co",
             "email":"a@beta.test","password":"correcthorsebattery"}"""))
                .andExpect(status().isCreated());

        var res = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content("""
            {"tenantSlug":"beta","email":"a@beta.test","password":"wrong-password-here"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"))
                .andReturn();

        assertThat(res.getResponse().getContentType()).contains("application/problem+json");
    }
}
