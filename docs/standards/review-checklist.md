# 提交前评审清单

> 配套 [开发规范](development-standard.md)。每次提交/合并前逐项勾选，不适用的标 `N/A`。

## 1. 结构与命名

- [ ] 包结构、类名、方法名符合规范
- [ ] Controller 没有注入 Mapper，没有业务逻辑
- [ ] Service 是接口 + 实现，事务加在实现类 public 方法
- [ ] 没有 Entity 直接作为接口返回值
- [ ] 构造器注入，无字段 `@Autowired`
- [ ] 无 `System.out`、`printStackTrace`、注释掉的代码
- [ ] public 类有类级 Javadoc，复杂方法说明约束/副作用
- [ ] 注释与代码一致，TODO 带里程碑，无裸 FIXME

## 2. API 与异常

- [ ] 业务接口使用 `POST + JSON`（健康检查/下载/SSE 除外）
- [ ] URL 符合 `/platform|tenant|technician/api/{资源}/{动作}`
- [ ] 返回统一 `CommonResponse`，没有 `new CommonResponse`
- [ ] 参数有 `@Valid` 和校验注解，错误消息为中文
- [ ] Controller 有 `@Tag`，方法有 `@Operation`，字段有 `@Schema`
- [ ] 业务失败抛 `BusinessException`，没有吞异常
- [ ] 响应不包含堆栈、SQL、内部类名
- [ ] 分页使用 `PageParam` / `PageResult`，`pageSize` 不超过 200
- [ ] 时间格式为 `yyyy-MM-dd HH:mm:ss`
- [ ] 写命令要求 `X-Request-Id`（M3 起）

## 3. 多租户

- [ ] 业务表有 `tenant_id`，实体继承 `TenantEntity`
- [ ] 全局表继承 `BaseEntity` 并加入 `ignore-tables`
- [ ] `tenantId` 不来自请求体/Query/Header
- [ ] 按 `uid` 查询仍带租户条件
- [ ] `@IgnoreTenant` 仅用于平台端，且有权限校验 + 手动租户条件 + 审计
- [ ] MQ / Job / 异步线程设置并清理 `TenantContext` 和 MDC
- [ ] 有租户隔离测试：A 的数据 B 查不到

## 4. 数据库

- [ ] 表名/字段名/索引名符合规范
- [ ] 字段有中文 `COMMENT`
- [ ] 业务主表有审计字段和 `is_delete`
- [ ] 核心表有 `version` 乐观锁
- [ ] 联合索引以 `tenant_id` 开头
- [ ] 无数据库外键
- [ ] 无 `SELECT *`、无 `${}`、无字符串拼接 SQL
- [ ] `docs/sql/schema.sql` 与 `web/src/main/resources/db/h2-schema.sql` 已同步
- [ ] 种子数据按需更新 `h2-data.sql` / `02-seed.sql`

## 5. 事务、并发、幂等

- [ ] 写方法有 `@Transactional(rollbackFor = Exception.class)`
- [ ] 事务内没有短信/OSS/OCR/HTTP 第三方调用
- [ ] 没有自调用事务方法
- [ ] 核心写操作使用乐观锁或分布式锁
- [ ] 写命令有幂等记录（M3 起）
- [ ] MQ 消息在事务提交后发送（M4 起）

## 6. 日志与安全

- [ ] 日志使用占位符，无字符串拼接
- [ ] 日志无密码、token、完整手机号、身份证号
- [ ] traceId 由 MDC 自动输出，没有手写
- [ ] 权限注解已加（M2 起）
- [ ] 无硬编码密码/密钥/真实地址
- [ ] 文件上传有类型和大小校验（M5 起）
- [ ] 生产 Swagger 已关闭
- [ ] 敏感字段加密/脱敏（M5 起）

## 7. 测试

- [ ] 新增/修改接口有成功路径测试
- [ ] 有参数校验失败测试
- [ ] 有业务失败（404/409）测试
- [ ] 有租户隔离测试（涉及租户数据）
- [ ] 有幂等测试（写命令）
- [ ] 测试数据唯一，不依赖执行顺序
- [ ] `.\mvnw.cmd clean test` 全部通过

## 8. 文档与提交

- [ ] 新增/修改接口已更新 `docs/design/api-examples.http`
- [ ] 表结构变化已更新设计文档
- [ ] 里程碑状态已更新 `README.md` / `docs/README.md`
- [ ] `git diff` 已自查，无无关改动
- [ ] 未提交 `target/`、`.idea/`、`.env`、日志、真实数据
- [ ] 提交信息符合 Conventional Commits
- [ ] 数据库变更在提交说明中写了回滚方式

## 9. AI 协作（如使用）

- [ ] 已阅读规范和对应模块代码
- [ ] AI 没有擅自新增依赖、改表结构、重构无关代码
- [ ] AI 生成的代码已人工审查 diff
- [ ] AI 已运行测试，或明确说明未运行及原因
- [ ] 没有把真实密钥/客户数据贴进对话
- [ ] 冲突处按规范执行，例外已登记
