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
 * 租户用户管理与用户角色绑定测试。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TenantUserApiTest {

    /** MockMvc 测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** JSON 序列化工具。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** JDBC 查询工具，用于校验逻辑删除字段。 */
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

    /** 用户分页应返回角色信息。 */
    @Test
    void userPageShouldReturnRoles() throws Exception {
        JsonNode page = userPage(tenantAdminToken, "tenant_admin");
        assertThat(page.path("records").size()).isEqualTo(1);
        JsonNode record = page.path("records").get(0);
        assertThat(texts(record.path("roleUids")))
                .contains(roleUid(tenantAdminToken, "tenant_admin"));
        assertThat(texts(record.path("roleNames"))).contains("租户管理员");
        assertThat(record.path("status").asInt()).isEqualTo(1);
        // 技师账号不属于租户用户管理范围
        assertThat(userPage(tenantAdminToken, "technician").path("records").size()).isZero();
    }

    /** 创建用户后应能使用初始密码登录，并获得绑定角色权限。 */
    @Test
    void createUserShouldBindRolesAndLogin() throws Exception {
        String tenantUserRoleUid = roleUid(tenantAdminToken, "tenant_user");
        String username = "user_" + System.currentTimeMillis();

        JsonNode create = createUser(tenantAdminToken, username, "普通用户", tenantUserRoleUid);
        assertThat(create.path("code").asInt()).isZero();
        assertThat(create.path("data").path("username").asString()).isEqualTo(username);
        String initialPassword = create.path("data").path("initialPassword").asString();
        assertThat(initialPassword).isNotBlank();

        String userToken = loginTenantToken("T0001", username, initialPassword);
        JsonNode auth = postJson("/account/api/tenant/user/auth", userToken, "tenant", null);
        assertThat(auth.path("code").asInt()).isZero();
        assertThat(texts(auth.path("data").path("roles"))).contains("tenant_user");
        assertThat(texts(auth.path("data").path("permissions"))).contains("tenant:user:page");

        JsonNode duplicate = createUser(tenantAdminToken, username, "重复用户", tenantUserRoleUid);
        assertThat(duplicate.path("code").asInt()).isEqualTo(409);
    }

    /** 编辑用户应更新资料并替换角色。 */
    @Test
    void updateUserShouldReplaceRoles() throws Exception {
        String tenantUserRoleUid = roleUid(tenantAdminToken, "tenant_user");
        String tenantAdminRoleUid = roleUid(tenantAdminToken, "tenant_admin");
        String username = "edit_" + System.currentTimeMillis();
        JsonNode create = createUser(tenantAdminToken, username, "待编辑用户", tenantUserRoleUid);
        String uid = create.path("data").path("uid").asString();
        String initialPassword = create.path("data").path("initialPassword").asString();

        JsonNode update = postJson("/tenant/api/user/update", tenantAdminToken, "tenant", """
                {
                  "uid": "%s",
                  "nickname": "已编辑用户",
                  "email": "edited@example.com",
                  "roleUids": ["%s"]
                }
                """.formatted(uid, tenantAdminRoleUid));
        assertThat(update.path("code").asInt()).isZero();

        JsonNode page = userPage(tenantAdminToken, username);
        JsonNode record = page.path("records").get(0);
        assertThat(record.path("nickname").asString()).isEqualTo("已编辑用户");
        assertThat(record.path("email").asString()).isEqualTo("edited@example.com");
        assertThat(texts(record.path("roleUids"))).contains(tenantAdminRoleUid);

        String userToken = loginTenantToken("T0001", username, initialPassword);
        JsonNode auth = postJson("/account/api/tenant/user/auth", userToken, "tenant", null);
        assertThat(texts(auth.path("data").path("roles"))).contains("tenant_admin");
        assertThat(texts(auth.path("data").path("permissions"))).contains("tenant:user:create");

        JsonNode noFields = postJson("/tenant/api/user/update", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid));
        assertThat(noFields.path("code").asInt()).isEqualTo(400);
    }

    /** 删除用户应逻辑删除并允许复用登录名。 */
    @Test
    void deleteUserShouldKeepRowAndAllowUsernameReuse() throws Exception {
        String tenantUserRoleUid = roleUid(tenantAdminToken, "tenant_user");
        String username = "delete_" + System.currentTimeMillis();
        JsonNode create = createUser(tenantAdminToken, username, "待删除用户", tenantUserRoleUid);
        String uid = create.path("data").path("uid").asString();
        String initialPassword = create.path("data").path("initialPassword").asString();
        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE uid = ?", Long.class, uid);

        JsonNode delete = postJson("/tenant/api/user/delete", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid));
        assertThat(delete.path("code").asInt()).isZero();

        assertThat(userPage(tenantAdminToken, username).path("records").size()).isZero();
        Integer isDelete = jdbcTemplate.queryForObject(
                "SELECT is_delete FROM sys_user WHERE id = ?", Integer.class, userId);
        Long deleteSeq = jdbcTemplate.queryForObject(
                "SELECT delete_seq FROM sys_user WHERE id = ?", Long.class, userId);
        assertThat(isDelete).isEqualTo(1);
        assertThat(deleteSeq).isEqualTo(userId);

        assertThat(postLogin("/account/api/tenant/login", """
                {"tenantCode":"T0001","username":"%s","password":"%s"}
                """.formatted(username, initialPassword)).path("code").asInt()).isEqualTo(401);

        JsonNode recreate = createUser(tenantAdminToken, username, "复用登录名", tenantUserRoleUid);
        assertThat(recreate.path("code").asInt()).isZero();
        assertThat(recreate.path("data").path("uid").asString()).isNotEqualTo(uid);

        JsonNode deleteAgain = postJson("/tenant/api/user/delete", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid));
        assertThat(deleteAgain.path("code").asInt()).isEqualTo(404);
    }

    /** 不能删除当前登录账号。 */
    @Test
    void deleteSelfShouldFail() throws Exception {
        String adminUid = findUserUid(userPage(tenantAdminToken, "tenant_admin"), "tenant_admin");
        JsonNode delete = postJson("/tenant/api/user/delete", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(adminUid));
        assertThat(delete.path("code").asInt()).isEqualTo(400);
        assertThat(delete.path("msg").asString()).contains("不能删除当前登录账号");
    }

    /** 重置密码、停用用户应让目标用户登录态立即失效。 */
    @Test
    void resetPasswordAndStatusShouldInvalidateTargetSession() throws Exception {
        String tenantUserRoleUid = roleUid(tenantAdminToken, "tenant_user");
        String username = "reset_" + System.currentTimeMillis();
        JsonNode create = createUser(tenantAdminToken, username, "重置测试用户", tenantUserRoleUid);
        String uid = create.path("data").path("uid").asString();
        String initialPassword = create.path("data").path("initialPassword").asString();
        String userToken = loginTenantToken("T0001", username, initialPassword);

        JsonNode reset = postJson("/tenant/api/user/reset-password", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid));
        assertThat(reset.path("code").asInt()).isZero();
        String newPassword = reset.path("data").path("newPassword").asString();
        assertThat(newPassword).isNotBlank();
        assertThat(postJson("/account/api/tenant/user/info", userToken, "tenant", null)
                .path("code").asInt()).isIn(401, 501);
        assertThat(postLogin("/account/api/tenant/login", """
                {"tenantCode":"T0001","username":"%s","password":"%s"}
                """.formatted(username, initialPassword)).path("code").asInt()).isEqualTo(401);

        String newToken = loginTenantToken("T0001", username, newPassword);
        JsonNode disable = postJson("/tenant/api/user/status", tenantAdminToken, "tenant", """
                {"uid":"%s","status":0}
                """.formatted(uid));
        assertThat(disable.path("code").asInt()).isZero();
        assertThat(postJson("/account/api/tenant/user/info", newToken, "tenant", null)
                .path("code").asInt()).isIn(401, 501);
        assertThat(postLogin("/account/api/tenant/login", """
                {"tenantCode":"T0001","username":"%s","password":"%s"}
                """.formatted(username, newPassword)).path("code").asInt()).isEqualTo(401);

        JsonNode enable = postJson("/tenant/api/user/status", tenantAdminToken, "tenant", """
                {"uid":"%s","status":1}
                """.formatted(uid));
        assertThat(enable.path("code").asInt()).isZero();
        loginTenantToken("T0001", username, newPassword);

        String adminUid = findUserUid(userPage(tenantAdminToken, "tenant_admin"), "tenant_admin");
        assertThat(postJson("/tenant/api/user/reset-password", tenantAdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(adminUid)).path("code").asInt()).isEqualTo(400);
        assertThat(postJson("/tenant/api/user/status", tenantAdminToken, "tenant", """
                {"uid":"%s","status":0}
                """.formatted(adminUid)).path("code").asInt()).isEqualTo(400);
    }

    /** 创建用户应受 max_user_count 限制。 */
    @Test
    void maxUserCountShouldBeEnforced() throws Exception {
        String platformToken = loginToken("/account/api/platform/login", """
                {
                  "username": "admin",
                  "password": "admin123"
                }
                """);
        String tenantCode = "M" + System.currentTimeMillis();
        JsonNode tenantCreate = postJson("/platform/api/tenant/create", platformToken, "platform", """
                {
                  "tenantCode": "%s",
                  "tenantName": "用户上限测试租户",
                  "maxUserCount": 2,
                  "adminUsername": "tenant_admin"
                }
                """.formatted(tenantCode));
        assertThat(tenantCreate.path("code").asInt()).isZero();
        String adminPassword = tenantCreate.path("data").path("initialPassword").asString();
        String adminToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "%s",
                  "username": "tenant_admin",
                  "password": "%s"
                }
                """.formatted(tenantCode, adminPassword));
        String roleUid = roleUid(adminToken, "tenant_admin");

        JsonNode first = createUser(adminToken, "limit_1", "上限用户一", roleUid);
        assertThat(first.path("code").asInt()).isZero();
        JsonNode second = createUser(adminToken, "limit_2", "上限用户二", roleUid);
        assertThat(second.path("code").asInt()).isEqualTo(400);
    }

    /** 普通租户用户只能查看用户，不能执行管理操作。 */
    @Test
    void tenantUserShouldNotManageUsers() throws Exception {
        String userToken = loginTenantToken("T0001", "tenant_user", "admin123");

        assertThat(postJson("/tenant/api/user/page", userToken, "tenant", """
                {"pageNum":1,"pageSize":10}
                """).path("code").asInt()).isZero();
        assertThat(postJson("/tenant/api/user/create", userToken, "tenant", """
                {"username":"x","nickname":"x","roleUids":["x"]}
                """).path("code").asInt()).isEqualTo(403);
        assertThat(postJson("/tenant/api/user/update", userToken, "tenant", """
                {"uid":"x","nickname":"x"}
                """).path("code").asInt()).isEqualTo(403);
        assertThat(postJson("/tenant/api/user/reset-password", userToken, "tenant", """
                {"uid":"x"}
                """).path("code").asInt()).isEqualTo(403);
        assertThat(postJson("/tenant/api/user/status", userToken, "tenant", """
                {"uid":"x","status":0}
                """).path("code").asInt()).isEqualTo(403);
        assertThat(postJson("/tenant/api/user/delete", userToken, "tenant", """
                {"uid":"x"}
                """).path("code").asInt()).isEqualTo(403);
    }

    /** 用户管理必须按租户隔离。 */
    @Test
    void userManagementShouldBeTenantIsolated() throws Exception {
        String tenantUserRoleUid = roleUid(tenantAdminToken, "tenant_user");
        String username = "isolated_" + System.currentTimeMillis();
        JsonNode create = createUser(tenantAdminToken, username, "隔离测试用户", tenantUserRoleUid);
        String uid = create.path("data").path("uid").asString();

        String t2AdminToken = loginToken("/account/api/tenant/login", """
                {
                  "tenantCode": "T0002",
                  "username": "tenant_admin",
                  "password": "admin123"
                }
                """);
        assertThat(userPage(t2AdminToken, username).path("records").size()).isZero();
        assertThat(postJson("/tenant/api/user/update", t2AdminToken, "tenant", """
                {"uid":"%s","nickname":"越权"}
                """.formatted(uid)).path("code").asInt()).isEqualTo(404);
        assertThat(postJson("/tenant/api/user/reset-password", t2AdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid)).path("code").asInt()).isEqualTo(404);
        assertThat(postJson("/tenant/api/user/delete", t2AdminToken, "tenant", """
                {"uid":"%s"}
                """.formatted(uid)).path("code").asInt()).isEqualTo(404);
        JsonNode invalidRole = postJson("/tenant/api/user/create", t2AdminToken, "tenant", """
                {
                  "username": "bad_role_%s",
                  "nickname": "错误角色",
                  "roleUids": ["%s"]
                }
                """.formatted(System.currentTimeMillis(), tenantUserRoleUid));
        assertThat(invalidRole.path("code").asInt()).isEqualTo(400);
    }

    /** 创建用户并返回响应 JSON。 */
    private JsonNode createUser(String token, String username, String nickname, String roleUid) throws Exception {
        return postJson("/tenant/api/user/create", token, "tenant", """
                {
                  "username": "%s",
                  "nickname": "%s",
                  "email": "%s@example.com",
                  "roleUids": ["%s"]
                }
                """.formatted(username, nickname, username, roleUid));
    }

    /** 查询用户分页数据。 */
    private JsonNode userPage(String token, String username) throws Exception {
        JsonNode response = postJson("/tenant/api/user/page", token, "tenant", """
                {"pageNum":1,"pageSize":20,"username":"%s"}
                """.formatted(username));
        assertThat(response.path("code").asInt()).isZero();
        return response.path("data");
    }

    /** 从角色分页中取角色 uid。 */
    private String roleUid(String token, String roleCode) throws Exception {
        JsonNode response = postJson("/tenant/api/role/page", token, "tenant", """
                {"pageNum":1,"pageSize":20,"roleCode":"%s"}
                """.formatted(roleCode));
        assertThat(response.path("code").asInt()).isZero();
        JsonNode records = response.path("data").path("records");
        assertThat(records.size()).isEqualTo(1);
        return records.get(0).path("uid").asString();
    }

    /** 从用户分页中取用户 uid。 */
    private String findUserUid(JsonNode page, String username) {
        JsonNode records = page.path("records");
        for (int i = 0; i < records.size(); i++) {
            if (username.equals(records.get(i).path("username").asString())) {
                return records.get(i).path("uid").asString();
            }
        }
        throw new AssertionError("用户不存在: " + username);
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

    /** 将 JSON 字符串数组转换为 List。 */
    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            values.add(array.get(i).asString());
        }
        return values;
    }
}
