package com.lty.workorder.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台端与租户端登录接口测试。
 *
 * <p>覆盖登录成功、密码错误、租户不存在、失败锁定，以及鉴权拦截器的无 token、
 * 登录端不匹配等基础校验。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LoginApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** 平台端登录应返回 token 和用户信息。 */
    @Test
    void platformLoginShouldReturnTokenAndUserInfo() throws Exception {
        mockMvc.perform(post("/account/api/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tokenName").value("satoken"))
                .andExpect(jsonPath("$.data.tokenValue").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.username").value("admin"))
                .andExpect(jsonPath("$.data.userInfo.tenantId").value("0"))
                .andExpect(jsonPath("$.data.userInfo.nickname").value("平台管理员"));
    }

    /** 租户端登录应返回 token。 */
    @Test
    void tenantLoginShouldReturnToken() throws Exception {
        String token = loginTenant("T0001", "tenant_admin", "admin123");
        assertThat(token).isNotBlank();
    }

    /** 密码错误应返回 401。 */
    @Test
    void wrongPasswordShouldReturn401() throws Exception {
        mockMvc.perform(post("/account/api/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 租户不存在应返回 401。 */
    @Test
    void unknownTenantShouldReturn401() throws Exception {
        mockMvc.perform(post("/account/api/tenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode": "T9999",
                                  "username": "tenant_admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 连续 5 次密码错误后应锁定 15 分钟。 */
    @Test
    void fiveFailuresShouldLockLoginFor15Minutes() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/account/api/tenant/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "tenantCode": "T0001",
                                      "username": "lock_test",
                                      "password": "wrong-password"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        mockMvc.perform(post("/account/api/tenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode": "T0001",
                                  "username": "lock_test",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429));
    }

    /** 未携带 token 访问受保护接口应返回 401。 */
    @Test
    void requestWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/platform/api/tenant/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** token 与 client-code 不匹配应返回 401。 */
    @Test
    void wrongClientCodeShouldReturn401() throws Exception {
        String token = loginPlatform("admin", "admin123");

        mockMvc.perform(post("/platform/api/tenant/page")
                        .header("satoken", token)
                        .header("client-code", "tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    private String loginPlatform(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/account/api/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readToken(result);
    }

    private String loginTenant(String tenantCode, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/account/api/tenant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantCode": "%s",
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(tenantCode, username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readToken(result);
    }

    private String readToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("tokenValue").asString();
    }
}
