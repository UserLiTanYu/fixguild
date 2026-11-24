package com.fixguild.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 退出登录与修改密码接口测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserAccountApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** 平台管理员登录 token。 */
    private String platformToken;

    /** 每个测试前登录平台管理员。 */
    @BeforeEach
    void loginAsPlatform() throws Exception {
        platformToken = loginToken("/account/api/platform/login", """
                {
                  "username": "admin",
                  "password": "admin123"
                }
                """);
    }

    /** 退出登录后旧 token 应立即失效。 */
    @Test
    void logoutShouldInvalidateCurrentToken() throws Exception {
        String token = loginTenantToken("T0001", "tenant_admin", "admin123");

        JsonNode logout = postJson("/account/api/tenant/logout", token, "tenant", null);
        assertThat(logout.path("code").asInt()).isZero();

        JsonNode info = postJson("/account/api/tenant/user/info", token, "tenant", null);
        assertThat(info.path("code").asInt()).isIn(401, 501);
    }

    /** 旧密码错误应返回 400，且当前登录态保持有效。 */
    @Test
    void wrongOldPasswordShouldReturn400AndKeepSession() throws Exception {
        String token = loginTenantToken("T0001", "tenant_user", "admin123");

        JsonNode change = postJson("/account/api/tenant/password/change", token, "tenant", """
                {
                  "oldPassword": "wrong-password",
                  "newPassword": "NewPass123"
                }
                """);
        assertThat(change.path("code").asInt()).isEqualTo(400);

        JsonNode info = postJson("/account/api/tenant/user/info", token, "tenant", null);
        assertThat(info.path("code").asInt()).isZero();
    }

    /** 新密码长度不足应返回 400，且不修改密码。 */
    @Test
    void newPasswordTooShortShouldReturn400() throws Exception {
        String token = loginTenantToken("T0001", "tenant_user", "admin123");

        JsonNode change = postJson("/account/api/tenant/password/change", token, "tenant", """
                {
                  "oldPassword": "admin123",
                  "newPassword": "1234567"
                }
                """);
        assertThat(change.path("code").asInt()).isEqualTo(400);

        JsonNode info = postJson("/account/api/tenant/user/info", token, "tenant", null);
        assertThat(info.path("code").asInt()).isZero();
    }

    /** 修改密码后当前 token 失效，旧密码不能登录，新密码可以登录。 */
    @Test
    void changePasswordShouldInvalidateTokenAndRequireNewPassword() throws Exception {
        String tenantCode = "P" + System.currentTimeMillis();
        JsonNode create = postJson("/platform/api/tenant/create", platformToken, "platform", """
                {
                  "tenantCode": "%s",
                  "tenantName": "修改密码测试租户",
                  "adminUsername": "tenant_admin"
                }
                """.formatted(tenantCode));
        assertThat(create.path("code").asInt()).isZero();
        String initialPassword = create.path("data").path("initialPassword").asString();

        String token = loginTenantToken(tenantCode, "tenant_admin", initialPassword);
        JsonNode change = postJson("/account/api/tenant/password/change", token, "tenant", """
                {
                  "oldPassword": "%s",
                  "newPassword": "NewPass123"
                }
                """.formatted(initialPassword));
        assertThat(change.path("code").asInt()).isZero();

        JsonNode info = postJson("/account/api/tenant/user/info", token, "tenant", null);
        assertThat(info.path("code").asInt()).isIn(401, 501);

        JsonNode oldLogin = postLogin("/account/api/tenant/login", """
                {
                  "tenantCode": "%s",
                  "username": "tenant_admin",
                  "password": "%s"
                }
                """.formatted(tenantCode, initialPassword));
        assertThat(oldLogin.path("code").asInt()).isEqualTo(401);

        JsonNode newLogin = postLogin("/account/api/tenant/login", """
                {
                  "tenantCode": "%s",
                  "username": "tenant_admin",
                  "password": "NewPass123"
                }
                """.formatted(tenantCode));
        assertThat(newLogin.path("code").asInt()).isZero();
    }

    /** 登录并返回 token。 */
    private String loginTenantToken(String tenantCode, String username, String password) throws Exception {
        return loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(tenantCode, username, password));
    }

    /** 登录并断言成功，返回 token。 */
    private String loginToken(String path, String body) throws Exception {
        JsonNode json = postLogin(path, body);
        assertThat(json.path("code").asInt()).isZero();
        return json.path("data").path("tokenValue").asString();
    }

    /** 调用登录接口并返回 JSON，不断言业务码。 */
    private JsonNode postLogin(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** 携带登录态调用接口并返回 JSON。 */
    private JsonNode postJson(String path, String token, String client, String body) throws Exception {
        MockHttpServletRequestBuilder request = post(path)
                .header("satoken", token)
                .header("client-code", client)
                .contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(body);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
