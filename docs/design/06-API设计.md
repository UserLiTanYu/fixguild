# 7. API 设计

> 所属：[总体设计](00-总体设计.md) · 原文章节：§7

## 7.1 通用约定

- 除 SSE、文件下载跳转外，**统一 POST + JSON**（沿用原项目约定，便于对比学习）；
- Header：
  - `satoken`：登录 token；
  - `client-code`：`platform` / `tenant` / `technician`；
  - `X-Request-Id`：请求幂等 ID（写操作必传）；
- 响应统一：

```json
{
  "code": 0,
  "msg": "success",
  "data": {},
  "traceId": "01J..."
}
```

- 分页请求：

```json
{
  "pageNum": 1,
  "pageSize": 20
}
```

- 分页响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "records": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

- 对外 ID 用 `uid` 或 `orderNo`，不用自增 `id`；
- 时间格式：`yyyy-MM-dd HH:mm:ss`。

## 7.2 认证接口

| 功能 | 路径 | 权限 |
|---|---|---|
| 平台登录 | `POST /account/api/platform/login` | 白名单 |
| 租户登录 | `POST /account/api/tenant/login` | 白名单 |
| 技师登录 | `POST /account/api/technician/login` | 白名单 |
| 发送短信 | `POST /account/api/{client}/sms/send` | 白名单 + 限流 |
| 当前用户 | `POST /account/api/{client}/user/info` | 登录 |
| 权限菜单 | `POST /account/api/{client}/user/auth` | 登录 |
| 退出 | `POST /account/api/{client}/logout` | 登录 |
| 修改密码 | `POST /account/api/{client}/password/change` | 登录 |

## 7.3 平台端接口

| 功能 | 路径 | 权限 |
|---|---|---|
| 租户分页 | `POST /platform/api/tenant/page` | `platform:tenant:page` |
| 租户新增（含初始管理员） | `POST /platform/api/tenant/create` | `platform:tenant:create` |
| 租户编辑 | `POST /platform/api/tenant/update` | `platform:tenant:update` |
| 租户启停 | `POST /platform/api/tenant/status` | `platform:tenant:status` |
| 租户详情 | `POST /platform/api/tenant/detail` | `platform:tenant:detail` |
| 全局字典分页 | `POST /platform/api/dict/type/page` | `platform:dict:page` |
| 字典数据维护 | `POST /platform/api/dict/data/*` | `platform:dict:edit` |
| 全局工单分页 | `POST /platform/api/work-order/page` | `platform:work-order:page` |
| 全局工单详情 | `POST /platform/api/work-order/detail` | `platform:work-order:detail` |
| 全局统计 | `POST /platform/api/statistics/overview` | `platform:statistics:view` |
| MQ 失败重试 | `POST /platform/api/mq/fail/retry` | `platform:mq:retry` |

> 平台跨租户查询必须走 `@IgnoreTenant`，并在操作日志中记录。
>
> 创建租户时会同步创建租户端初始管理员，响应中的 `initialPassword` 只返回一次，
> 数据库仅保存 BCrypt 哈希；默认管理员角色与菜单权限在 RBAC 任务中补齐。

## 7.4 租户端接口

| 功能 | 路径 | 权限 |
|---|---|---|
| 用户分页 | `POST /tenant/api/user/page` | `tenant:user:page` |
| 用户新增 | `POST /tenant/api/user/create` | `tenant:user:create` |
| 用户编辑 | `POST /tenant/api/user/update` | `tenant:user:update` |
| 重置密码 | `POST /tenant/api/user/reset-password` | `tenant:user:reset` |
| 用户启停 | `POST /tenant/api/user/status` | `tenant:user:status` |
| 用户删除 | `POST /tenant/api/user/delete` | `tenant:user:delete` |
| 角色分页 | `POST /tenant/api/role/page` | `tenant:role:page` |
| 角色新增 | `POST /tenant/api/role/create` | `tenant:role:create` |
| 角色编辑 | `POST /tenant/api/role/update` | `tenant:role:update` |
| 角色分配菜单 | `POST /tenant/api/role/menus` | `tenant:role:menus` |
| 角色删除 | `POST /tenant/api/role/delete` | `tenant:role:delete` |
| 菜单树 | `POST /tenant/api/menu/tree` | `tenant:menu:tree` |
| 技师分页 | `POST /tenant/api/technician/page` | `tenant:technician:page` |
| 技师新增 | `POST /tenant/api/technician/create` | `tenant:technician:create` |
| 技师编辑 | `POST /tenant/api/technician/update` | `tenant:technician:update` |
| 技师启停 | `POST /tenant/api/technician/status` | `tenant:technician:status` |
| 技师工作量 | `POST /tenant/api/technician/workload` | `tenant:technician:page` |
| 客户分页 | `POST /tenant/api/customer/page` | `tenant:customer:page` |
| 客户新增 | `POST /tenant/api/customer/create` | `tenant:customer:create` |
| 客户编辑 | `POST /tenant/api/customer/update` | `tenant:customer:update` |
| 客户详情 | `POST /tenant/api/customer/detail` | `tenant:customer:detail` |
| 设备分页 | `POST /tenant/api/equipment/page` | `tenant:equipment:page` |
| 设备新增 | `POST /tenant/api/equipment/create` | `tenant:equipment:create` |
| 设备编辑 | `POST /tenant/api/equipment/update` | `tenant:equipment:update` |
| 设备详情 | `POST /tenant/api/equipment/detail` | `tenant:equipment:detail` |
| 工单分页 | `POST /tenant/api/work-order/page` | `tenant:work-order:page` |
| 工单新增 | `POST /tenant/api/work-order/create` | `tenant:work-order:create` |
| 工单详情 | `POST /tenant/api/work-order/detail` | `tenant:work-order:detail` |
| 工单编辑 | `POST /tenant/api/work-order/update` | `tenant:work-order:update` |
| 派单 | `POST /tenant/api/work-order/assign` | `tenant:work-order:assign` |
| 取消 | `POST /tenant/api/work-order/cancel` | `tenant:work-order:cancel` |
| 确认 | `POST /tenant/api/work-order/confirm` | `tenant:work-order:confirm` |
| 返工 | `POST /tenant/api/work-order/reopen` | `tenant:work-order:reopen` |
| 工单日志 | `POST /tenant/api/work-order/log/page` | `tenant:work-order:log` |
| 工单附件 | `POST /tenant/api/work-order/attachment` | `tenant:work-order:attachment` |
| 工单导出 | `POST /tenant/api/work-order/export` | `tenant:work-order:export` |
| 工单统计 | `POST /tenant/api/work-order/statistics` | `tenant:work-order:statistics` |
| 通知分页 | `POST /tenant/api/notification/page` | `tenant:notification:page` |
| 通知已读 | `POST /tenant/api/notification/read` | `tenant:notification:read` |
| 配置列表 | `POST /tenant/api/config/list` | `tenant:config:list` |

> 租户端角色只管理自定义角色，系统内置角色只读；菜单树包含按钮节点，供角色分配菜单使用。
> 用户新增和重置密码返回的初始密码只出现一次，数据库只保存 BCrypt 哈希；停用用户或重置密码后目标用户登录态立即失效。

## 7.5 技师端接口

| 功能 | 路径 | 权限 |
|---|---|---|
| 我的工单分页 | `POST /technician/api/work-order/page` | `tech:work-order:page` |
| 工单详情 | `POST /technician/api/work-order/detail` | `tech:work-order:detail` |
| 接单 | `POST /technician/api/work-order/accept` | `tech:work-order:accept` |
| 拒单 | `POST /technician/api/work-order/reject` | `tech:work-order:reject` |
| 开始 | `POST /technician/api/work-order/start` | `tech:work-order:start` |
| 完工 | `POST /technician/api/work-order/complete` | `tech:work-order:complete` |
| 上传附件 | `POST /technician/api/work-order/attachment` | `tech:work-order:attachment` |
| 今日概览 | `POST /technician/api/work-order/today` | `tech:work-order:today` |
| 通知分页 | `POST /technician/api/notification/page` | `tech:notification:page` |
| 通知已读 | `POST /technician/api/notification/read` | `tech:notification:read` |

## 7.6 公共接口

| 功能 | 路径 | 权限 |
|---|---|---|
| 健康检查 | `GET /api/health` | 白名单 |
| 文件预签名 | `POST /api/file/presign` | 登录 |
| 文件确认 | `POST /api/file/confirm` | 登录 |
| 导出任务分页 | `POST /api/export/task/page` | 登录 |
| 导出任务状态 | `POST /api/export/task/status` | 登录 |
| 导出下载 | `POST /api/export/task/download` | 登录 |
| 字典数据 | `POST /api/dict/data/{dictType}` | 登录/白名单 |
| SSE 通知 | `GET /sse/notification` | 登录 |
| OpenAPI 回调 | `POST /open/api/callback/{type}` | 签名校验 |

## 7.7 核心接口示例

### 创建工单

```http
POST /tenant/api/work-order/create
satoken: xxx
client-code: tenant
X-Request-Id: 01JABCDEF...
Content-Type: application/json

{
  "customerUid": "C01J...",
  "equipmentUid": "E01J...",
  "title": "空调不制冷",
  "description": "3 楼会议室空调开机后不制冷，出风口有异响",
  "priority": 3,
  "expectTime": "2026-11-17 10:00:00",
  "attachments": [
    {
      "fileName": "1.jpg",
      "fileKey": "workorder/2026/11/16/xxx.jpg",
      "fileType": 1
    }
  ]
}
```

响应：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "orderUid": "WO01J...",
    "orderNo": "WO20261116000001"
  },
  "traceId": "01J..."
}
```

### 派单

```http
POST /tenant/api/work-order/assign
{
  "orderUid": "WO01J...",
  "technicianUid": "T01J...",
  "remark": "请尽快上门",
  "requestId": "01JABCDEF..."
}
```

### 接单

```http
POST /technician/api/work-order/accept
{
  "orderUid": "WO01J...",
  "requestId": "01JABCDEF..."
}
```

### 完工

```http
POST /technician/api/work-order/complete
{
  "orderUid": "WO01J...",
  "finishRemark": "更换电容，已恢复",
  "attachments": [
    {
      "fileName": "after.jpg",
      "fileKey": "workorder/2026/11/16/yyy.jpg",
      "fileType": 1
    }
  ],
  "requestId": "01JABCDEF..."
}
```

### 工单分页

```http
POST /tenant/api/work-order/page
{
  "pageNum": 1,
  "pageSize": 20,
  "status": "ASSIGNED",
  "priority": 3,
  "technicianUid": "T01J...",
  "keyword": "空调",
  "createTimeStart": "2026-11-01 00:00:00",
  "createTimeEnd": "2026-11-30 23:59:59"
}
```

---

上一篇：[05-数据库设计](05-数据库设计.md) · [返回总览](00-总体设计.md) · 下一篇：[07-关键时序](07-关键时序.md)
