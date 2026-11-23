# lty-workorder 开发规范

| 项 | 内容 |
|---|---|
| 项目 | `lty-workorder`（多租户维保工单系统） |
| 版本 | v0.3 |
| 日期 | 2026-11-19 |
| 适用范围 | 所有模块（`common`、`account`、`core`、`web`）及后续新增模块 |
| 配套文档 | [总体设计](../design/00-总体设计.md) · [数据库 DDL](../sql/schema.sql) · [接口示例](../design/api-examples.http) · [评审清单](review-checklist.md) |

---

## 0. 阅读说明

### 0.1 约束级别

本文用三个级别描述规则：

| 级别 | 含义 | 违反后果 |
|---|---|---|
| **必须** | 强制要求，没有例外 | 评审不通过，不允许合并 |
| **建议** | 默认做法，偏离需要理由 | 在提交说明或评审中解释原因 |
| **禁止** | 明确不允许 | 评审不通过，必须修改 |

### 0.2 状态标记

| 标记 | 含义 |
|---|---|
| ✅ | 当前代码已经落地，本文是现状的固化 |
| 🟡 | M2–M6 规划中的能力，当前不作为验收项；实现时按本文执行 |

### 0.3 与其它文档的关系

- 业务范围、领域模型、表结构、接口清单、里程碑：以[总体设计](../design/00-总体设计.md)及对应分册为准。
- 工程结构、编码、分层、事务、测试、Git 等实现细节：以本文为准。
- 两者的冲突处理：**业务范围听设计文档，工程实现听本文**。如果设计文档的工程示例与本文不一致，以本文为准，并在后续维护设计文档时同步修正。

### 0.4 规范的目标

规范不是为了增加流程，而是为了减少三类问题：

1. 多租户串数据、越权、敏感信息泄漏；
2. 分层混乱、重复代码、改一处坏三处；
3. 并发、幂等、事务边界错误导致的重复派单、重复完工。

因此本文优先约束**会影响正确性和可维护性**的地方，不纠结个人风格。

---

## 1. 总则

### 1.1 基本原则

1. **正确优先于性能，性能优先于简洁。** 先保证租户隔离、事务、幂等正确，再谈优化。
2. **显式优于隐式。** 状态流转、租户来源、事务边界、失败处理都要在代码里看得见。
3. **约定优于配置。** 能用统一基类、统一响应、统一异常解决的，不在业务代码里重复造。
4. **不引入不必要的抽象。** 一期是模块化单体，不为了"以后可能拆微服务"提前上分布式组件。
5. **可观测。** 每个请求、每条 MQ 消息、每个定时任务都要能通过 `traceId` 串起来。
6. **文档与代码同步。** 接口、表结构、里程碑变化必须同步更新文档。

### 1.2 适用范围

- 新增功能必须遵守本文。
- 修改存量代码时，**至少保证改动部分符合本文**；不顺手的存量问题按附录 B 的时机处理，不做无关重构。
- 紧急修复可以先合入，但必须在提交说明中标注偏离项，并在下一个迭代补齐。

---

## 2. 技术栈与版本基线

### 2.1 版本清单

| 组件 | 版本 | 用途 | 状态 |
|---|---|---|---|
| Java | 21 | 语言基线 | ✅ |
| Spring Boot | 4.0.5 | 基础框架 | ✅ |
| Maven Wrapper | 3.9.9 | 构建 | ✅ |
| MyBatis-Flex | 1.11.8 | ORM、分页、租户、审计 | ✅ |
| springdoc-openapi | 3.0.3 | API 文档 | ✅ |
| Hutool | 5.8.32 | 工具库 | ✅ |
| Lombok | 1.18.44 | 简化代码 | ✅ |
| Jackson | Boot 4 默认（Jackson 3） | Web JSON | ✅ |
| MySQL | 8.4 | 业务库 | ✅ |
| H2 | Boot 管理版本 | 本地/测试库 | ✅ |
| Redis | 7 | 会话、缓存、锁、序列号 | 🟡 M2 |
| MinIO | 最新稳定版 | 对象存储 | 🟡 M5 |
| RocketMQ | 5.x 或 4.9.x | 异步消息 | 🟡 M4 |
| XXL-Job | 2.4+ | 定时任务 | 🟡 M4 |
| Redisson | 3.x | 分布式锁 | 🟡 M5 |

### 2.2 版本管理规则

- **必须**在根 `pom.xml` 的 `<properties>` 和 `<dependencyManagement>` 中统一管理版本。
- **禁止**在子模块 `pom.xml` 中为已管理的依赖写 `<version>`。
- **禁止**随意覆盖 Spring Boot 管理的依赖版本；确需覆盖时，必须在提交说明中写明原因和兼容性验证结果。
- 新增依赖时**必须**同步更新本文 2.1 节和总体设计的技术栈表。

### 2.3 Spring Boot 4 注意事项

1. Web 依赖使用 **`spring-boot-starter-webmvc`**，不要写旧版 `spring-boot-starter-web`。
2. Boot 4 默认使用 **Jackson 3**：
   - 数据绑定用 `tools.jackson.databind.ObjectMapper`；
   - 注解仍然是 `com.fasterxml.jackson.annotation.*`（如 `@JsonFormat`、`@JsonIgnore`）；
   - **禁止**混用 Jackson 2 的 `com.fasterxml.jackson.databind.ObjectMapper`。
3. 测试 Web 层使用 `spring-boot-starter-webmvc-test`，不要引旧版 `spring-boot-starter-test` 的 Web 片段。
4. 遇到第三方库与 Boot 4 不兼容时，先在设计文档"风险与待定项"登记，再决定降级或替换，禁止静默改版本。

---

## 3. 工程结构与模块边界

### 3.1 模块职责

| 模块 | 职责 | 允许依赖 | 禁止 |
|---|---|---|---|
| `common` | 响应、异常、租户上下文、TraceId、MyBatis-Flex 配置、通用工具 | 仅基础框架与通用库 | 依赖任何业务模块 |
| `account` | 租户、用户、角色、菜单、登录、权限 | `common` | 依赖 `core` / `web` |
| `core` | 工单、派工、客户、设备、技师、附件 | `common` | 依赖 `account` / `web` |
| `web` | 启动类、跨模块配置、公共接口 | `common` / `account` / `core` | 写核心业务逻辑 |

### 3.2 依赖方向

```text
web ──> account ──> common
 └───> core ──────> common
```

- **必须**保持单向依赖，禁止循环依赖。
- **禁止**`account` 与 `core` 互相直接依赖。
- 跨模块调用**必须**通过以下方式之一：
  1. `common` 中的 `I*ProviderService` 接口（由提供方实现）；
  2. Spring 事件（同步或异步）；
  3. 明确的领域事件 / MQ（🟡 M4）。
- **禁止**为了图方便在 `core` 中直接注入 `account` 的 Mapper 或 ServiceImpl。

### 3.3 启动类与扫描

`web` 模块的 `WebApplication` 是唯一启动入口：

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.lty.workorder")
@MapperScan(basePackages = {
        "com.lty.workorder.account.mapper",
        "com.lty.workorder.core.mapper"
})
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
```

新增模块时**必须**同步：

1. 根 `pom.xml` 的 `<modules>` 和 `<dependencyManagement>`；
2. `web/pom.xml` 的依赖；
3. `WebApplication` 的 `@MapperScan`；
4. `application.yml` 的 `springdoc.group-configs`（如果模块有 Controller）；
5. `mybatis-flex.tenant.ignore-tables`（如果模块有全局表）。

### 3.4 关于 `report` 模块

总体设计中的 `report` 模块**暂不创建**。等 `core` 稳定后，再按"报表/导出/消息"拆分，避免过早增加模块复杂度。

---

## 4. 包结构与命名

### 4.1 包结构

业务模块统一使用以下结构：

```text
com.lty.workorder.{module}
├── controller        # 接口层
├── service           # 业务接口
│   └── impl          # 业务实现
├── mapper            # MyBatis-Flex Mapper
├── entity            # 数据库实体
├── param             # 入参
├── vo                # 出参
├── dto               # 跨模块/内部传输对象（可选）
├── enums             # 模块内枚举（可选）
└── config            # 模块内配置（可选）
```

`common` 模块按能力划分：

```text
com.lty.workorder.common
├── config            # MyBatis-Flex、OpenAPI 等全局配置
├── domain            # BaseEntity、TenantEntity、CommonResponse、PageParam、PageResult
├── enums             # ErrorCode 等
├── exception         # BusinessException、GlobalExceptionHandler
├── filter            # TraceIdFilter
├── listener          # AuditFieldListener
├── tenant            # TenantContext、MyTenantFactory
├── annotation        # 🟡 @IgnoreTenant、@Log、@DistributedLock
├── aop               # 🟡 对应切面
├── mq                # 🟡 消息基类
└── utils             # 🟡 加密、脱敏、Excel
```

### 4.2 类命名

| 类型 | 规则 | 示例 |
|---|---|---|
| Controller | `XxxController` | `SysTenantController` |
| Service 接口 | `XxxService` | `SysTenantService` |
| Service 实现 | `XxxServiceImpl` | `SysTenantServiceImpl` |
| Mapper | `XxxMapper` | `SysTenantMapper` |
| 实体 | `XxxEntity` | `SysTenantEntity` |
| 分页入参 | `XxxPageParam` | `CustomerPageParam` |
| 新增入参 | `XxxCreateParam` | `CustomerCreateParam` |
| 修改入参 | `XxxUpdateParam` | `CustomerUpdateParam` |
| 通用入参 | `XxxParam` | 仅在多个操作字段完全一致时使用 |
| 出参 | `XxxVO` | `SysTenantVO` |
| 内部传输 | `XxxDTO` | `WorkOrderEventDTO` |
| 枚举 | `XxxStatus` / `XxxType` | `WorkOrderStatus` |
| 常量 | `XxxConstants` | `WorkOrderConstants` |
| 配置 | `XxxConfig` | `MyBatisFlexConfig` |
| 监听器 | `XxxListener` | `AuditFieldListener` |
| 过滤器 | `XxxFilter` | `TraceIdFilter` |

补充规则：

- **必须**使用名词 + 职责后缀，禁止 `Manager`、`Helper`、`Util` 作为业务类名。
- **禁止**使用 `Req` / `Resp` / `Query` / `BO` / `PO` 等与现有 `Param` / `VO` / `DTO` 冲突的命名。
- 枚举值**必须**全大写，多个单词用下划线：`PENDING`、`IN_PROGRESS`。

### 4.3 方法命名

| 场景 | 规则 | 示例 |
|---|---|---|
| 查询单个 | `getXxx` / `detail` | `getByUid`、`detail` |
| 查询列表 | `listXxx` / `page` | `listByOrderId`、`page` |
| 新增 | `create` / `add` | `create` |
| 修改 | `update` / `changeXxx` | `update`、`changeStatus` |
| 删除 | `delete` / `remove` | `delete` |
| 判断 | `isXxx` / `hasXxx` / `canXxx` | `canTransitionTo` |
| 转换 | `toXxx` | `toVO`、`toEntity` |
| 统计 | `countXxx` | `countByStatus` |

- 方法名**必须**表达业务含义，禁止 `doSomething`、`handle`、`process` 这类无信息量的名字。
- Controller、Service、Mapper 的同名操作**建议**保持一致，方便按链路排查。

### 4.4 数据库命名

| 对象 | 规则 | 示例 |
|---|---|---|
| 表 | 小写下划线、单数 | `sys_tenant`、`work_order`、`customer` |
| 平台/账号表 | `sys_` 前缀 | `sys_user`、`sys_role` |
| 业务表 | 不加前缀 | `work_order`、`technician` |
| 字段 | 小写下划线 | `tenant_id`、`create_time` |
| 主键 | `id` | `id` |
| 业务 ID | `uid` | `uid` |
| 租户 | `tenant_id` | `tenant_id` |
| 布尔语义 | `is_` 前缀 | `is_delete`、`is_system` |
| 时间 | `_time` 后缀 | `create_time`、`finish_time` |
| 日期 | `_date` 后缀 | `stat_date` |
| 金额 | `_amount` 后缀，`DECIMAL` | `service_amount` |
| 数量 | `_count` 后缀 | `max_user_count` |
| 唯一索引 | `uk_` 前缀 | `uk_tenant_order_no` |
| 普通索引 | `idx_` 前缀 | `idx_tenant_status` |

- **必须**使用 `utf8mb4` / `utf8mb4_0900_ai_ci`。
- 字段**必须**写 `COMMENT`，表和关键字段都要有中文说明。
- **禁止**使用 MySQL 关键字作为表名/字段名（如 `order`、`desc`、`status` 可用但需加反引号；优先换词）。

### 4.5 URL 命名

| 端 | 前缀 | 示例 |
|---|---|---|
| 平台端 | `/platform/api/{资源}/{动作}` | `/platform/api/tenant/page` |
| 租户端 | `/tenant/api/{资源}/{动作}` | `/tenant/api/work-order/create` |
| 技师端 | `/technician/api/{资源}/{动作}` | `/technician/api/work-order/accept` |
| 公共接口 | `/api/{资源}/{动作}` | `/api/health`、`/api/file/presign` |
| 登录/账号 | `/account/api/{client}/{动作}` | `/account/api/tenant/login` |

规则：

- 资源名**必须**用小写中划线：`work-order`、`export-task`。
- 动作**必须**用动词：`page`、`create`、`update`、`detail`、`status`、`assign`、`accept`、`reject`、`start`、`complete`、`confirm`、`cancel`、`reopen`。
- **禁止**在 URL 中使用自增 `id`；对外使用 `uid` / `orderNo`。
- 路径版本化暂不做；如需不兼容升级，在资源名中加 `v2`（如 `/tenant/api/work-order-v2/page`）。

### 4.6 权限标识

格式：`{client}:{domain}:{action}`

| 端 | client | 示例 |
|---|---|---|
| 平台端 | `platform` | `platform:tenant:page` |
| 租户端 | `tenant` | `tenant:work-order:assign` |
| 技师端 | `tech` | `tech:work-order:accept` |

- 权限串**必须**与 URL 的资源和动作对应。
- 权限串**必须**集中维护在 `sys_menu.perms`，禁止在代码里临时拼权限字符串。
- 新增接口时**必须**同时设计权限串，并在 `@SaCheckPermission` 中使用（🟡 M2）。

### 4.7 UID 与业务编号

| 类型 | 规则 | 示例 |
|---|---|---|
| `uid` | 32 位小写十六进制 UUID | `7f3c1a9b8d2e4f6a9c0b1d2e3f4a5b6c` |
| 工单号 | `WO` + `yyyyMMdd` + 6 位序号 | `WO20261119000001` |
| 客户编码 | 租户内业务编码 | `C0001` |
| 技师编码 | 租户内业务编码 | `T0001` |

- `uid` **必须**用 `IdUtil.fastSimpleUUID()` 生成，全局唯一。
- **禁止**把自增 `id` 暴露给前端或第三方。
- 业务编号（`order_no`、`customer_code`）与 `uid` 是两套东西：前者给人看、租户内唯一，后者给系统用、全局唯一。
- 工单号生成必须使用 Redis 序列号或数据库序列，**禁止**用 `count + 1`（并发会重复）。

---

## 5. 分层职责

### 5.1 Controller

**职责**：参数接收、校验、调用 Service、包装响应。

**允许**：

- `@RestController`、`@RequestMapping`、`@PostMapping`；
- `@Valid` 校验；
- `@Tag`、`@Operation` 文档注解；
- `CommonResponse` 包装；
- 简单的参数预处理（如取 Header 中的 `client-code`）。

**禁止**：

- 注入 Mapper；
- 写业务判断、事务、循环查询；
- 直接返回 Entity；
- 捕获异常后返回 `CommonResponse.ok()`；
- 在 Controller 里拼 SQL 或调用第三方。

标准写法：

```java
@RestController
@RequestMapping("/platform/api/tenant")
@RequiredArgsConstructor
@Tag(name = "平台端-租户管理")
public class SysTenantController {

    private final SysTenantService sysTenantService;

    @PostMapping("/page")
    @Operation(summary = "租户分页")
    public CommonResponse<PageResult<SysTenantVO>> page(@Valid @RequestBody SysTenantPageParam param) {
        return CommonResponse.ok(sysTenantService.page(param));
    }

    @PostMapping("/create")
    @Operation(summary = "新增租户")
    public CommonResponse<String> create(@Valid @RequestBody SysTenantCreateParam param) {
        return CommonResponse.ok(sysTenantService.create(param));
    }
}
```

> 当前 `SysTenantController` 已按本节拆分 Param 并使用 `@Valid`；后续新增接口沿用同样方式。

### 5.2 Service

**职责**：业务规则、事务边界、调用 Mapper、组装 VO。

**规则**：

- 业务 Service **必须**是"接口 + 实现"：接口放 `service`，实现放 `service.impl`。
- 实现类**必须**使用 `@Service`、`@RequiredArgsConstructor`、`@Slf4j`。
- 写操作方法**必须**标注 `@Transactional(rollbackFor = Exception.class)`。
- 读方法不需要事务；复杂只读查询可用 `@Transactional(readOnly = true)`（建议，不强制）。
- Service 对外返回 `VO`、`DTO`、基本类型或 `PageResult`，**禁止**把 Entity 作为 public 方法返回值。
- Service 内部可以使用 Entity 做查询和持久化。
- **禁止**在 Service 中直接依赖 `HttpServletRequest`、`HttpServletResponse`；需要请求信息时由 Controller 传参。
- **禁止**自调用事务方法（`this.xxx()` 不会走代理）；需要拆事务时用独立 Bean 或门面服务。

标准写法：

```java
public interface SysTenantService {

    PageResult<SysTenantVO> page(SysTenantPageParam param);

    SysTenantVO detail(String uid);

    String create(SysTenantCreateParam param);

    void update(SysTenantUpdateParam param);

    void changeStatus(String uid, Integer status);
}
```

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService {

    private final SysTenantMapper sysTenantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SysTenantCreateParam param) {
        // 1. 业务校验
        // 2. 唯一性校验
        // 3. 组装并插入
        // 4. 记录关键日志
        return uid;
    }
}
```

### 5.3 Mapper

**职责**：数据库访问。

**规则**：

- **必须**使用 `@Mapper` + `extends BaseMapper<XxxEntity>`。
- 优先使用 MyBatis-Flex 的 `QueryWrapper` 和 `BaseMapper` 内置方法。
- 复杂 SQL（多表、聚合、窗口函数）才写 XML；XML 放在 `src/main/resources/mapper/{module}/XxxMapper.xml`。
- XML 中**必须**使用 `#{}` 参数占位，**禁止** `${}` 拼接。
- **禁止**在 Mapper 接口中写 default 方法承载业务逻辑。
- 查询单条**必须**保证条件唯一（`uid`、`tenant_id + code` 等）；否则用 `selectListByQuery`。
- 分页**必须**用 `paginate(pageNum, pageSize, query)`，`pageSize` 上限 200。

```java
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenantEntity> {
}
```

### 5.4 Entity

**职责**：数据库表映射。

**规则**：

- 业务表**必须**继承 `TenantEntity`；平台/全局表继承 `BaseEntity`。
- 实体**必须**使用 `@Data`、`@EqualsAndHashCode(callSuper = true)`、`@Table`。
- 实体**禁止**出现业务方法、Swagger 注解、事务注解。
- 实体字段**必须**与表字段一一对应；不要放纯展示字段。
- 实体**禁止**直接作为接口返回值。
- 继承 `BaseEntity` 的表**必须**包含 `remark`、`create_by`、`update_by`、`create_time`、`update_time`、`is_delete`；如果某张表不需要这些字段，**不要继承** `BaseEntity`，改用普通实体 + `@Id`。
- 关联表、日志表可以只保留 `id`、`tenant_id`、`create_by`、`create_time`，不强制继承 `BaseEntity`。
- `@Table` 上**禁止**重复声明全局已注册的审计监听器。

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_tenant")
public class SysTenantEntity extends BaseEntity {

    private String uid;

    private String tenantCode;

    private String tenantName;
}
```

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "work_order")
public class WorkOrderEntity extends TenantEntity {

    private String uid;

    private String orderNo;

    private String status;

    private Integer version;
}
```

### 5.5 Param

**职责**：接收客户端入参。

**规则**：

- 分页查询**必须**继承 `PageParam`。
- 新增、修改**必须**使用独立 Param，**禁止**复用分页 Param 承载写操作字段。
- 每个字段**必须**加 `@Schema(description = "...")`。
- 校验注解**必须**加在 Param 上（`@NotBlank`、`@NotNull`、`@Size`、`@Pattern`、`@Min`、`@Max`）。
- 跨字段校验（如开始时间早于结束时间）在 Service 中手写。
- **禁止**在面向客户端的 Param 中包含 `id`、`tenantId`、`createBy`、`updateBy`、`isDelete`、`version`。
- **禁止**用 Entity 直接接收请求体。

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户分页参数")
public class SysTenantPageParam extends PageParam {

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
```

```java
@Data
@Schema(description = "新增租户参数")
public class SysTenantCreateParam {

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 32, message = "租户编码长度不能超过 32")
    @Schema(description = "租户编码", example = "T0001")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 128, message = "租户名称长度不能超过 128")
    @Schema(description = "租户名称", example = "演示维保公司")
    private String tenantName;
}
```

### 5.6 VO

**职责**：返回给客户端的数据。

**规则**：

- **必须**实现 `Serializable`，并声明 `serialVersionUID`。
- **必须**加 `@Schema`。
- 时间字段**必须**加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")`。
- **禁止**返回密码、密文、哈希、内部状态字段。
- 手机号等敏感字段**必须**脱敏后再放入 VO（🟡 M5 统一注解）。
- 一个 VO 可以同时服务列表和详情；当详情显著更重时，拆 `XxxDetailVO`。
- VO **禁止**直接引用 Entity 类型。

```java
@Data
@Schema(description = "租户信息")
public class SysTenantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "租户业务ID")
    private String uid;

    @Schema(description = "租户编码")
    private String tenantCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

### 5.7 DTO

**职责**：跨模块传输、MQ 消息体、内部聚合。

**规则**：

- 命名 `XxxDTO`，放在 `dto` 包。
- 不加 Swagger 注解。
- 简单只读 DTO 可以用 Java 21 `record`；需要框架反序列化的 DTO 用普通类 + 无参构造。
- **禁止**把 DTO 直接返回给客户端。

### 5.8 对象转换

- 新增：专用 `CreateParam` → Entity，可以用 `BeanUtil.copyProperties`，随后显式设置 `uid`、默认值。
- 修改：**必须**逐字段赋值，**禁止**整包 `copyProperties` 到 Entity（防止覆盖 `tenantId`、`status`、`version`）。
- 更新持久化**建议**使用 `mapper.update(entity, true)`（忽略 null），只更新实际传入的字段。
- Entity → VO：简单场景用 `BeanUtil.copyProperties`；涉及脱敏、计算、枚举翻译时手写 `toVO`。
- MapStruct 🟡：当 DTO/VO 转换超过 10 处或字段映射复杂时再引入，当前不强制。

```java
// 新增：允许
CustomerEntity entity = new CustomerEntity();
BeanUtil.copyProperties(param, entity);
entity.setUid(IdUtil.fastSimpleUUID());

// 修改：必须逐字段
SysTenantEntity update = new SysTenantEntity();
update.setId(existing.getId());
update.setTenantName(param.getTenantName());
update.setContactName(param.getContactName());
sysTenantMapper.update(update, true);
```

### 5.9 分层禁止清单

| 禁止项 | 原因 |
|---|---|
| Controller 注入 Mapper | 绕过业务规则和事务 |
| Controller 返回 Entity | 暴露内部字段、租户字段、审计字段 |
| Service public 方法返回 Entity | 同上，且难以演进 |
| Entity 写业务方法 | 领域逻辑分散，难测试 |
| Param 携带 `tenantId` | 客户端可伪造租户 |
| 修改时整包拷贝 Param 到 Entity | 批量赋值漏洞 |
| Mapper 中写业务分支 | SQL 难维护、难复用 |
| 在事务内调用短信/OSS/OCR | 长事务、外部失败导致回滚异常 |

---

## 6. 编码风格

### 6.1 格式

- 文件编码 **UTF-8**。
- 缩进 **4 个空格**，禁止 Tab。
- 单行建议不超过 **120** 字符。
- 左大括号不换行（K&R 风格）。
- 一个源文件只放一个 public 顶层类。
- import 明确到类，**禁止** `import xxx.*`（静态导入除外）。
- 同一文件不要混用 CRLF / LF；新增文件建议 LF。

### 6.2 Lombok

| 场景 | 用法 |
|---|---|
| Entity / Param / VO | `@Data` |
| 继承类 | `@Data` + `@EqualsAndHashCode(callSuper = true)` |
| Service 实现 / Controller | `@RequiredArgsConstructor` |
| 需要日志的类 | `@Slf4j` |
| 枚举 | `@Getter` + `@AllArgsConstructor` |
| 异常类 | `@Getter` |

**禁止**：

- `@SneakyThrows`（隐藏受检异常）；
- `@Builder` 用在需要 MyBatis-Flex 反射的 Entity 上（除非确认无兼容问题）；
- `@Data` 用在含敏感字段且需要打印日志的类上而不做脱敏；
- 用 Lombok 生成 `equals/hashCode` 后又在 `Set` 中修改字段。

### 6.3 依赖注入

**必须**使用构造器注入：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService {

    private final SysTenantMapper sysTenantMapper;
}
```

**禁止**：

- 字段注入 `@Autowired`；
- 注入 `ApplicationContext` 后用 `getBean` 取业务对象；
- 在 Controller 中注入多个 Service 只为了拼装一个响应（应下沉到 Service）。

### 6.4 空值与集合

- 判断字符串**必须**用 `StrUtil.isBlank/isNotBlank`，不要手写 `null == str || str.trim().isEmpty()`。
- 判断对象相等**必须**用 `Objects.equals` 或 `StrUtil.equals`。
- 返回集合时**必须**返回空集合而不是 `null`。
- **禁止**返回 `Optional` 作为 Controller 响应；内部可以使用。
- **禁止**用 `==` 比较包装类型（`Integer`、`Long`）。

### 6.5 时间

- 统一使用 `java.time.LocalDateTime` / `LocalDate`。
- **禁止**使用 `java.util.Date`、`Calendar`、`Timestamp`。
- JSON 时间格式统一 `yyyy-MM-dd HH:mm:ss`，时区 `GMT+8`。
- 数据库使用 `DATETIME`；不要用字符串存时间。
- 时间比较用 `isBefore` / `isAfter`，不要转成毫秒比较。

### 6.6 金额与数值

- 金额**必须**用 `BigDecimal`，数据库 `DECIMAL(12,2)`。
- **禁止**用 `double` / `float` 表示金额。
- `BigDecimal` 比较**必须**用 `compareTo`，不要用 `equals`。
- 除法**必须**指定精度和舍入模式。

### 6.7 注释

**语言与总则**

- 注释和 Javadoc **必须**使用中文；代码标识符保持英文。
- 注释**必须**解释 why、约束、边界和副作用，不要重复代码已经表达的 what。
- 注释**必须**与代码同步；修改行为时**必须**同步更新注释，过期注释视为缺陷。
- **禁止**在注释中写密码、token、真实客户信息、内部地址。

**类与方法**

- 每个 public 类**必须**有类级 Javadoc，至少说明职责、关键约束；涉及事务、线程安全、租户隔离时**必须**写明。
- 每个类**必须**加 `@author`（姓名，不写邮箱）和 `@since`（创建日期，格式 `yyyy-MM-dd`）；后续修改不覆盖原作者，重大重构可在方法级补充说明。
- public 接口、领域服务、公共工具方法**必须**有 Javadoc，按需使用 `@param`、`@return`、`@throws`；实现类的 `@Override` 方法可以省略或使用 `{@inheritDoc}`，避免复制。
- 简单 getter/setter、Lombok 生成方法、显而易见的一行方法可以省略方法 Javadoc。
- Controller 方法用 `@Operation` 描述接口，Param/VO 字段用 `@Schema` 描述字段；这些是 API 文档，不替代类级 Javadoc，也不需要重复写同样的方法 Javadoc。
- 复杂业务方法**建议**说明前置条件、状态变化、事务边界、幂等/并发要求。

**行内注释**

- 复杂 SQL、正则、状态流转、金额/权限计算、临时兼容逻辑**必须**有行内注释说明原因。
- 行内注释**必须**紧贴对应代码，不要写与代码无关的“日记式”注释。
- **禁止**保留注释掉的代码；需要时从 Git 历史找回。
- **禁止**用注释代替异常处理、参数校验或权限判断。

**TODO / FIXME / 废弃**

- TODO 格式：`// TODO(M2): 接入 Sa-Token 后从登录态取用户`，**必须**带里程碑或负责人。
- FIXME **必须**写明问题、影响和修复计划；**禁止**提交裸 `XXX`、`FIXME`。
- 废弃 API **必须**使用 `@Deprecated`，并在 Javadoc 中用 `@deprecated` 写明替代方案和移除计划。

**示例**

```java
/**
 * 租户管理服务。
 *
 * <p>租户表是平台级全局表；所有写操作在事务内完成。</p>
 */
public interface SysTenantService {

    /**
     * 新增租户。
     *
     * @param param 新增参数，编码和名称不能为空
     * @return 租户业务 ID
     * @throws BusinessException 租户编码重复时抛出 409
     */
    String create(SysTenantCreateParam param);
}
```

```java
// 正确：说明第二排序键的作用
.orderBy(SysTenantEntity::getCreateTime, false)
.orderBy(SysTenantEntity::getId, false) // 同一时间创建时保证分页稳定

// 错误：重复代码含义
// 设置租户名称
update.setTenantName(param.getTenantName());
```

### 6.8 其它

- **禁止** `System.out.println`、`printStackTrace`。
- **禁止**魔法值：状态、类型、错误码必须用枚举或常量。
- **禁止**在循环中逐条查库（N+1）；批量查询或分页。
- **建议**单个方法不超过 80 行；超过时拆分私有方法。
- **建议**单个类不超过 500 行；超过时按职责拆分。
- **建议**早返回（guard clause），减少嵌套。

---

## 7. API 规范

### 7.1 通用约定

- 除以下场景外，**必须**使用 `POST + application/json`：
  - 健康检查 `GET /api/health`；
  - Swagger / 文档跳转；
  - 文件下载、SSE；
  - 第三方回调（按对方要求）。
- 所有业务接口**必须**返回 `CommonResponse`。
- 查询接口**必须**幂等；核心工单写命令**必须**带幂等键；其它写操作**必须**说明幂等策略（唯一键、状态校验等）。
- 时间格式统一 `yyyy-MM-dd HH:mm:ss`。
- 对外 ID 统一 `uid` / `orderNo`，**禁止**暴露自增 `id`。

### 7.2 请求头

| Header | 必填 | 说明 |
|---|---|---|
| `satoken` | 登录后必填 | Sa-Token 令牌（🟡 M2） |
| `client-code` | 登录后必填 | `platform` / `tenant` / `technician` |
| `X-Request-Id` | 写命令必填 | 请求幂等 ID，前端生成并在重试时复用（🟡 M3） |
| `X-Trace-Id` | 可选 | 调用方传入的链路 ID；不传则服务端生成 |

### 7.3 分页

请求：

```json
{
  "pageNum": 1,
  "pageSize": 20
}
```

响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  },
  "traceId": "7f3c1a9b8d2e4f6a9c0b1d2e3f4a5b6c"
}
```

规则：

- `pageNum` 从 1 开始；`pageSize` 默认 20，最大 200。
- 列表默认按 `create_time DESC, id DESC` 排序。
- **禁止**在分页参数中传 `offset` / `limit` 绕过统一封装。
- 深分页（超过 1 万页）**建议**用游标方式，普通管理端用 `LIMIT offset`。

### 7.4 参数校验

- Controller 的 `@RequestBody` **必须**加 `@Valid`。
- 类路径参数 / `@RequestParam` **必须**在类上使用 `@Validated`，并在参数上写约束注解。
- 校验消息**必须**是中文、面向用户。
- 字段错误由全局异常处理汇总，格式：`字段: 错误信息; 字段: 错误信息`。
- **禁止**在 Controller 里手写 `if (param.getXxx() == null)` 做基础校验。

```java
@PostMapping("/create")
@Operation(summary = "新增租户")
public CommonResponse<String> create(@Valid @RequestBody SysTenantCreateParam param) {
    return CommonResponse.ok(sysTenantService.create(param));
}
```

### 7.5 Swagger

- 每个 Controller **必须**有 `@Tag`，命名格式 `{端}-{功能}`，如 `平台端-租户管理`。
- 每个接口**必须**有 `@Operation(summary = "...")`。
- Param / VO 字段**必须**有 `@Schema(description = "...")`；枚举字段写明取值。
- 新增 Controller 包时**必须**在 `application.yml` 的 `springdoc.group-configs` 中登记分组。
- 生产环境**必须**关闭 Swagger（`application-prod.yml` 已关闭）。
- 接口变更**必须**同步更新 `docs/design/api-examples.http`。

### 7.6 幂等

- 所有"创建、派单、接单、拒单、开始、完工、确认、取消、返工"写命令**必须**要求 `X-Request-Id`（🟡 M3）。
- 服务端以 `(tenant_id, request_id)` 唯一键落 `work_order_command`：
  - 首次请求：插入 `PROCESSING`，执行，成功后更新 `SUCCESS`；
  - 重复请求且已成功：直接返回首次结果；
  - 重复请求且处理中：返回 `409 请求处理中`。
- 前端重试**必须**复用同一个 `requestId`，不能每次重试重新生成。

### 7.7 兼容性

- 新增字段**必须**向后兼容；删除/改名字段属于不兼容变更，必须评估并通知调用方。
- 错误码含义**禁止**随意改变；新增错误码见 8.2。
- 接口废弃**必须**保留至少一个迭代，并在 Swagger 中标注 `deprecated`。

---

## 8. 响应、异常与错误码

### 8.1 统一响应

```json
{
  "code": 0,
  "msg": "success",
  "data": {},
  "traceId": "7f3c1a9b8d2e4f6a9c0b1d2e3f4a5b6c"
}
```

规则：

- **必须**通过 `CommonResponse.ok(...)` / `CommonResponse.fail(...)` 创建响应。
- **禁止**在业务代码中 `new CommonResponse<>(...)`。
- HTTP 状态码统一为 **200**；业务结果看 `code`。这样前端只需要处理一套逻辑。
- `traceId` 由 `TraceIdFilter` 写入 MDC，`CommonResponse` 自动读取；**禁止**手写 `traceId`。
- 无返回数据时使用 `CommonResponse<Void>` / `CommonResponse.ok()`。

### 8.2 错误码

当前错误码（`common/enums/ErrorCode.java`）：

| code | 枚举 | 含义 |
|---|---|---|
| 0 | `SUCCESS` | 成功 |
| 400 | `BAD_REQUEST` | 参数错误 |
| 401 | `UNAUTHORIZED` | 未登录 |
| 403 | `FORBIDDEN` | 无权限 |
| 404 | `NOT_FOUND` | 资源不存在 |
| 409 | `CONFLICT` | 数据冲突 / 并发修改 / 重复请求 |
| 429 | `TOO_MANY_REQUESTS` | 请求过于频繁 |
| 500 | `INTERNAL_ERROR` | 系统异常 |
| 501 | `LOGIN_EXPIRED` | 登录过期（沿用原项目约定） |

规则：

- 优先复用以上错误码，不要为每个业务场景新增一个码。
- 需要新增时**必须**：
  1. 在 `ErrorCode` 中增加枚举；
  2. 错误码段与现有语义不冲突；
  3. 同步更新本文和前端约定；
  4. 在 Swagger 中说明。
- **禁止**直接把数据库异常、堆栈、SQL 信息返回给客户端。

### 8.3 业务异常

- 预期内的业务失败**必须**抛 `BusinessException`。
- **禁止**用 `return CommonResponse.fail(...)` 代替异常（Controller 层除外）。
- **禁止**捕获 `BusinessException` 后吞掉。
- 消息必须面向用户、可理解、中文；不要包含内部类名、SQL、文件路径。

```java
if (StrUtil.isBlank(param.getTenantCode())) {
    throw new BusinessException(ErrorCode.BAD_REQUEST, "租户编码不能为空");
}

if (exists > 0) {
    throw new BusinessException(ErrorCode.CONFLICT, "租户编码已存在");
}
```

### 8.4 全局异常处理

`GlobalExceptionHandler` 负责：

| 异常 | 处理 |
|---|---|
| `BusinessException` | `WARN` 日志，返回对应 code/msg |
| `MethodArgumentNotValidException` | 汇总字段错误，返回 400 |
| `BindException` | 同上 |
| `ConstraintViolationException` | 返回 400 |
| `HttpMessageNotReadableException` | 返回 400，提示请求体格式错误 |
| `MethodArgumentTypeMismatchException` | 返回 400，提示参数类型错误 |
| `MissingServletRequestParameterException` | 返回 400，提示缺少参数 |
| `DuplicateKeyException` | 返回 409，提示数据已存在 |
| Sa-Token 异常（`NotLoginException` / `NotPermissionException` 等，🟡 M2） | 返回 401 / 403 / 501 |
| `NoResourceFoundException` | 返回 404 |
| 其它 `Exception` | `ERROR` 日志 + 堆栈，返回 500，不暴露细节 |

规则：

- **禁止**在业务代码中写 `try/catch(Exception)` 后返回成功。
- 需要转换异常时，**必须**保留原始异常作为 cause。
- 新增全局异常类型时，**必须**在 `GlobalExceptionHandler` 中显式处理，不要依赖兜底。

### 8.5 日志与异常的分工

- 业务异常：`log.warn`，不需要堆栈。
- 系统异常：`log.error("...", e)`，必须带堆栈。
- **禁止**对同一个异常既在 Service 里 `log.error` 又在全局处理器里 `log.error`（重复日志）。
- 敏感信息（密码、token、手机号明文）**禁止**写日志。

---

## 9. 多租户规范

这是本项目最重要的正确性约束，**必须**严格遵守。

### 9.1 数据模型

- 所有业务表**必须**包含 `tenant_id VARCHAR(32) NOT NULL`。
- `tenant_id` 存的是 `sys_tenant.tenant_code`（如 `T0001`），不是自增 `id`。
- 平台数据统一 `tenant_id = '0'`。
- 业务实体**必须**继承 `TenantEntity`：

```java
public class TenantEntity extends BaseEntity {

    @Column(tenantId = true)
    private String tenantId;
}
```

- 平台/全局表（`sys_tenant`、`sys_menu`、`sys_dict_type`、`sys_dict_data`、`sys_login_log`、`sys_operate_log`、`mq_consume_log`、`msg_fail`）**必须**继承 `BaseEntity`，并登记到 `application.yml`：

```yaml
mybatis-flex:
  tenant:
    enabled: true
    column: tenant_id
    ignore-tables:
      - sys_tenant
      - sys_menu
      - sys_dict_type
      - sys_dict_data
      - sys_login_log
      - sys_operate_log
      - mq_consume_log
      - msg_fail
```

- 新增全局表时**必须**同时更新 `ignore-tables`，否则租户插件会错误拼条件。

### 9.2 租户上下文

- HTTP 请求：由登录拦截器从会话读取 `tenantId`，写入 `TenantContext`，请求结束清理（🟡 M2）。
- 平台端：`TenantContext.set("0")`，跨租户查询必须显式 `@IgnoreTenant`。
- MQ 消费：从消息体 `tenantId` 写入，消费结束清理。
- 定时任务：任务参数或遍历租户时显式 `set`，结束清理。
- 异步线程：通过 `TaskDecorator` 从父线程复制 `TenantContext` 和 MDC。

```java
public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    public static void set(String tenantId) { TENANT.set(tenantId); }

    public static String get() { return TENANT.get(); }

    public static boolean isIgnore() { return Boolean.TRUE.equals(IGNORE.get()); }

    public static void ignore() { IGNORE.set(true); }

    public static void clear() {
        TENANT.remove();
        IGNORE.remove();
    }
}
```

**禁止**：

- 在 `MyTenantFactory` 中硬编码租户 ID；
- 从请求体、Query 参数、Header 中直接读取 `tenantId` 作为数据隔离依据；
- 在业务代码中直接调用 `TenantContext.ignore()` 绕过隔离（必须用 `@IgnoreTenant` 注解，且仅限平台端）。

### 9.3 查询规则

- 租户端、技师端的所有查询**必须**依赖租户插件自动拼接 `tenant_id`。
- 按 `uid` 查询也**必须**带租户条件；即使有唯一索引，也不能跨租户查。
- `selectById`、`selectOneById` 等按主键查询同样受租户插件保护；**禁止**用 `@IgnoreTenant` 后不补租户条件。
- 跨租户资源访问**必须**表现为"不存在"（404），不要返回"无权限"暴露资源存在性。
- 列表查询**禁止**查全表后再在内存过滤租户。

正确：

```java
CustomerEntity entity = customerMapper.selectOneByQuery(
        QueryWrapper.create().eq(CustomerEntity::getUid, uid));
if (entity == null) {
    throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在");
}
```

错误（绕过插件）：

```java
// 禁止：手动拼 tenant_id 字符串且没有平台权限校验
QueryWrapper.create().where("tenant_id = '" + tenantId + "'");
```

### 9.4 平台跨租户查询

🟡 M2 实现 `@IgnoreTenant` 后：

- 只有平台端接口可以使用 `@IgnoreTenant`。
- 使用前**必须**校验平台权限（`@SaCheckPermission`）。
- 如果按指定租户查看，**必须**在查询中手动追加 `tenant_id = 请求参数`，不能查全表。
- 跨租户查询**必须**记录操作日志（谁、何时、查了哪个租户）。
- **禁止**在租户端、技师端使用 `@IgnoreTenant`。

```java
@IgnoreTenant
@SaCheckPermission("platform:work-order:page")
@PostMapping("/page")
public CommonResponse<PageResult<WorkOrderVO>> page(@Valid @RequestBody PlatformWorkOrderPageParam param) {
    return CommonResponse.ok(workOrderService.platformPage(param));
}
```

### 9.5 异步、MQ、Job

- 线程池**必须**配置 `TaskDecorator`，复制 `TenantContext` 和 MDC。
- MQ 消费者**必须**在 `handleMessage` 开始时 `TenantContext.set(message.getTenantId())`，`finally` 中 `TenantContext.clear()`。
- 定时任务**必须**显式设置租户上下文，处理完清理。
- **禁止**依赖 ThreadLocal 自动传递到新线程。

### 9.6 租户隔离测试（必须）

每个涉及租户数据的接口**必须**至少覆盖：

1. 租户 A 创建数据；
2. 租户 B 用同一个 `uid` 查询 → 404；
3. 租户 B 修改/删除 → 404；
4. 列表查询只返回本租户数据；
5. 平台端 `@IgnoreTenant` 查询在权限校验通过时能看到多租户数据。

测试失败**禁止**通过放宽查询条件"修复"。

### 9.7 多租户禁止清单

| 禁止项 | 后果 |
|---|---|
| 业务表没有 `tenant_id` | 数据串租户 |
| 实体继承 `BaseEntity` 但表有 `tenant_id` | 插件不生效 |
| 全局表未加入 `ignore-tables` | 平台查询被错误过滤 |
| 请求体传 `tenantId` 并用于查询 | 可伪造租户 |
| 业务代码直接 `TenantContext.ignore()` | 绕过隔离且无审计 |
| 异步线程不传递/不清理上下文 | 串数据、内存泄漏 |
| 按 `uid` 查询不带租户条件 | 越权 |

---

## 10. 数据库规范

### 10.1 基础约定

| 约定 | 说明 |
|---|---|
| 引擎 | `InnoDB` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_0900_ai_ci` |
| 主键 | `id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT` |
| Java 主键类型 | `Long` |
| 业务 ID | `uid VARCHAR(32) NOT NULL`，全局唯一 |
| 租户 | `tenant_id VARCHAR(32) NOT NULL DEFAULT '0'` |
| 时间 | `DATETIME`，Java `LocalDateTime` |
| 金额 | `DECIMAL(12,2)`（二期） |
| 外键 | **禁止**数据库外键，应用层保证 |
| 枚举 | 简单状态用 `TINYINT`，状态机用 `VARCHAR(32)` 存枚举名 |

### 10.2 必备字段

业务主表**必须**包含：

```sql
`id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
`uid`         VARCHAR(32)     NOT NULL COMMENT '业务ID',
`tenant_id`   VARCHAR(32)     NOT NULL COMMENT '租户编码',
`remark`      VARCHAR(500)    DEFAULT NULL COMMENT '备注',
`create_by`   VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
`update_by`   VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
`create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`is_delete`   TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是'
```

核心表（工单）额外**必须**有：

```sql
`version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本'
```

关联表/日志表可以精简：

- 关联表（`sys_user_role`、`sys_role_menu`）：`id`、`tenant_id`、关联字段、`create_by`、`create_time`；
- 日志表（`sys_login_log`、`sys_operate_log`）：`id`、`tenant_id`、业务字段、`create_time`；
- 不强制 `is_delete`、`update_time`。

### 10.3 索引

- 唯一索引命名 `uk_*`，普通索引 `idx_*`。
- 租户表的联合索引**必须**把 `tenant_id` 放在第一列。
- 唯一约束**必须**包含 `tenant_id`（除非是全局唯一，如 `uid`）。
- 列表页主查询、状态筛选、时间范围、外键关联字段**必须**有索引。
- **禁止**给低选择性字段单独建索引（如 `status`、`is_delete`）；应作为联合索引的后续列。
- 新增索引前**必须**用 `EXPLAIN` 验证；删除索引前**必须**确认无查询依赖。

示例：

```sql
UNIQUE KEY `uk_tenant_order_no` (`tenant_id`, `order_no`),
KEY `idx_tenant_status` (`tenant_id`, `status`),
KEY `idx_tenant_technician_status` (`tenant_id`, `technician_id`, `status`),
KEY `idx_tenant_expect` (`tenant_id`, `expect_time`, `status`)
```

### 10.4 逻辑删除

- 业务主表**必须**使用逻辑删除 `is_delete = 0/1`。
- 日志、关联、幂等表可以物理删除或按时间归档。
- 查询默认由 MyBatis-Flex 自动追加 `is_delete = 0`；**禁止**手写 `is_delete = 0` 条件（除非使用原生 SQL）。
- 删除**必须**用 `mapper.deleteById(...)` 触发逻辑删除，**禁止**手写 `UPDATE ... SET is_delete = 1`。
- 唯一索引与逻辑删除的冲突（删除后无法复用编码）**必须**在建表时明确：能复用就调整唯一键策略，不能复用就在注释中说明。

### 10.5 关联与冗余

- **禁止**数据库外键；关联完整性由应用保证。
- 列表页**建议**冗余展示字段，减少 join：
  - `work_order` 冗余 `customer_name`、`equipment_name`、`technician_name`；
  - 冗余字段在源数据变更时同步更新（🟡 事件或事务内更新）。
- 详情页可以按 `id` 查关联表，但**禁止**在循环中逐条查（N+1）。
- 多表 join 只用于报表和详情；列表页 join **建议**不超过 1 张表。

### 10.6 SQL 规范

- **禁止** `SELECT *`；必须显式列或使用 MyBatis-Flex 的实体映射。
- **禁止**字符串拼接 SQL；必须参数化。
- **禁止** `${}`；只允许 `#{}`。
- 批量写入**必须**分批，每批 500 条以内。
- 批量更新/删除**必须**带 `tenant_id` 条件。
- 慢 SQL 阈值 **500ms**；超过时记录 WARN 日志并优化；普通 SQL 只在 DEBUG 输出，且不打印参数值。
- **建议**避免 `%keyword%` 前置模糊查询；大数据量用全文索引或搜索引擎（二期）。

### 10.7 Schema 同步（必须）

本项目有两套 schema：

| 文件 | 用途 | 维护方 |
|---|---|---|
| `docs/sql/schema.sql` | MySQL 8 正式建表，Docker 初始化 | 唯一事实来源 |
| `web/src/main/resources/db/h2-schema.sql` | 本地/测试 H2 | 按已启用功能与 MySQL 同步 |
| `web/src/main/resources/db/h2-data.sql` | 本地/测试种子数据 | 按需同步 |
| `docker/mysql/init/02-seed.sql` | Docker MySQL 种子数据 | 按需同步 |

**任何当前已启用功能涉及的表结构变更必须同时修改**：

1. `docs/sql/schema.sql`；
2. `web/src/main/resources/db/h2-schema.sql`；
3. 如果涉及种子数据，更新 `h2-data.sql` 和 `02-seed.sql`；
4. 如果字段影响实体，更新 Entity / Param / VO / Swagger 示例。

> H2 只维护当前已启用功能需要的表；尚未启用的 MySQL 表不要求立即镜像到 H2，但对应功能开始使用时必须补齐。

**禁止**只改 MySQL 或只改 H2。评审时**必须**检查两套 schema 的字段、索引、默认值一致。

### 10.8 数据库变更流程

1. 在 `docs/sql/schema.sql` 写 DDL，带中文 `COMMENT`；
2. 同步 `h2-schema.sql`（H2 不支持 `UNSIGNED`、`COMMENT` 等语法时做等价简化）；
3. 更新实体和映射；
4. 补测试（含租户隔离）；
5. 更新 `docs/design/05-数据库设计.md` 的表清单/字段说明；
6. 本地 `docker compose down -v && docker compose up -d` 验证初始化脚本；
7. 提交说明写明"数据库变更 + 影响范围 + 回滚方式"。

---

## 11. 事务、并发与幂等

### 11.1 事务

- 事务**必须**加在 Service 实现的 public 方法上，使用 `@Transactional(rollbackFor = Exception.class)`。
- **禁止**加在 Controller、Mapper、private 方法上。
- **禁止**自调用事务方法（`this.method()` 不经过代理）。
- 事务**必须**短：只包数据库写操作；**禁止**在事务内调用短信、OSS、OCR、HTTP 第三方。
- 多个写操作（工单 + 日志 + 派工记录 + 幂等记录）**必须**在同一事务内。
- 只读查询不需要事务；复杂报表可以用 `readOnly = true`。
- **禁止**用 `REQUIRES_NEW` 除非有明确理由（如独立失败日志）。

标准模式：

```java
@Transactional(rollbackFor = Exception.class)
public void assign(String orderUid, String technicianUid, String requestId) {
    // 1. 幂等记录
    // 2. 状态校验
    // 3. 乐观锁更新
    // 4. 写日志 / 派工记录
    // 5. 事务提交后发事件（不在这里发）
}
```

### 11.2 乐观锁

核心表（`work_order`）**必须**使用 `version` 乐观锁：

```sql
UPDATE work_order
SET status = #{target}, version = version + 1, update_time = NOW()
WHERE id = #{id}
  AND tenant_id = #{tenantId}
  AND version = #{version}
  AND status = #{fromStatus}
  AND is_delete = 0;
```

规则：

- 影响行数为 0 → 抛 `BusinessException(ErrorCode.CONFLICT, "数据已被修改，请刷新后重试")`。
- **禁止**先查后改而不带版本条件（并发会丢更新）。
- 状态流转**必须**同时校验 `fromStatus`，不能只校验 `version`。
- 更新成功后**必须**重新查询或使用数据库返回值，不要假设内存中的状态已同步。

### 11.3 分布式锁（🟡 M5）

- 使用 Redisson，注解 `@DistributedLock`。
- Key 格式：`lock:{biz}:{action}:{tenantId}:{businessId}`。
- 锁**必须**包住事务，释放锁在事务提交之后；推荐"门面服务加锁 + 事务服务执行"两层结构。
- 租约时间**必须**大于事务耗时；长事务使用看门狗（`leaseTime = -1`）。
- **禁止**在锁内调用第三方或长时间等待。
- 获取锁失败**必须**返回明确业务提示，不能静默重试无限循环。

### 11.4 请求幂等（🟡 M3）

- 写命令**必须**要求 `X-Request-Id`。
- `work_order_command` 唯一键 `(tenant_id, request_id)`。
- 流程：插入 `PROCESSING` → 执行业务 → 更新 `SUCCESS` / `FAILED`。
- 重复请求：
  - `SUCCESS`：返回首次结果；
  - `PROCESSING`：返回 `409 请求处理中`；
  - `FAILED`：按业务决定是否允许重试。
- **禁止**只靠前端防重复点击。

### 11.5 事务与消息

- MQ 事件**必须**在事务提交后发送；事务内发送会先于数据库提交，消费方可能查不到数据。
- 推荐使用 `TransactionSynchronizationManager` 的 `afterCommit`（🟡 M4）。
- 发送失败**必须**落 `msg_fail`，由定时任务补偿。
- **禁止**在事务内同步调用消费者逻辑。

---

## 12. 规划中技术约定（M4–M6）

以下内容当前未实现，标记为 🟡；实现时**必须**按本节执行，不另行发明约定。

### 12.1 消息（RocketMQ，🟡 M4）

- Topic：`lty_workorder_topic${mq.env-suffix}`。
- Tag：`CREATED`、`ASSIGNED`、`ACCEPTED`、`REJECTED`、`STARTED`、`COMPLETED`、`CONFIRMED`、`CANCELED`、`TIMEOUT`。
- 消费组：`lty-workorder-notify-consumer`、`lty-workorder-stat-consumer`、`lty-workorder-audit-consumer`。
- 消息信封**必须**包含：

```json
{
  "eventId": "01J...",
  "eventType": "WORK_ORDER_ASSIGNED",
  "tenantId": "T0001",
  "traceId": "01J...",
  "timestamp": 1760000000000,
  "payload": {}
}
```

- 生产**必须**在事务提交后发送。
- 消费**必须**幂等：`mq_consume_log` 唯一键 `(topic, consumer_group, msg_id)`。
- 消费**必须**设置/清理 `TenantContext` 和 MDC。
- 重试 3 次后落 `msg_fail`，状态 `DEAD`，提供平台端重试入口。
- **禁止**在消息体里放密码、token、完整手机号。

### 12.2 定时任务（XXL-Job，🟡 M4）

- 每个任务**必须**加分布式锁，避免多实例重复执行。
- 任务**必须**幂等：重复执行结果一致。
- 按 `id` 分批，每批 200 条。
- 日志**必须**带 `jobId`、`traceId`、处理数量、失败数量。
- **禁止**在任务中全表扫描；必须走索引。
- 典型任务：`workOrderSlaJob`、`workOrderAutoConfirmJob`、`workOrderDailyStatJob`、`exportTaskRecoveryJob`、`mqFailRetryJob`。

### 12.3 缓存（Redis，🟡 M5）

Key 格式：`{biz}:{type}:{tenantId}:{id}`

| Key | TTL | 失效方式 |
|---|---|---|
| `auth:version:{client}:{tenant}` | 永久 | 角色/菜单变更 `INCR` |
| `auth:info:{client}:{tenant}:{user}:{version}` | 30 分钟 | 版本号变更 |
| `dict:{dictType}` | 1 小时 | 字典变更删除 |
| `tenant:info:{tenantId}` | 10 分钟 | 租户变更删除 |
| `seq:workorder:{tenant}:{date}` | 2 天 | 自然过期 |
| `login:fail:{client}:{username}` | 15 分钟 | 登录成功删除 |

规则：

- **必须**设置 TTL（除版本号等特殊 key）。
- 所有业务缓存 key **必须**带租户，避免串租户。
- 更新顺序：先更新 DB，再删除缓存。
- **禁止**使用 `KEYS` / `SCAN` 清理业务缓存；用版本号或精确 key。
- 本地 Caffeine 只放极热数据（字典、租户），TTL 短。
- 工单列表**禁止**缓存，靠索引。

### 12.4 文件（MinIO，🟡 M5）

- 使用预签名直传，文件不经过应用服务器。
- 单文件最大 20MB；只允许 `jpg/jpeg/png/webp/pdf`。
- 文件 key 随机生成，**禁止**暴露原始路径。
- 私有桶；下载使用 15 分钟预签名 URL。
- 上传前**必须**校验 `contentType` 和扩展名；不能只信客户端。
- 文件元数据落库，包含 `fileKey`、`fileName`、`fileType`、`bizType`、`tenantId`。

### 12.5 异步导出（🟡 M5）

- 创建 `export_task`，状态 `PENDING`，异步线程执行。
- 分页查询，每页 2000 条；使用流式写 Excel（SXSSF / EasyExcel）。
- 导出上限 10 万条，超过提示缩小范围。
- 成功后上传 MinIO，更新 `SUCCESS` + `fileKey`；失败更新 `FAILED` + `error_msg`。
- 前端轮询状态；下载用预签名 URL。
- **禁止**在请求线程内同步导出大文件。

### 12.6 敏感数据（🟡 M5）

- 手机号、身份证号**必须**加密存储（SM4）。
- 精确查询用哈希索引：`mobile_hash = SHA-256(明文 + 盐)`。
- 输出**必须**脱敏：`@SensitiveMask(MaskType.MOBILE)`。
- 查看明文**必须**记录审计日志（谁、何时、看了哪条数据）。
- **禁止**明文进日志、进 MQ、进导出文件。

---

## 13. 安全与审计

### 13.1 认证与密码

- 密码**必须**使用 BCrypt（strength 10）存储，**禁止**明文、MD5、SHA-1。
- 登录失败 5 次锁定 15 分钟（🟡 M2）。
- 短信验证码 5 分钟有效，最多校验 5 次；发送限流 1 分钟 1 次、1 天 10 次（🟡 M2）。
- Token 30 天有效，5 天无活动失效，剩余 < 30 分钟自动续期（🟡 M2）。
- 同一账号多端登录按 `client-code` 隔离（🟡 M2）。

### 13.2 权限

- 除白名单外，所有接口**必须**加 `@SaCheckPermission`（🟡 M2）。
- 白名单**必须**集中配置，至少包括：登录、短信、健康检查、Swagger（仅本地/开发）。
- **禁止**在 Controller 方法内手写"是否是管理员"的判断绕过权限框架。
- 数据权限（本人/本租户/全部）**必须**在 Service 查询条件中体现，不能只靠前端隐藏按钮。

### 13.3 输入与 SQL

- 所有入参**必须**校验：长度、格式、范围、枚举值。
- SQL **必须**参数化，**禁止** `${}` 和字符串拼接。
- 排序字段、动态列名**必须**白名单校验。
- 文件上传**必须**校验类型、大小、内容（M5）。

### 13.4 敏感数据

**禁止**出现在日志、响应、MQ、导出文件中的内容：

- 密码、token、密钥；
- 完整手机号、身份证号、银行卡号；
- 客户详细地址（按需脱敏）；
- 数据库连接串、内部 IP。

需要展示时**必须**脱敏，例如 `138****0000`。

### 13.5 文件

- 上传大小限制在 `application.yml` 中统一配置（当前 20MB/50MB）。
- 只允许白名单类型。
- 文件名**禁止**直接用于存储路径；使用随机 key。
- 下载**必须**校验权限和租户。

### 13.6 配置与密钥

- **禁止**把真实密码、密钥、token 提交到仓库。
- 本地密钥放 `.env`，`.env` 已在 `.gitignore`。
- 生产配置**必须**通过环境变量注入，`application-prod.yml` 只写 `${DB_URL}` 这类占位。
- 示例配置放 `.env.example`，值必须是假数据。
- 生产环境**必须**关闭 Swagger（已配置）。
- Actuator 只暴露 `health,info`（已配置）。

### 13.7 审计

- 所有写操作**必须**记录操作日志（🟡 M5：`@Log` + AOP + `sys_operate_log`）。
- 记录内容：租户、用户、端、URI、方法、参数（脱敏）、结果、耗时、IP、traceId。
- 查询默认不记录；敏感查询（查看明文、跨租户查询）**必须**记录。
- 日志表按时间归档或分表（二期）。

---

## 14. 日志与追踪

### 14.1 日志规则

- **必须**使用 SLF4J：`private static final Logger log = LoggerFactory.getLogger(...)` 或 Lombok `@Slf4j`。
- **必须**使用占位符：`log.info("创建租户成功: tenantCode={}, uid={}", code, uid)`。
- **禁止**字符串拼接日志：`log.info("... " + code)`。
- **禁止** `System.out`、`printStackTrace`。
- 级别使用：

| 级别 | 场景 |
|---|---|
| `ERROR` | 系统异常、需要人工介入 |
| `WARN` | 业务异常、可恢复失败 |
| `INFO` | 关键业务事件（创建、派单、完工、启动完成） |
| `DEBUG` | 开发诊断，生产默认关闭 |
| `TRACE` | 不用于生产 |

- **禁止**在循环中打印大量 INFO 日志。
- **禁止**打印完整请求体（可能含敏感信息）。

### 14.2 traceId

- `TraceIdFilter` 为每个请求生成/接收 `X-Trace-Id`，写入 MDC 和响应头。
- 日志 pattern 已包含 `[%X{traceId}]`，**禁止**在日志消息里重复拼 traceId。
- MQ 消息体**必须**带 `traceId`，消费者写入 MDC。
- 定时任务每次执行**必须**生成 `traceId`。
- 线程池**必须**通过 `TaskDecorator` 传递 MDC。
- 异步线程结束**必须**清理 MDC，避免线程复用串日志。

### 14.3 慢 SQL

- 慢 SQL 阈值 **500ms**。
- 当前 `MyBatisFlexConfig` 已按阈值输出：普通 SQL 为 `DEBUG`，耗时 ≥ 500ms 为 `WARN`；只输出 `AuditMessage.getQuery()`，不打印参数值。
- 慢 SQL 日志**必须**包含：SQL、耗时、traceId、租户、用户。
- **禁止**在生产打印完整参数中的敏感字段。

---

## 15. 配置与环境

### 15.1 Profile

| Profile | 用途 | 数据库 | Swagger | H2 控制台 |
|---|---|---|---|---|
| `local` | 本地开发（默认） | H2 内存 | 开 | 开 |
| `dev` | 本地/联调 MySQL | MySQL | 开 | 关 |
| `test` | CI / 集成测试 | H2 内存 | 开 | 关 |
| `prod` | 生产 | MySQL（环境变量） | 关 | 关 |

- `application.yml` 默认激活 `local`。
- 使用 MySQL 时显式指定：`--spring.profiles.active=dev`。
- **禁止**在 `prod` 中启用 H2 控制台或 Swagger。
- 新增 Profile **必须**同步本文和 README。

### 15.2 配置分层

- `application.yml`：所有环境共享的配置（应用名、端口、线程、上传限制、MyBatis-Flex 租户、springdoc、Actuator）。
- `application-{profile}.yml`：只放该环境差异（数据源、开关）。
- **禁止**在共享配置中写环境相关的密码/地址。
- 配置项**必须**有注释或文档说明，新增配置**必须**在本文或 README 登记。

### 15.3 环境变量

生产/联调配置**必须**使用环境变量：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
ROCKETMQ_NAME_SERVER
MINIO_ENDPOINT
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
XXL_JOB_ADMIN_ADDRESSES
```

规则：

- 环境变量名**必须**大写 + 下划线。
- 默认值只允许出现在 `local` / `dev`，`prod` **禁止**给密码默认值。
- `.env` **禁止**提交；`.env.example` 只放假数据。
- **禁止**在代码中硬编码密钥。

### 15.4 新增配置的检查项

1. 是否所有 profile 都能启动？
2. 是否有安全的默认值？
3. 是否包含敏感信息？
4. 是否在 `.env.example` / README / 本文登记？
5. 是否影响 Docker Compose？

---

## 16. 测试规范

### 16.1 测试分层

| 类型 | 位置 | 内容 |
|---|---|---|
| 单元测试 | 各模块 `src/test/java` | 状态机、工具类、加密、权限计算、金额 |
| 集成测试 | `web/src/test/java` | API、事务、租户隔离、幂等 |
| 并发测试 | `web/src/test/java` 或独立脚本 | 并发接单、重复请求、工单号 |
| 压测 | 独立工具（JMeter/Gatling） | 分页、创建、接单 p95 |

- 当前只有 `web` 模块有集成测试（`SysTenantApiTest`），新增业务模块**必须**补对应测试。
- 集成测试**必须**使用 `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("local")` 或 `test`。
- 单元测试**禁止**连接真实 MySQL / Redis。

### 16.2 命名

- 测试类：`XxxApiTest`、`XxxServiceTest`、`XxxStateMachineTest`。
- 测试方法：`shouldXxxWhenYyy` 或清晰的英文描述。
- 可选 `@DisplayName("中文场景")`，但**禁止**只有中文方法名。
- 结构：given / when / then，一个测试只验证一个场景。

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SysTenantApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn404WhenTenantNotExists() throws Exception {
        mockMvc.perform(post("/platform/api/tenant/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"uid\":\"not-exist\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
```

### 16.3 每个接口的必测场景

- 成功路径（含关键字段断言）；
- 参数校验失败（400）；
- 业务失败（409 / 404 等）；
- 未登录 / 无权限（M2 起，401 / 403）；
- 租户隔离（涉及租户数据时，**必须**）；
- 幂等（写命令，重复 `requestId` 只产生一条数据）；
- traceId 响应头存在。

### 16.4 测试数据

- **禁止**依赖测试执行顺序。
- **禁止**依赖数据库自增 ID 的固定值。
- 每条测试**必须**生成唯一数据（`System.currentTimeMillis()` / UUID）。
- 测试结束**建议**清理自己创建的数据；H2 内存库每次启动是干净的。
- **禁止**在测试中使用真实客户数据。

### 16.5 执行

提交前**必须**执行：

```powershell
.\mvnw.cmd clean test
```

只跑某个模块：

```powershell
.\mvnw.cmd -pl web -am test
```

要求：

- 全部测试通过才能提交。
- 新增测试失败**禁止**通过 `@Disabled` 绕过；确需跳过**必须**在提交说明中写明原因和恢复时间。
- 修改公共能力（`common`）**必须**跑全量测试。

### 16.6 覆盖率

- 🟡 M6 目标：核心域（状态机、工单、权限）行覆盖率 ≥ 70%。
- 当前不强制覆盖率门槛，但**禁止**新增核心逻辑没有任何测试。
- 覆盖率不是目的；租户隔离、并发、幂等的关键路径**必须**有测试。

---

## 17. 依赖管理

### 17.1 版本规则

- 所有第三方版本**必须**在根 `pom.xml` 统一声明。
- 子模块**禁止**写已管理依赖的 `<version>`。
- **禁止**使用 `LATEST` / `RELEASE` / 动态版本。
- 自有模块版本统一 `${project.version}`。
- **禁止**引入 SNAPSHOT 第三方依赖。

### 17.2 新增依赖的评审

新增依赖前**必须**回答：

1. 解决什么问题？现有依赖能否替代？
2. 是否兼容 Java 21 / Spring Boot 4？
3. 许可证是否允许？
4. 是否活跃维护、有无已知漏洞？
5. 包体积/启动开销是否可接受？
6. 是否会在多个模块重复引入？

新增后**必须**：

- 更新根 `pom.xml` 的 `dependencyManagement`；
- 更新本文 2.1 节和设计文档技术栈；
- 在提交说明中写明理由。

### 17.3 JSON 库

- Web 层序列化/反序列化**必须**使用 Jackson 3（Boot 默认）。
- 当前未引入其它 JSON 库；如需引入 Fastjson2 等，**必须**按 17.2 评审，并只在明确的内部场景使用，**禁止**在 Controller 返回值上混用。
- **禁止**同时引 Jackson 2 和 Jackson 3 的 databind。
- JSON 字段命名统一 camelCase；数据库 snake_case 由 MyBatis-Flex 映射。

---

## 18. Git 规范

> 仓库已初始化，主分支为 `main`；`.gitignore` 已就绪。

### 18.1 分支

| 分支 | 用途 |
|---|---|
| `main` | 稳定分支，始终可构建、测试通过 |
| `feat/{milestone}-{feature}` | 新功能，如 `feat/m2-login` |
| `fix/{issue}` | 修复，如 `fix/tenant-isolation` |
| `docs/{topic}` | 文档，如 `docs/standards` |
| `chore/{topic}` | 构建、依赖、脚本 |
| `refactor/{topic}` | 不改变行为的重构 |

- **禁止**直接在 `main` 上开发大功能。
- **禁止**对 `main` 强制推送或重写历史。
- 个人学习项目可以不发 PR，但**必须**在合并前自查 diff。

### 18.2 提交信息

采用 Conventional Commits，scope 用模块名：

```text
feat(account): 新增租户启停接口
fix(core): 修复并发派单重复问题
docs(standards): 新增开发规范与评审清单
test(web): 补充租户隔离测试
refactor(common): 抽取分页转换方法
chore(deps): 升级 MyBatis-Flex 到 1.11.8
```

规则：

- 一行标题不超过 72 字符。
- 标题用祈使句，说明"做了什么"，不加句号。
- 正文说明 **why**、影响范围、验证方式；数据库变更必须写回滚方式。
- 一个提交只做一件事；格式化、重命名、功能修改不要混在一起。
- **禁止** `update`、`fix bug`、`修改` 这类无信息量的提交信息。

示例：

```text
feat(core): 新增工单派单接口

- 状态机校验 PENDING -> ASSIGNED
- 乐观锁更新 version 和 status
- 写 work_order_log 和 work_order_dispatch
- 新增租户隔离测试与并发测试

验证：.\mvnw.cmd clean test
```

### 18.3 提交前检查

- [ ] `.\mvnw.cmd clean test` 通过；
- [ ] `git diff` 已自查，无调试代码、无无关改动；
- [ ] 无 `target/`、`.idea/`、`.env`、日志、真实数据；
- [ ] 无密码、token、密钥；
- [ ] 新增/修改接口已更新 `api-examples.http`；
- [ ] 表结构变更已同步 MySQL + H2 schema；
- [ ] 文档索引、README、里程碑状态已更新。

### 18.4 禁止事项

- **禁止**提交 `target/`、`.idea/`、`*.iml`、`*.log`、`.env`；
- **禁止**提交本地数据库文件、MinIO 数据、导出文件；
- **禁止**提交注释掉的大段代码；
- **禁止**提交与当前任务无关的格式化改动；
- **禁止**在提交信息中写真实客户信息、内部地址、密钥。

---

## 19. 前端规范（预留，🟡）

当前仓库没有前端代码。后续新增前端时，按本节执行。

### 19.1 目录与工程

- 前端**必须**放在独立目录（如 `frontend/`）或独立仓库，**禁止**塞进 `web` Maven 模块。
- 技术栈确定后**必须**更新本文和 README。
- 开发环境通过代理转发 `/api`、`/platform/api`、`/tenant/api`、`/technician/api`，**禁止**在前端硬编码后端地址。

### 19.2 接口对接

- 所有业务请求**必须**使用 `POST + application/json`。
- 登录后**必须**携带 `satoken` 和 `client-code`。
- 写命令**必须**携带 `X-Request-Id`，重试时复用。
- 统一处理 `CommonResponse`：`code === 0` 成功，其它按错误码提示。
- `401` / `501` 统一跳转登录；`403` 提示无权限；`409` 提示刷新重试。
- 时间字段按 `yyyy-MM-dd HH:mm:ss` 解析和展示。
- 分页字段统一 `pageNum` / `pageSize` / `records` / `total`。
- ID 字段按字符串处理（`uid`、`orderNo`），**禁止**转成 Number（避免精度问题）。

### 19.3 安全

- Token 存储位置**必须**评估 XSS/CSRF 风险；**禁止**把密钥、AppSecret 放前端。
- **禁止**在前端做权限的最终判断；权限由后端强制执行。
- **禁止**在 URL 中传敏感信息。
- 文件上传**必须**先走后端预签名，再直传对象存储。

---

## 20. AI 协作规范

使用 OpenCode / 其它 AI 工具参与开发时，**必须**遵守：

### 20.1 开始前

- 先阅读本文、相关模块代码、设计文档对应章节。
- 明确任务边界：要改哪些文件、不改哪些文件。
- **禁止**让 AI 在未读代码的情况下凭空生成接口、表结构、字段名、版本号。

### 20.2 生成代码

- 生成代码**必须**符合本文的包结构、命名、分层、异常、日志、多租户规则。
- **禁止** AI 擅自：
  - 新增/升级依赖；
  - 修改表结构；
  - 重构无关模块；
  - 删除测试或加 `@Disabled`；
  - 修改安全/权限配置。
- **禁止**生成占位实现、假接口、未使用的类/方法/依赖。
- **禁止**生成没有里程碑标注的 TODO。

### 20.3 测试与验证

- AI 改完代码**必须**运行 `.\mvnw.cmd clean test`。
- 无法运行（缺少环境/依赖）时**必须**在回复中明确说明，不能默认"应该没问题"。
- 新增接口**必须**补测试；涉及租户数据**必须**补隔离测试。
- AI 生成的测试**必须**能真实失败（不能只断言 `code == 0` 而不断言业务字段）。

### 20.4 人工审查

合并前**必须**人工检查 diff：

- 是否越权、是否串租户；
- 是否有 SQL 注入、批量赋值、敏感信息泄漏；
- 事务边界、幂等、并发是否正确；
- 是否有多余依赖、调试代码、无关格式化；
- 文档、schema、`.http` 是否同步。

### 20.5 数据与隐私

- **禁止**把真实密钥、token、客户数据、生产地址贴进对话。
- 需要示例数据时使用假数据（如 `13800000000`）。
- AI 生成的配置**必须**人工确认没有真实凭据。

### 20.6 冲突处理

- AI 输出与本文冲突时，以本文为准。
- 确需例外时，**必须**在提交说明或评审中写明原因，并按附录 B 登记。
- **禁止**为了"让 AI 跑通"而降低规范（如关闭租户插件、跳过测试、放开权限）。

---

## 21. 评审清单与 Definition of Done

### 21.1 每次提交前（必须）

- [ ] 代码符合包结构、命名、分层规范；
- [ ] 没有 Controller 注入 Mapper、没有 Entity 出接口；
- [ ] 构造器注入，无字段 `@Autowired`；
- [ ] 无 `System.out`、`printStackTrace`、注释掉的代码；
- [ ] 新接口有 `@Tag`、`@Operation`、`@Schema`；
- [ ] 请求参数有 `@Valid` 和校验注解；
- [ ] 统一 `CommonResponse`，HTTP 200 + body code；
- [ ] 业务异常用 `BusinessException`，无堆栈泄漏；
- [ ] 多租户：业务表继承 `TenantEntity`，全局表登记 `ignore-tables`；
- [ ] 按 `uid` 查询仍带租户条件；
- [ ] `@IgnoreTenant` 仅平台端且有人工租户条件 + 审计；
- [ ] 写方法有 `@Transactional(rollbackFor = Exception.class)`；
- [ ] 无第三方调用在事务内；
- [ ] 核心写操作有乐观锁/幂等（如适用）；
- [ ] 日志用占位符，无敏感信息；
- [ ] 表结构变更已同步 `schema.sql` + `h2-schema.sql`；
- [ ] 新接口有成功、失败、租户隔离测试；
- [ ] `.\mvnw.cmd clean test` 通过；
- [ ] `api-examples.http`、README、docs 已更新；
- [ ] 无 `target/`、`.idea/`、`.env`、密钥进入提交。

### 21.2 合并/里程碑验收

- [ ] 设计文档与实际实现一致；
- [ ] 数据库 schema 可从零初始化（`docker compose down -v && up -d`）；
- [ ] Swagger 可访问且分组正确；
- [ ] 本地 `local` 和 `dev` 都能启动；
- [ ] 关键链路有集成测试；
- [ ] 已知偏离项已登记在附录 B；
- [ ] 里程碑状态在 `README.md` / `docs/README.md` 更新；
- [ ] Git tag 打在该里程碑验收提交上（如 `m1`、`m2`）。

### 21.3 代码评审关注顺序

1. 正确性：租户、权限、事务、并发、幂等；
2. 安全：敏感数据、SQL 注入、批量赋值、密钥；
3. 可维护性：分层、命名、重复代码；
4. 可观测性：日志、traceId、错误信息；
5. 测试：是否覆盖失败路径和隔离；
6. 文档与 schema 同步。

---

## 附录 A：新增一个 CRUD 的标准步骤

以租户端"客户管理"为例。**必须**按顺序完成，缺一不可。

### A.1 设计表

先在设计文档确认字段，再写 DDL。客户表至少包含：`uid`、`tenant_id`、`customer_code`、`name`、`contact_name`、`contact_mobile`、`status`、审计字段、`is_delete`。

### A.2 写 SQL

`docs/sql/schema.sql`：

```sql
CREATE TABLE IF NOT EXISTS `customer` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`            VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`      VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `customer_code`  VARCHAR(32)     NOT NULL COMMENT '客户编码，租户内唯一',
    `name`           VARCHAR(128)    NOT NULL COMMENT '客户名称',
    `contact_name`   VARCHAR(64)     DEFAULT NULL COMMENT '联系人',
    `contact_mobile` VARCHAR(128)    DEFAULT NULL COMMENT '联系人手机号（SM4 密文）',
    `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `remark`         VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`      VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`      VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_customer_code` (`tenant_id`, `customer_code`),
    KEY `idx_tenant_name` (`tenant_id`, `name`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户';
```

同步 `web/src/main/resources/db/h2-schema.sql`（H2 语法做等价简化）。

### A.3 Entity

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "customer")
public class CustomerEntity extends TenantEntity {

    private String uid;

    private String customerCode;

    private String name;

    private String contactName;

    private String contactMobile;

    private Integer status;
}
```

### A.4 Mapper

```java
@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {
}
```

### A.5 Param / VO

```java
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "客户分页参数")
public class CustomerPageParam extends PageParam {

    @Schema(description = "关键字：客户名称/编码")
    private String keyword;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
```

```java
@Data
@Schema(description = "新增客户参数")
public class CustomerCreateParam {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 32, message = "客户编码长度不能超过 32")
    @Schema(description = "客户编码", example = "C0001")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称长度不能超过 128")
    @Schema(description = "客户名称", example = "示例客户")
    private String name;

    @Size(max = 64, message = "联系人长度不能超过 64")
    @Schema(description = "联系人")
    private String contactName;
}
```

```java
@Data
@Schema(description = "客户信息")
public class CustomerVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "客户业务ID")
    private String uid;

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "客户名称")
    private String name;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系人手机号（脱敏）")
    private String contactMobile;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
```

### A.6 Service

```java
public interface CustomerService {

    PageResult<CustomerVO> page(CustomerPageParam param);

    CustomerVO detail(String uid);

    String create(CustomerCreateParam param);
}
```

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<CustomerVO> page(CustomerPageParam param) {
        QueryWrapper query = QueryWrapper.create()
                .eq(CustomerEntity::getStatus, param.getStatus(), param.getStatus() != null)
                .like(CustomerEntity::getName, param.getKeyword(), StrUtil.isNotBlank(param.getKeyword()))
                .orderBy(CustomerEntity::getCreateTime, false);

        Page<CustomerEntity> page = customerMapper.paginate(param.getPageNum(), param.getPageSize(), query);
        return PageResult.of(page.map(this::toVO));
    }

    @Override
    public CustomerVO detail(String uid) {
        return toVO(getByUid(uid));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CustomerCreateParam param) {
        long exists = customerMapper.selectCountByQuery(
                QueryWrapper.create().eq(CustomerEntity::getCustomerCode, param.getCustomerCode()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "客户编码已存在");
        }

        CustomerEntity entity = new CustomerEntity();
        BeanUtil.copyProperties(param, entity);
        entity.setUid(IdUtil.fastSimpleUUID());
        entity.setStatus(1);
        customerMapper.insert(entity);

        log.info("创建客户成功: customerCode={}, uid={}", entity.getCustomerCode(), entity.getUid());
        return entity.getUid();
    }

    private CustomerEntity getByUid(String uid) {
        if (StrUtil.isBlank(uid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "uid 不能为空");
        }
        CustomerEntity entity = customerMapper.selectOneByQuery(
                QueryWrapper.create().eq(CustomerEntity::getUid, uid));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在");
        }
        return entity;
    }

    private CustomerVO toVO(CustomerEntity entity) {
        CustomerVO vo = new CustomerVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
```

### A.7 Controller

```java
@RestController
@RequestMapping("/tenant/api/customer")
@RequiredArgsConstructor
@Tag(name = "租户端-客户管理")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/page")
    @Operation(summary = "客户分页")
    public CommonResponse<PageResult<CustomerVO>> page(@Valid @RequestBody CustomerPageParam param) {
        return CommonResponse.ok(customerService.page(param));
    }

    @PostMapping("/detail")
    @Operation(summary = "客户详情")
    public CommonResponse<CustomerVO> detail(@Valid @RequestBody CustomerDetailParam param) {
        return CommonResponse.ok(customerService.detail(param.getUid()));
    }

    @PostMapping("/create")
    @Operation(summary = "新增客户")
    public CommonResponse<String> create(@Valid @RequestBody CustomerCreateParam param) {
        return CommonResponse.ok(customerService.create(param));
    }
}
```

> `CustomerDetailParam` 只包含 `@NotBlank uid`，按 5.5 规范单独定义。

### A.8 Swagger 分组

如果 `customer` 在 `core` 模块，**必须**在 `application.yml` 增加：

```yaml
springdoc:
  group-configs:
    - group: core
      display-name: 工单核心
      paths-to-match: /**
      packages-to-scan: com.lty.workorder.core.controller
```

### A.9 测试

`CustomerApiTest` 至少覆盖：

- 创建成功，返回 `uid`；
- 分页能查到刚创建的数据；
- 详情不存在返回 404；
- 编码重复返回 409；
- 租户 B 查租户 A 的 `uid` 返回 404；
- 分页不返回其它租户数据。

### A.10 文档与索引

- 更新 `docs/design/api-examples.http`；
- 更新 `docs/README.md` 文档索引（如有新文档）；
- 更新 `README.md` 当前进度；
- 更新本文（如果新增了通用规则或依赖）。

---

## 附录 B：现状例外清单

> 以下问题在 M1 已存在。本文只约束**新增和改动**的代码；存量按"处理时机"逐步修正，不做全量重构。

| # | 现状 | 规范要求 | 处理时机 |
|---|---|---|---|
| B.1 | 设计示例使用 ULID / 前缀 ID，代码使用 `IdUtil.fastSimpleUUID()` | `uid` 统一 32 位小写十六进制 UUID；业务编号单独生成 | 后续维护设计文档示例时统一 |
| B.2 | `springdoc` 分组目前只有 `account` 和 `web` | 每个有 Controller 的模块必须有分组 | 新增 `core` Controller 时补 |
| B.3 | `sys_tenant.contact_mobile` 仍是明文存储（VO 已脱敏） | 存储层 M5 改 SM4 密文，字段统一为 `_cipher` 命名 | M5 加密阶段 |
| B.4 | `AuditFieldListener` 的 `create_by/update_by` 固定为 `system` | M2 从登录态取当前用户 | M2 接入 Sa-Token 后 |

**例外管理规则**：

1. 新增例外**必须**在本表登记，写明现状、要求、处理时机。
2. **禁止**以"存量代码就这样"为由拒绝遵守新规范。
3. 例外修复后**必须**从本表删除，并在提交说明中注明。
4. 与安全、租户隔离相关的例外**必须**优先处理，不能拖延到后续里程碑。

---

## 附录 C：常用命令

### C.1 构建与测试

```powershell
# 全量测试
.\mvnw.cmd clean test

# 跳过测试打包
.\mvnw.cmd -DskipTests package

# 只测 web 模块（同时构建依赖模块）
.\mvnw.cmd -pl web -am test

# 本地安装依赖模块
.\mvnw.cmd install -DskipTests
```

### C.2 本地启动

```powershell
# H2（默认 local）
java -jar web/target/lty-workorder.jar --spring.profiles.active=local

# MySQL（dev）
java -jar web/target/lty-workorder.jar --spring.profiles.active=dev

# IDE 直接运行 WebApplication，程序参数：
--spring.profiles.active=local
```

### C.3 Maven 插件启动

```powershell
.\mvnw.cmd install -DskipTests
.\mvnw.cmd -pl web spring-boot:run "-Dspring-boot.run.profiles=local"
```

### C.4 Docker

```powershell
copy .env.example .env
docker compose up -d
docker compose down
docker compose down -v   # 连同数据卷一起删除，重置数据库
```

### C.5 本地访问

| 服务 | 地址 |
|---|---|
| 应用 | `http://localhost:8090` |
| 健康检查 | `http://localhost:8090/api/health` |
| Swagger | `http://localhost:8090/doc.html` |
| H2 控制台 | `http://localhost:8090/h2-console` |
| MySQL | `localhost:3306`，库 `lty_workorder` |
| Redis | `localhost:6379` |
| MinIO API | `http://localhost:9000` |
| MinIO 控制台 | `http://localhost:9001` |

### C.6 排错入口

| 现象 | 先看 |
|---|---|
| 启动失败 | `application-{profile}.yml`、端口占用、数据库连接 |
| 接口 404 | Controller 是否在扫描包、URL 前缀、`@RequestMapping` |
| 租户数据不对 | `TenantContext`、`ignore-tables`、实体是否继承 `TenantEntity` |
| 事务不生效 | 是否 public、是否自调用、是否加在 Controller |
| 慢查询 | MyBatis-Flex SQL 日志、`EXPLAIN`、索引 |
| 测试失败 | `@ActiveProfiles`、H2 schema 是否同步、测试数据是否唯一 |

---

## 附录 D：参考文件

| 文件 | 用途 |
|---|---|
| `pom.xml` | 版本基线、依赖管理、编译插件 |
| `common/src/main/java/com/lty/workorder/common/domain/CommonResponse.java` | 统一响应 |
| `common/src/main/java/com/lty/workorder/common/exception/GlobalExceptionHandler.java` | 全局异常 |
| `common/src/main/java/com/lty/workorder/common/tenant/TenantContext.java` | 租户上下文 |
| `common/src/main/java/com/lty/workorder/common/filter/TraceIdFilter.java` | traceId |
| `account/src/main/java/com/lty/workorder/account/controller/SysTenantController.java` | 分层参考 |
| `account/src/main/java/com/lty/workorder/account/service/impl/SysTenantServiceImpl.java` | Service 参考 |
| `docs/sql/schema.sql` | MySQL schema |
| `web/src/main/resources/db/h2-schema.sql` | H2 schema |
| `docs/design/api-examples.http` | 接口示例 |
| `docs/standards/review-checklist.md` | 提交前检查 |

---

**变更记录**

| 版本 | 日期 | 说明 |
|---|---|---|
| v0.1 | 2026-11-19 | 首版：基于 M1 代码和总体设计制定 |
| v0.2 | 2026-11-19 | 同步租户 Param 拆分、异常映射、SQL 审计、依赖清理与手机号脱敏 |
| v0.3 | 2026-11-19 | 完善注释规范与评审清单，补齐类级 Javadoc |
