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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户端菜单树与角色管理接口测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoleMenuApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** JDBC 查询工具，用于校验角色菜单关联。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** T0001 租户管理员 token。 */
    private String tenantAdminToken;

    /** 每个测试前登录 T0001 租户管理员。 */
    @BeforeEach
    void loginTenantAdmin() throws Exception {
        tenantAdminToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);
    }

    /** 菜单树应包含目录、菜单和按钮，按钮带权限串。 */
    @Test
    void menuTreeShouldIncludeButtonsForRoleAssignment() throws Exception {
        JsonNode response = postJson("/tenant/api/menu/tree", tenantAdminToken, "tenant", null);
        assertThat(response.path("code").asInt()).isZero();

        JsonNode menus = response.path("data");
        JsonNode system = findMenu(menus, "系统管理");
        JsonNode user = findMenu(menus, "用户管理");
        JsonNode create = findMenu(menus, "用户新增");
        assertThat(system).isNotNull();
        assertThat(user).isNotNull();
        assertThat(create).isNotNull();
        assertThat(user.path("parentUid").asString()).isEqualTo(system.path("uid").asString());
        assertThat(create.path("menuType").asString()).isEqualTo("F");
        assertThat(create.path("perms").asString()).isEqualTo("tenant:user:create");
    }

    /** 普通租户用户不能访问角色和菜单管理接口。 */
    @Test
    void tenantUserShouldNotAccessRoleOrMenuManagement() throws Exception {
        String userToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "tenant_user",
                  "password": "admin123"
                }
                """);

        assertThat(postJson("/tenant/api/menu/tree", userToken, "tenant", null).path("code").asInt())
                .isEqualTo(403);
        assertThat(postJson("/tenant/api/role/page", userToken, "tenant", """
                {"pageNum":1,"pageSize":10}
                """).path("code").asInt()).isEqualTo(403);
    }

    /** 角色分页应返回当前租户的租户端角色。 */
    @Test
    void rolePageShouldReturnTenantRoles() throws Exception {
        JsonNode data = rolePage(tenantAdminToken, null);
        assertThat(roleCodeExists(data, "tenant_admin")).isTrue();
        assertThat(roleCodeExists(data, "tenant_user")).isTrue();
    }

    /** 自定义角色应支持新增、编辑、重复校验和系统角色保护。 */
    @Test
    void createAndUpdateRoleShouldWork() throws Exception {
        String roleCode = "dispatcher_" + System.currentTimeMillis();
        String uid = createRole(roleCode, "调度员");

        JsonNode page = rolePage(tenantAdminToken, roleCode);
        assertThat(page.path("records").size()).isEqualTo(1);
        assertThat(page.path("records").get(0).path("roleName").asString()).isEqualTo("调度员");
        assertThat(page.path("records").get(0).path("status").asInt()).isEqualTo(1);

        JsonNode duplicate = postJson("/tenant/api/role/create", tenantAdminToken, "tenant", """
                {
                  "roleCode": "%s",
                  "roleName": "重复角色",
                  "dataScope": 2
                }
                """.formatted(roleCode));
        assertThat(duplicate.path("code").asInt()).isEqualTo(409);

        JsonNode update = postJson("/tenant/api/role/update", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "roleName": "高级调度员",
                  "dataScope": 2,
                  "status": 0
                }
                """.formatted(uid));
        assertThat(update.path("code").asInt()).isZero();

        JsonNode updated = rolePage(tenantAdminToken, roleCode);
        assertThat(updated.path("records").get(0).path("roleName").asString()).isEqualTo("高级调度员");
        assertThat(updated.path("records").get(0).path("status").asInt()).isZero();

        JsonNode invalidScope = postJson("/tenant/api/role/create", tenantAdminToken, "tenant", """
                {
                  "roleCode": "invalid_scope_%s",
                  "roleName": "非法数据范围",
                  "dataScope": 1
                }
                """.formatted(System.currentTimeMillis()));
        assertThat(invalidScope.path("code").asInt()).isEqualTo(400);

        String systemRoleUid = findRoleUid(rolePage(tenantAdminToken, "tenant_admin"), "tenant_admin");
        JsonNode systemUpdate = postJson("/tenant/api/role/update", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "roleName": "修改系统角色"
                }
                """.formatted(systemRoleUid));
        assertThat(systemUpdate.path("code").asInt()).isEqualTo(400);
    }

    /** 分配菜单应覆盖旧关联，无效菜单应整体回滚，系统角色不允许修改。 */
    @Test
    void bindMenusShouldReplaceRoleMenus() throws Exception {
        String roleCode = "menu_test_" + System.currentTimeMillis();
        String roleUid = createRole(roleCode, "菜单测试角色");
        Long roleId = roleId(roleUid);

        JsonNode first = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU0010", "MENU0011", "MENU0012"]
                }
                """.formatted(roleUid));
        assertThat(first.path("code").asInt()).isZero();
        assertThat(roleMenuCount(roleId)).isEqualTo(3);
        assertThat(rolePermissionCount(roleId, "tenant:user:page")).isEqualTo(1);

        JsonNode second = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU0020", "MENU0021"]
                }
                """.formatted(roleUid));
        assertThat(second.path("code").asInt()).isZero();
        assertThat(roleMenuCount(roleId)).isEqualTo(2);
        assertThat(rolePermissionCount(roleId, "tenant:user:page")).isZero();

        JsonNode invalid = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU_NOT_EXIST"]
                }
                """.formatted(roleUid));
        assertThat(invalid.path("code").asInt()).isEqualTo(400);
        assertThat(roleMenuCount(roleId)).isEqualTo(2);

        String systemRoleUid = findRoleUid(rolePage(tenantAdminToken, "tenant_admin"), "tenant_admin");
        JsonNode systemBind = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU0010"]
                }
                """.formatted(systemRoleUid));
        assertThat(systemBind.path("code").asInt()).isEqualTo(400);
    }

    /** 角色和菜单操作必须按租户隔离。 */
    @Test
    void roleMenusShouldBeTenantIsolated() throws Exception {
        String roleCode = "shared_code_" + System.currentTimeMillis();
        String t1RoleUid = createRole(roleCode, "T0001 自定义角色");

        String t2AdminToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0002",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);

        JsonNode crossTenantBind = postJson("/tenant/api/role/menus", t2AdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU0010"]
                }
                """.formatted(t1RoleUid));
        assertThat(crossTenantBind.path("code").asInt()).isEqualTo(404);

        JsonNode sameCodeInT2 = postJson("/tenant/api/role/create", t2AdminToken, "tenant", """
                {
                  "roleCode": "%s",
                  "roleName": "T0002 同名角色",
                  "dataScope": 2
                }
                """.formatted(roleCode));
        assertThat(sameCodeInT2.path("code").asInt()).isZero();

        JsonNode t2Page = rolePage(t2AdminToken, roleCode);
        assertThat(t2Page.path("records").size()).isEqualTo(1);
        assertThat(t2Page.path("records").get(0).path("roleName").asString())
                .isEqualTo("T0002 同名角色");
    }

    /** 角色菜单变更后，已登录用户的权限应立即生效。 */
    @Test
    void roleMenusShouldTakeEffectImmediately() throws Exception {
        String roleCode = "live_" + System.currentTimeMillis();
        String roleUid = createRole(roleCode, "即时权限角色");
        String username = "live_" + System.currentTimeMillis();
        JsonNode createUser = postJson("/tenant/api/user/create", tenantAdminToken, "tenant", """
                {
                  "username": "%s",
                  "nickname": "即时权限用户",
                  "roleUids": ["%s"]
                }
                """.formatted(username, roleUid));
        assertThat(createUser.path("code").asInt()).isZero();
        String password = createUser.path("data").path("initialPassword").asString();
        String userToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0001",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password));

        assertThat(postJson("/tenant/api/user/page", userToken, "tenant", """
                {"pageNum":1,"pageSize":10}
                """).path("code").asInt()).isEqualTo(403);

        JsonNode bind = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "menuUids": ["MENU0010", "MENU0011", "MENU0012"]
                }
                """.formatted(roleUid));
        assertThat(bind.path("code").asInt()).isZero();
        JsonNode auth = postJson("/account/api/tenant/user/auth", userToken, "tenant", null);
        assertThat(texts(auth.path("data").path("permissions"))).contains("tenant:user:page");
        assertThat(postJson("/tenant/api/user/page", userToken, "tenant", """
                {"pageNum":1,"pageSize":10}
                """).path("code").asInt()).isZero();

        JsonNode clear = postJson("/tenant/api/role/menus", tenantAdminToken, "tenant", """
                {"uid":"%s","menuUids":[]}
                """.formatted(roleUid));
        assertThat(clear.path("code").asInt()).isZero();
        assertThat(postJson("/tenant/api/user/page", userToken, "tenant", """
                {"pageNum":1,"pageSize":10}
                """).path("code").asInt()).isEqualTo(403);
    }

    /** 登录并返回 token。 */
    private String loginToken(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("code").asInt()).isZero();
        return json.path("data").path("tokenValue").asString();
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

    /** 查询角色分页数据。 */
    private JsonNode rolePage(String token, String roleCode) throws Exception {
        String body = roleCode == null
                ? "{\"pageNum\":1,\"pageSize\":20}"
                : "{\"pageNum\":1,\"pageSize\":20,\"roleCode\":\"%s\"}".formatted(roleCode);
        JsonNode response = postJson("/tenant/api/role/page", token, "tenant", body);
        assertThat(response.path("code").asInt()).isZero();
        return response.path("data");
    }

    /** 创建自定义角色并返回 uid。 */
    private String createRole(String roleCode, String roleName) throws Exception {
        JsonNode response = postJson("/tenant/api/role/create", tenantAdminToken, "tenant", """
                {
                  "roleCode": "%s",
                  "roleName": "%s",
                  "dataScope": 2
                }
                """.formatted(roleCode, roleName));
        assertThat(response.path("code").asInt()).isZero();
        return response.path("data").asString();
    }

    /** 将 JSON 字符串数组转换为 List。 */
    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(array.get(i).asString());
        }
        return values;
    }

    /** 递归查找菜单节点。 */
    private JsonNode findMenu(JsonNode menus, String menuName) {
        for (int i = 0; i < menus.size(); i++) {
            JsonNode menu = menus.get(i);
            if (menuName.equals(menu.path("menuName").asString())) {
                return menu;
            }
            JsonNode child = findMenu(menu.path("children"), menuName);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    /** 判断角色分页中是否包含指定角色编码。 */
    private boolean roleCodeExists(JsonNode page, String roleCode) {
        return findRoleUid(page, roleCode) != null;
    }

    /** 从角色分页中查找角色 uid。 */
    private String findRoleUid(JsonNode page, String roleCode) {
        JsonNode records = page.path("records");
        for (int i = 0; i < records.size(); i++) {
            JsonNode record = records.get(i);
            if (roleCode.equals(record.path("roleCode").asString())) {
                return record.path("uid").asString();
            }
        }
        return null;
    }

    /** 查询角色主键。 */
    private Long roleId(String roleUid) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE uid = ?", Long.class, roleUid);
    }

    /** 统计角色菜单关联数量。 */
    private Integer roleMenuCount(Long roleId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_menu WHERE tenant_id = 'T0001' AND role_id = ?",
                Integer.class, roleId);
    }

    /** 统计角色是否拥有指定权限。 */
    private Integer rolePermissionCount(Long roleId, String perms) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_role_menu rm
                JOIN sys_menu m ON rm.menu_id = m.id
                WHERE rm.tenant_id = 'T0001'
                  AND rm.role_id = ?
                  AND m.perms = ?
                """, Integer.class, roleId, perms);
    }
}
