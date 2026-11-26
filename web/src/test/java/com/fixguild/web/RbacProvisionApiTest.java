package com.fixguild.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 种子数据和租户开通默认角色绑定测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RbacProvisionApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** JDBC 查询工具，用于直接校验租户关联表。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 平台管理员登录 token。 */
    private String platformToken;

    /** 每个测试前登录平台管理员。 */
    @BeforeEach
    void loginAsPlatform() throws Exception {
        MvcResult result = mockMvc.perform(post("/account/api/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        platformToken = json.path("data").path("tokenValue").asString();
    }

    /** 种子租户管理员应拥有角色和租户端权限。 */
    @Test
    void seedTenantAdminShouldHaveRoleAndPermission() {
        Integer roleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role
                WHERE tenant_id = 'T0001'
                  AND client_code = 'tenant'
                  AND role_code = 'tenant_admin'
                """, Integer.class);
        assertThat(roleCount).isEqualTo(1);

        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_menu rm
                JOIN sys_role r ON rm.role_id = r.id AND rm.tenant_id = r.tenant_id
                JOIN sys_menu m ON rm.menu_id = m.id
                WHERE rm.tenant_id = 'T0001'
                  AND r.role_code = 'tenant_admin'
                  AND m.perms = 'tenant:user:page'
                """, Integer.class);
        assertThat(permissionCount).isEqualTo(1);
    }

    /** 种子用户应绑定到对应租户角色。 */
    @Test
    void seedUserShouldBeBoundToRole() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_user u ON ur.user_id = u.id
                JOIN sys_role r ON ur.role_id = r.id AND ur.tenant_id = r.tenant_id
                WHERE ur.tenant_id = 'T0001'
                  AND u.username = 'tenant_admin'
                  AND r.role_code = 'tenant_admin'
                """, Integer.class);
        assertThat(count).isEqualTo(1);
    }

    /** 平台创建租户后应自动创建管理员角色、绑定菜单和用户角色。 */
    @Test
    void createdTenantShouldProvisionAdminRole() throws Exception {
        String tenantCode = "R" + System.currentTimeMillis();
        String adminUsername = "tenant_admin";

        MvcResult createResult = mockMvc.perform(platformPost("/platform/api/tenant/create", """
                        {
                          "tenantCode": "%s",
                          "tenantName": "RBAC 开通测试租户",
                          "adminUsername": "%s"
                        }
                        """.formatted(tenantCode, adminUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String adminUserUid = createJson.path("data").path("adminUserUid").asString();

        Long roleId = jdbcTemplate.queryForObject("""
                SELECT id FROM sys_role
                WHERE tenant_id = ?
                  AND client_code = 'tenant'
                  AND role_code = 'tenant_admin'
                """, Long.class, tenantCode);
        assertThat(roleId).isNotNull();

        Integer userRoleCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_user u ON ur.user_id = u.id
                WHERE ur.tenant_id = ?
                  AND ur.role_id = ?
                  AND u.uid = ?
                """, Integer.class, tenantCode, roleId, adminUserUid);
        assertThat(userRoleCount).isEqualTo(1);

        Integer expectedMenuCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_menu
                WHERE client_code = 'tenant' AND status = 1
                """, Integer.class);
        Integer actualMenuCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_role_menu WHERE tenant_id = ? AND role_id = ?
                """, Integer.class, tenantCode, roleId);
        assertThat(actualMenuCount).isEqualTo(expectedMenuCount);

        Integer permissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_menu rm
                JOIN sys_menu m ON rm.menu_id = m.id
                WHERE rm.tenant_id = ?
                  AND rm.role_id = ?
                  AND m.perms = 'tenant:user:create'
                """, Integer.class, tenantCode, roleId);
        assertThat(permissionCount).isEqualTo(1);
    }

    /** 构造平台端 POST 请求。 */
    private MockHttpServletRequestBuilder platformPost(String path, String body) {
        return post(path)
                .header("satoken", platformToken)
                .header("client-code", "platform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
