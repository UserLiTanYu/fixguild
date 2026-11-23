# lty-workorder

多租户维保工单系统，基于 `kld-transport` 的技术栈做一个学习型项目。

## 当前进度

- ✅ **M1**：多模块骨架 + 统一响应/异常/TraceId + MyBatis-Flex + springdoc + `sys_tenant` CRUD
- 🟡 **M2**（进行中）：登录基础完成，RBAC、权限注解、数据范围待完成
- ⬜ **M3–M6**
- ⬜ **M7**（可选）

详细任务与验收见 [开发计划总览](docs/plan/00-开发计划总览.md)。

## 技术栈

Java 21 · Spring Boot 4.0.5 · MyBatis-Flex 1.11.8 · springdoc 3.0.3 · Hutool · MySQL 8 · Redis 7 · MinIO · Maven Wrapper

## 文档

- [开发规范](docs/standards/development-standard.md)
- [评审清单](docs/standards/review-checklist.md)
- [总体设计](docs/design/00-总体设计.md)
- [开发计划](docs/plan/00-开发计划总览.md)
- [数据库 DDL](docs/sql/schema.sql)
- [接口示例](docs/design/api-examples.http)
- [文档索引](docs/README.md)

## 快速开始

### 方式一：本地 H2（无需 Docker）

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -DskipTests package
java -jar web/target/lty-workorder.jar --spring.profiles.active=local
```

也可以在 IDE 里直接运行 `WebApplication`，程序参数加：

```text
--spring.profiles.active=local
```

如果要用 `spring-boot:run`，先 `install` 再只对 `web` 模块启动：

```powershell
.\mvnw.cmd install -DskipTests
.\mvnw.cmd -pl web spring-boot:run "-Dspring-boot.run.profiles=local"
```

访问：

- 健康检查：`http://localhost:8090/api/health`
- Swagger：`http://localhost:8090/doc.html`（会跳转到 `/swagger-ui/index.html`）
- H2 控制台：`http://localhost:8090/h2-console`
  - JDBC URL：`jdbc:h2:mem:lty_workorder;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1`
  - 用户名：`sa`
  - 密码：空

### 方式二：MySQL + Redis + MinIO（Docker）

```powershell
copy .env.example .env
docker compose up -d
.\mvnw.cmd -DskipTests package
java -jar web/target/lty-workorder.jar --spring.profiles.active=dev
```

`application.yml` 默认激活 `local` profile（H2）；连接 MySQL 时显式指定 `dev` profile。

默认端口：

| 服务 | 地址 |
|---|---|
| 应用 | `http://localhost:8090` |
| MySQL | `localhost:3306`，库 `lty_workorder` |
| Redis | `localhost:6379` |
| MinIO API | `http://localhost:9000` |
| MinIO 控制台 | `http://localhost:9001` |

> MySQL 首次启动会自动执行 `docs/sql/schema.sql` 和 `docker/mysql/init/02-seed.sql`。  
> 需要重置数据时执行 `docker compose down -v`。

## M1 已实现

> 详细交付物、验收结果与已知限制见 [M1 完成文档](docs/plan/01-M1-工程骨架与基础设施.md)。

- 4 个 Maven 模块：`common`、`account`、`core`、`web`
- 统一响应 `CommonResponse<T>`、分页 `PageResult<T>`
- 全局异常处理 `GlobalExceptionHandler`
- `TraceIdFilter`：每个请求生成 `traceId`，写入 MDC 和响应头 `X-Trace-Id`
- MyBatis-Flex：分页、逻辑删除、审计字段、SQL 审计日志
- 多租户骨架：`TenantContext` + `MyTenantFactory`（M2 接登录态）
- `sys_tenant` 完整 CRUD：分页、新增、编辑、详情、启停
- springdoc OpenAPI 文档 + `/doc.html` 兼容跳转
- H2 本地 profile + MySQL dev profile
- Maven Wrapper，无需本机安装 Maven
- 集成测试：健康检查 + 租户 CRUD

## 核心接口（M1）

| 功能 | 方法 | 路径 |
|---|---|---|
| 健康检查 | GET | `/api/health` |
| 租户分页 | POST | `/platform/api/tenant/page` |
| 新增租户 | POST | `/platform/api/tenant/create` |
| 编辑租户 | POST | `/platform/api/tenant/update` |
| 租户详情 | POST | `/platform/api/tenant/detail` |
| 启停租户 | POST | `/platform/api/tenant/status` |

请求示例见 `docs/design/api-examples.http`。

## 目录结构

```text
lty-workorder/
├── common/          # 基础能力：响应、异常、租户、TraceId、MyBatis-Flex、OpenAPI
├── account/         # 租户、用户、角色、菜单、登录
├── core/            # 工单、派工、客户、设备、技师、附件
├── web/             # 启动模块 + 跨模块配置 + 公共接口
├── docs/            # 设计文档 + SQL
├── docker/          # 本地初始化脚本
├── docker-compose.yml
├── mvnw / mvnw.cmd
└── pom.xml
```
