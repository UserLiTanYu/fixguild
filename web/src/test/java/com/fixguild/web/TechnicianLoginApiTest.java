package com.fixguild.web;

import com.fixguild.account.security.LoginAttemptGuard;
import com.fixguild.common.enums.ClientCode;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 技师端登录与技师端权限测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TechnicianLoginApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** 登录失败锁定器，测试后清理，避免影响其他用例。 */
    @Autowired
    private LoginAttemptGuard loginAttemptGuard;

    /** 清理测试产生的登录失败记录。 */
    @AfterEach
    void clearLoginFailures() {
        loginAttemptGuard.clear(ClientCode.TECHNICIAN, "T0001", "technician");
        loginAttemptGuard.clear(ClientCode.TECHNICIAN, "T0002", "technician");
    }

    /** 技师登录后应能获取技师端权限和菜单。 */
    @Test
    void technicianLoginShouldReturnTokenAndTechnicianAuth() throws Exception {
        JsonNode login = postLogin("/account/api/technician/login", """
                {
                  "tenantCode": "T0001",
                  "username": "technician",
                  "password": "admin123"
                }
                """);
        assertThat(login.path("code").asInt()).isZero();
        assertThat(login.path("data").path("userInfo").path("clientCode").asString())
                .isEqualTo("technician");
        assertThat(login.path("data").path("userInfo").path("tenantId").asString())
                .isEqualTo("T0001");
        String token = login.path("data").path("tokenValue").asString();

        JsonNode info = postJson("/account/api/technician/user/info", token, "technician", null);
        assertThat(info.path("code").asInt()).isZero();
        assertThat(info.path("data").path("username").asString()).isEqualTo("technician");
        assertThat(info.path("data").path("clientCode").asString()).isEqualTo("technician");
        assertThat(info.path("data").path("tenantId").asString()).isEqualTo("T0001");

        JsonNode auth = postJson("/account/api/technician/user/auth", token, "technician", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("technician");
        assertThat(texts(auth.path("data").path("permissions")))
                .contains("technician:work-order:page");
        assertThat(containsMenu(auth.path("data").path("menus"), "我的工单")).isTrue();
        assertThat(containsMenu(auth.path("data").path("menus"), "系统管理")).isFalse();
    }

    /** 技师 token 与 client-code 不匹配应返回 401。 */
    @Test
    void technicianTokenWithWrongClientCodeShouldReturn401() throws Exception {
        String token = loginToken("/account/api/technician/login", """
                {
                  "tenantCode": "T0001",
                  "username": "technician",
                  "password": "admin123"
                }
                """);

        JsonNode response = postJson("/account/api/technician/user/info", token, "tenant", null);
        assertThat(response.path("code").asInt()).isEqualTo(401);
    }

    /** 账号不能跨登录端登录。 */
    @Test
    void accountsShouldNotCrossLoginEndpoints() throws Exception {
        JsonNode technicianOnTenant = postLogin("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "technician",
                  "password": "admin123"
                }
                """);
        assertThat(technicianOnTenant.path("code").asInt()).isEqualTo(401);

        JsonNode tenantOnTechnician = postLogin("/account/api/technician/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);
        assertThat(tenantOnTechnician.path("code").asInt()).isEqualTo(401);
    }

    /** 两个租户的技师都应能登录，但租户上下文正确隔离。 */
    @Test
    void technicianLoginShouldKeepTenantContext() throws Exception {
        String token = loginToken("/account/api/technician/login", """
                {
                  "tenantCode": "T0002",
                  "username": "technician",
                  "password": "admin123"
                }
                """);

        JsonNode auth = postJson("/account/api/technician/user/auth", token, "technician", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("technician");
        assertThat(containsMenu(auth.path("data").path("menus"), "我的工单")).isTrue();

        JsonNode info = postJson("/account/api/technician/user/info", token, "technician", null);
        assertThat(info.path("data").path("tenantId").asString()).isEqualTo("T0002");
    }

    /** 技师密码错误应返回 401。 */
    @Test
    void technicianWrongPasswordShouldReturn401() throws Exception {
        JsonNode response = postLogin("/account/api/technician/login", """
                {
                  "tenantCode": "T0001",
                  "username": "technician",
                  "password": "wrong-password"
                }
                """);
        assertThat(response.path("code").asInt()).isEqualTo(401);
    }

    /** 登录并返回 token。 */
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

    /** 将 JSON 字符串数组转换为 List。 */
    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(array.get(i).asString());
        }
        return values;
    }

    /** 递归判断菜单树中是否包含指定名称。 */
    private boolean containsMenu(JsonNode menus, String menuName) {
        for (int i = 0; i < menus.size(); i++) {
            JsonNode menu = menus.get(i);
            if (menuName.equals(menu.path("menuName").asString())) {
                return true;
            }
            if (containsMenu(menu.path("children"), menuName)) {
                return true;
            }
        }
        return false;
    }
}
