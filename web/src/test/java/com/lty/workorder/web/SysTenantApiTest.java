package com.lty.workorder.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租户 CRUD 与参数校验接口测试。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SysTenantApiTest {

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
        MvcResult result = mockMvc.perform(post("/account/api/platform/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        platformToken = json.path("data").path("tokenValue").asString();
    }

    /** 健康检查应返回 UP 并携带 traceId。 */
    @Test
    void healthShouldReturnUpWithTraceId() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("UP"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    /** 应回显请求头中的 traceId。 */
    @Test
    void shouldEchoIncomingTraceId() throws Exception {
        String traceId = "trace-test-001";

        mockMvc.perform(get("/api/health").header("X-Trace-Id", traceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.traceId").value(traceId));
    }

    /** 种子租户的中文名称应正确返回。 */
    @Test
    void seedTenantShouldKeepChineseText() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/detail", "{\"uid\":\"TENANT0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tenantName").value("演示维保公司"));
    }

    /** 租户不存在应返回 404。 */
    @Test
    void missingTenantShouldReturn404() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/detail", "{\"uid\":\"not-exist\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /** uid 为空应返回 400。 */
    @Test
    void shouldReturn400WhenUidBlank() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/detail", "{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 新增租户名称为空应返回 400。 */
    @Test
    void shouldReturn400WhenCreateTenantNameBlank() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/create", """
                        {
                          "tenantCode": "T%s",
                          "tenantName": "   "
                        }
                        """.formatted(System.currentTimeMillis())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 租户名称超长应返回 400。 */
    @Test
    void shouldReturn400WhenTenantNameTooLong() throws Exception {
        String tenantName = "长".repeat(129);

        mockMvc.perform(platformPost("/platform/api/tenant/create", """
                        {
                          "tenantCode": "T%s",
                          "tenantName": "%s"
                        }
                        """.formatted(System.currentTimeMillis(), tenantName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 状态值非法应返回 400。 */
    @Test
    void shouldReturn400WhenStatusInvalid() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/status", "{\"uid\":\"TENANT0001\",\"status\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 编辑不存在的租户应返回 404。 */
    @Test
    void shouldReturn404WhenUpdateTenantNotExists() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/update",
                        "{\"uid\":\"not-exist\",\"tenantName\":\"不存在\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    /** 租户编码重复应返回 409。 */
    @Test
    void shouldReturn409WhenTenantCodeDuplicated() throws Exception {
        String body = """
                {
                  "tenantCode": "D%s",
                  "tenantName": "重复租户"
                }
                """.formatted(System.currentTimeMillis());

        mockMvc.perform(platformPost("/platform/api/tenant/create", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(platformPost("/platform/api/tenant/create", body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    /** 请求体 JSON 格式错误应返回 400。 */
    @Test
    void shouldReturn400WhenJsonMalformed() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/create", "{\"tenantCode\":"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 分页大小应被限制在最大 200。 */
    @Test
    void shouldClampPageSize() throws Exception {
        mockMvc.perform(platformPost("/platform/api/tenant/page", "{\"pageNum\":1,\"pageSize\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(200));
    }

    /** 新增、分页和详情流程应正常。 */
    @Test
    void createPageAndDetailTenant() throws Exception {
        String tenantCode = "T" + System.currentTimeMillis();

        MvcResult createResult = mockMvc.perform(platformPost("/platform/api/tenant/create", """
                        {
                          "tenantCode": "%s",
                          "tenantName": "测试租户",
                          "contactName": "李四",
                          "contactMobile": "13800000000",
                          "maxUserCount": 20
                        }
                        """.formatted(tenantCode)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isString())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String uid = createJson.get("data").asString();

        mockMvc.perform(platformPost("/platform/api/tenant/page", """
                        {
                          "pageNum": 1,
                          "pageSize": 10,
                          "tenantCode": "%s"
                        }
                        """.formatted(tenantCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].tenantCode").value(tenantCode))
                .andExpect(jsonPath("$.data.records[0].contactMobile").value("138****0000"));

        mockMvc.perform(platformPost("/platform/api/tenant/detail", "{\"uid\":\"" + uid + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.uid").value(uid))
                .andExpect(jsonPath("$.data.tenantCode").value(tenantCode))
                .andExpect(jsonPath("$.data.tenantName").value("测试租户"))
                .andExpect(jsonPath("$.data.contactMobile").value("138****0000"));

        mockMvc.perform(platformPost("/platform/api/tenant/update", """
                        {
                          "uid": "%s",
                          "tenantName": "测试租户-改名",
                          "contactName": "王五",
                          "maxUserCount": 30
                        }
                        """.formatted(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(platformPost("/platform/api/tenant/status",
                        "{\"uid\":\"" + uid + "\",\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(platformPost("/platform/api/tenant/detail", "{\"uid\":\"" + uid + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantName").value("测试租户-改名"))
                .andExpect(jsonPath("$.data.status").value(0));
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
