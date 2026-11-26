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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前用户与 RBAC 权限接口测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthApiTest {

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

    /** 平台管理员应拿到平台用户信息和平台权限。 */
    @Test
    void platformAuthShouldReturnPlatformPermissions() throws Exception {
        JsonNode info = postJson("/account/api/platform/user/info", platformToken, "platform", null);
        assertThat(info.path("code").asInt()).isZero();
        assertThat(info.path("data").path("tenantId").asString()).isEqualTo("0");
        assertThat(info.path("data").path("username").asString()).isEqualTo("admin");

        JsonNode auth = postJson("/account/api/platform/user/auth", platformToken, "platform", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("platform_admin");
        assertThat(texts(auth.path("data").path("permissions"))).contains("platform:tenant:page");
        assertThat(auth.path("data").path("menus").size()).isPositive();
        assertThat(containsMenu(auth.path("data").path("menus"), "租户管理")).isTrue();
    }

    /** 租户管理员应拿到租户端权限和菜单。 */
    @Test
    void tenantAdminAuthShouldReturnTenantPermissions() throws Exception {
        String token = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);

        JsonNode info = postJson("/account/api/tenant/user/info", token, "tenant", null);
        assertThat(info.path("code").asInt()).isZero();
        assertThat(info.path("data").path("tenantId").asString()).isEqualTo("T0001");
        assertThat(info.path("data").path("username").asString()).isEqualTo("tenant_admin");

        JsonNode auth = postJson("/account/api/tenant/user/auth", token, "tenant", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("tenant_admin");
        assertThat(texts(auth.path("data").path("permissions")))
                .contains("tenant:user:create", "tenant:role:menus",
                        "tenant:user:delete", "tenant:role:delete");
        assertThat(containsMenu(auth.path("data").path("menus"), "系统管理")).isTrue();
        assertThat(containsMenu(auth.path("data").path("menus"), "用户管理")).isTrue();
    }

    /** 租户普通用户只应拿到受限权限。 */
    @Test
    void tenantUserAuthShouldBeLimited() throws Exception {
        String token = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_user",
                  "password": "admin123"
                }
                """);

        JsonNode auth = postJson("/account/api/tenant/user/auth", token, "tenant", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("tenant_user");
        assertThat(texts(auth.path("data").path("permissions"))).contains("tenant:user:page");
        assertThat(texts(auth.path("data").path("permissions")))
                .doesNotContain("tenant:user:create", "tenant:role:page",
                        "tenant:user:delete", "tenant:role:delete");
    }

    /** 新开通租户的初始管理员应自动获得租户权限。 */
    @Test
    void newTenantAdminShouldGetProvisionedAuth() throws Exception {
        String tenantCode = "A" + System.currentTimeMillis();
        JsonNode create = postJson("/platform/api/tenant/create", platformToken, "platform", """
                {
                  "tenantCode": "%s",
                  "tenantName": "权限测试租户",
                  "adminUsername": "tenant_admin"
                }
                """.formatted(tenantCode));
        assertThat(create.path("code").asInt()).isZero();
        String initialPassword = create.path("data").path("initialPassword").asString();

        String token = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "%s",
                  "username": "tenant_admin",
                  "password": "%s"
                }
                """.formatted(tenantCode, initialPassword));

        JsonNode auth = postJson("/account/api/tenant/user/auth", token, "tenant", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("tenant_admin");
        assertThat(texts(auth.path("data").path("permissions")))
                .contains("tenant:user:create", "tenant:user:delete", "tenant:role:delete");
        assertThat(containsMenu(auth.path("data").path("menus"), "系统管理")).isTrue();
    }

    /** 租户 token 访问平台接口应返回 403。 */
    @Test
    void tenantTokenShouldBeForbiddenOnPlatformApi() throws Exception {
        String token = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);

        JsonNode response = postJson("/platform/api/tenant/page", token, "tenant", """
                {
                  "pageNum": 1,
                  "pageSize": 10
                }
                """);
        assertThat(response.path("code").asInt()).isEqualTo(403);
    }

    /** 路径登录端与 token 登录端不一致应返回 401。 */
    @Test
    void clientPathMismatchShouldReturn401() throws Exception {
        String token = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);

        JsonNode response = postJson("/account/api/platform/user/info", token, "tenant", null);
        assertThat(response.path("code").asInt()).isEqualTo(401);
    }

    /** 登录并返回 token。 */
    private String loginToken(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("tokenValue").asString();
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
