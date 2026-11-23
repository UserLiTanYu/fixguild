-- =============================================================
-- 多租户维保工单系统 · 一期核心表
-- 项目：lty-workorder
-- 数据库：MySQL 8.0+
-- 说明：
--   1. 所有业务表带 tenant_id，平台数据统一 tenant_id = '0'
--   2. 对外使用 uid / order_no，不暴露自增 id
--   3. 逻辑删除统一 is_delete = 0/1
--   4. 不使用数据库外键，关联关系由应用保证
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 如果数据库名不是 lty_workorder，请同步修改下面一行
USE `lty_workorder`;

-- -------------------------------------------------------------
-- 1. 租户
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_tenant` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_code`   VARCHAR(32)     NOT NULL COMMENT '租户编码，业务表的 tenant_id 存这个值',
    `tenant_name`   VARCHAR(128)    NOT NULL COMMENT '租户名称',
    `contact_name`  VARCHAR(64)     DEFAULT NULL COMMENT '联系人',
    `contact_mobile` VARCHAR(128)   DEFAULT NULL COMMENT '联系人手机号（M5 前明文，M5 改 SM4 密文）',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `expire_time`   DATETIME        DEFAULT NULL COMMENT '到期时间',
    `max_user_count` INT            NOT NULL DEFAULT 50 COMMENT '最大用户数',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户';

-- -------------------------------------------------------------
-- 2. 用户
-- 平台用户 tenant_id = '0'；client_code 区分端
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户编码，平台为0',
    `client_code`   VARCHAR(32)     NOT NULL COMMENT '端：platform/tenant/technician',
    `user_code`     VARCHAR(32)     NOT NULL COMMENT '用户编码，Sa-Token loginId',
    `username`      VARCHAR(64)     NOT NULL COMMENT '登录名',
    `password`      VARCHAR(100)    NOT NULL COMMENT 'BCrypt 密码',
    `nickname`      VARCHAR(64)     NOT NULL COMMENT '昵称/姓名',
    `mobile_cipher` VARCHAR(128)    DEFAULT NULL COMMENT '手机号密文（SM4）',
    `mobile_hash`   VARCHAR(64)     DEFAULT NULL COMMENT '手机号哈希，用于精确查询',
    `email`         VARCHAR(128)    DEFAULT NULL COMMENT '邮箱',
    `avatar`        VARCHAR(255)    DEFAULT NULL COMMENT '头像',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `last_login_time` DATETIME      DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64)     DEFAULT NULL COMMENT '最后登录IP',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_user_code` (`user_code`),
    UNIQUE KEY `uk_tenant_client_username` (`tenant_id`, `client_code`, `username`),
    KEY `idx_tenant_client_status` (`tenant_id`, `client_code`, `status`),
    KEY `idx_mobile_hash` (`mobile_hash`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';

-- -------------------------------------------------------------
-- 3. 角色
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户编码，平台为0',
    `client_code`   VARCHAR(32)     NOT NULL COMMENT '端：platform/tenant/technician',
    `role_code`     VARCHAR(64)     NOT NULL COMMENT '角色编码',
    `role_name`     VARCHAR(64)     NOT NULL COMMENT '角色名称',
    `data_scope`    TINYINT         NOT NULL DEFAULT 2 COMMENT '数据范围：1全部 2本租户 3本人 4本客户',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `is_system`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_client_role` (`tenant_id`, `client_code`, `role_code`),
    KEY `idx_tenant_client_status` (`tenant_id`, `client_code`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色';

-- -------------------------------------------------------------
-- 4. 菜单/权限
-- 菜单是全局的，通过 client_code 区分端
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `client_code`   VARCHAR(32)     NOT NULL COMMENT '端：platform/tenant/technician',
    `parent_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
    `menu_name`     VARCHAR(64)     NOT NULL COMMENT '菜单名称',
    `menu_type`     CHAR(1)         NOT NULL COMMENT 'M目录 C菜单 F按钮',
    `path`          VARCHAR(200)    DEFAULT NULL COMMENT '路由地址',
    `component`     VARCHAR(200)    DEFAULT NULL COMMENT '前端组件',
    `perms`         VARCHAR(128)    DEFAULT NULL COMMENT '权限标识，如 tenant:work-order:assign',
    `icon`          VARCHAR(64)     DEFAULT NULL COMMENT '图标',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `visible`       TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `is_system`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_client_perms` (`client_code`, `perms`),
    KEY `idx_client_parent_sort` (`client_code`, `parent_id`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单/权限';

-- -------------------------------------------------------------
-- 5. 用户角色
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `role_id`       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_role` (`tenant_id`, `user_id`, `role_id`),
    KEY `idx_role` (`tenant_id`, `role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色';

-- -------------------------------------------------------------
-- 6. 角色菜单
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `role_id`       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    `menu_id`       BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_role_menu` (`tenant_id`, `role_id`, `menu_id`),
    KEY `idx_menu` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单';

-- -------------------------------------------------------------
-- 7. 字典类型
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `dict_type`     VARCHAR(64)     NOT NULL COMMENT '字典类型编码',
    `dict_name`     VARCHAR(64)     NOT NULL COMMENT '字典名称',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `is_system`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否系统内置',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型';

-- -------------------------------------------------------------
-- 8. 字典数据
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `dict_type`     VARCHAR(64)     NOT NULL COMMENT '字典类型编码',
    `dict_label`    VARCHAR(64)     NOT NULL COMMENT '字典标签',
    `dict_value`    VARCHAR(64)     NOT NULL COMMENT '字典值',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `css_class`     VARCHAR(64)     DEFAULT NULL COMMENT '样式类',
    `is_default`    TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_dict_data` (`dict_type`, `dict_value`),
    KEY `idx_dict_type_sort` (`dict_type`, `sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据';

-- -------------------------------------------------------------
-- 9. 租户配置
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户编码，平台为0',
    `config_key`    VARCHAR(128)    NOT NULL COMMENT '配置键',
    `config_value`  TEXT            DEFAULT NULL COMMENT '配置值',
    `config_type`   TINYINT         NOT NULL DEFAULT 1 COMMENT '1字符串 2数字 3布尔 4JSON',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_config` (`tenant_id`, `config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租户配置';

-- -------------------------------------------------------------
-- 10. 登录日志
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_login_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户编码',
    `user_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    `user_code`     VARCHAR(32)     DEFAULT NULL COMMENT '用户编码',
    `client_code`   VARCHAR(32)     NOT NULL COMMENT '端',
    `login_type`    VARCHAR(32)     NOT NULL DEFAULT 'PASSWORD' COMMENT '登录方式：PASSWORD/SMS',
    `ip`            VARCHAR(64)     DEFAULT NULL COMMENT 'IP',
    `user_agent`    VARCHAR(500)    DEFAULT NULL COMMENT 'User-Agent',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    `msg`           VARCHAR(500)    DEFAULT NULL COMMENT '结果说明',
    `login_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_login_time` (`tenant_id`, `login_time`),
    KEY `idx_user_login_time` (`user_code`, `login_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志';

-- -------------------------------------------------------------
-- 11. 操作日志
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_operate_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL DEFAULT '0' COMMENT '租户编码',
    `user_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '用户ID',
    `user_code`     VARCHAR(32)     DEFAULT NULL COMMENT '用户编码',
    `client_code`   VARCHAR(32)     DEFAULT NULL COMMENT '端',
    `module`        VARCHAR(64)     DEFAULT NULL COMMENT '模块',
    `operation`     VARCHAR(128)    DEFAULT NULL COMMENT '操作',
    `method`        VARCHAR(200)    DEFAULT NULL COMMENT '方法',
    `request_uri`   VARCHAR(255)    DEFAULT NULL COMMENT '请求地址',
    `request_param` TEXT            DEFAULT NULL COMMENT '请求参数（脱敏）',
    `response_result` TEXT          DEFAULT NULL COMMENT '响应结果（脱敏）',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    `error_msg`     TEXT            DEFAULT NULL COMMENT '错误信息',
    `ip`            VARCHAR(64)     DEFAULT NULL COMMENT 'IP',
    `user_agent`    VARCHAR(500)    DEFAULT NULL COMMENT 'User-Agent',
    `duration_ms`   BIGINT          DEFAULT NULL COMMENT '耗时毫秒',
    `trace_id`      VARCHAR(64)     DEFAULT NULL COMMENT '链路ID',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_create` (`tenant_id`, `create_time`),
    KEY `idx_user_create` (`user_code`, `create_time`),
    KEY `idx_trace` (`trace_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志';

-- -------------------------------------------------------------
-- 12. 技师
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `technician` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `user_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '关联 sys_user.id',
    `technician_code` VARCHAR(32)   NOT NULL COMMENT '技师编码',
    `name`          VARCHAR(64)     NOT NULL COMMENT '姓名',
    `mobile_cipher` VARCHAR(128)    DEFAULT NULL COMMENT '手机号密文（SM4）',
    `mobile_hash`   VARCHAR(64)     DEFAULT NULL COMMENT '手机号哈希',
    `id_card_cipher` VARCHAR(128)   DEFAULT NULL COMMENT '身份证密文（SM4）',
    `id_card_hash`  VARCHAR(64)     DEFAULT NULL COMMENT '身份证哈希',
    `skill_tags`    VARCHAR(255)    DEFAULT NULL COMMENT '技能标签，逗号分隔',
    `level`         TINYINT         NOT NULL DEFAULT 1 COMMENT '等级',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1可接单 2忙碌 3休息 4停用',
    `current_order_count` INT       NOT NULL DEFAULT 0 COMMENT '当前工单数',
    `max_order_count` INT           NOT NULL DEFAULT 5 COMMENT '最大并行工单数',
    `rating`        DECIMAL(3,2)    NOT NULL DEFAULT 5.00 COMMENT '评分',
    `entry_time`    DATE            DEFAULT NULL COMMENT '入职日期',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_technician_code` (`tenant_id`, `technician_code`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_mobile_hash` (`mobile_hash`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技师';

-- -------------------------------------------------------------
-- 13. 客户
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `customer` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `customer_code` VARCHAR(32)     NOT NULL COMMENT '客户编码',
    `name`          VARCHAR(128)    NOT NULL COMMENT '客户名称',
    `contact_name`  VARCHAR(64)     DEFAULT NULL COMMENT '联系人',
    `contact_mobile_cipher` VARCHAR(128) DEFAULT NULL COMMENT '联系人手机号密文（SM4）',
    `contact_mobile_hash` VARCHAR(64) DEFAULT NULL COMMENT '联系人手机号哈希',
    `address`       VARCHAR(255)    DEFAULT NULL COMMENT '地址',
    `longitude`     DECIMAL(10,6)   DEFAULT NULL COMMENT '经度',
    `latitude`      DECIMAL(10,6)   DEFAULT NULL COMMENT '纬度',
    `customer_level` TINYINT        NOT NULL DEFAULT 1 COMMENT '客户等级',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1正常 0停用',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_customer_code` (`tenant_id`, `customer_code`),
    KEY `idx_tenant_name` (`tenant_id`, `name`),
    KEY `idx_mobile_hash` (`contact_mobile_hash`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户';

-- -------------------------------------------------------------
-- 14. 设备
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `equipment` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `customer_id`   BIGINT UNSIGNED NOT NULL COMMENT '客户ID',
    `equipment_code` VARCHAR(32)    NOT NULL COMMENT '设备编码',
    `equipment_name` VARCHAR(128)   NOT NULL COMMENT '设备名称',
    `model`         VARCHAR(64)     DEFAULT NULL COMMENT '型号',
    `brand`         VARCHAR(64)     DEFAULT NULL COMMENT '品牌',
    `install_date`  DATE            DEFAULT NULL COMMENT '安装日期',
    `warranty_end_date` DATE        DEFAULT NULL COMMENT '保修截止日期',
    `location`      VARCHAR(255)    DEFAULT NULL COMMENT '安装位置',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1正常 2维修中 3报废',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_equipment_code` (`tenant_id`, `equipment_code`),
    KEY `idx_tenant_customer` (`tenant_id`, `customer_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备';

-- -------------------------------------------------------------
-- 15. 工单
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `order_no`      VARCHAR(32)     NOT NULL COMMENT '工单号',
    `customer_id`   BIGINT UNSIGNED NOT NULL COMMENT '客户ID',
    `equipment_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '设备ID',
    `customer_name` VARCHAR(128)    DEFAULT NULL COMMENT '客户名称（冗余）',
    `equipment_name` VARCHAR(128)   DEFAULT NULL COMMENT '设备名称（冗余）',
    `technician_name` VARCHAR(64)   DEFAULT NULL COMMENT '技师姓名（冗余）',
    `title`         VARCHAR(128)    NOT NULL COMMENT '标题',
    `description`   TEXT            DEFAULT NULL COMMENT '问题描述',
    `priority`      TINYINT         NOT NULL DEFAULT 2 COMMENT '1低 2普通 3高 4紧急',
    `source`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1租户创建 2客户报修 3导入',
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `technician_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '技师ID',
    `dispatcher_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '派单人ID',
    `expect_time`   DATETIME        DEFAULT NULL COMMENT '期望上门时间',
    `assign_time`   DATETIME        DEFAULT NULL COMMENT '派单时间',
    `accept_time`   DATETIME        DEFAULT NULL COMMENT '接单时间',
    `start_time`    DATETIME        DEFAULT NULL COMMENT '开始处理时间',
    `finish_time`   DATETIME        DEFAULT NULL COMMENT '完工时间',
    `confirm_time`  DATETIME        DEFAULT NULL COMMENT '确认时间',
    `cancel_time`   DATETIME        DEFAULT NULL COMMENT '取消时间',
    `cancel_reason` VARCHAR(500)    DEFAULT NULL COMMENT '取消原因',
    `finish_remark` VARCHAR(1000)   DEFAULT NULL COMMENT '完工说明',
    `timeout_flag`  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已超时提醒：0否 1是',
    `timeout_notify_time` DATETIME  DEFAULT NULL COMMENT '超时提醒时间',
    `evaluation_score` TINYINT      DEFAULT NULL COMMENT '评分（冗余）',
    `version`       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_order_no` (`tenant_id`, `order_no`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_technician_status` (`tenant_id`, `technician_id`, `status`),
    KEY `idx_tenant_customer` (`tenant_id`, `customer_id`),
    KEY `idx_tenant_expect` (`tenant_id`, `expect_time`, `status`),
    KEY `idx_tenant_create` (`tenant_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单';

-- -------------------------------------------------------------
-- 16. 工单日志/时间线
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    `order_no`      VARCHAR(32)     NOT NULL COMMENT '工单号',
    `action`        VARCHAR(32)     NOT NULL COMMENT '动作：CREATE/ASSIGN/ACCEPT/REJECT/START/COMPLETE/CONFIRM/CANCEL/REOPEN',
    `from_status`   VARCHAR(32)     DEFAULT NULL COMMENT '原状态',
    `to_status`     VARCHAR(32)     DEFAULT NULL COMMENT '新状态',
    `operator_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64)     DEFAULT NULL COMMENT '操作人名称',
    `operator_type` TINYINT         DEFAULT NULL COMMENT '1平台 2租户 3技师 4系统 5客户',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `trace_id`      VARCHAR(64)     DEFAULT NULL COMMENT '链路ID',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`, `id`),
    KEY `idx_tenant_order_no` (`tenant_id`, `order_no`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单日志';

-- -------------------------------------------------------------
-- 17. 工单附件
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_attachment` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    `file_name`     VARCHAR(255)    NOT NULL COMMENT '文件名',
    `file_key`      VARCHAR(500)    NOT NULL COMMENT '对象存储 key',
    `file_type`     TINYINT         NOT NULL DEFAULT 1 COMMENT '1图片 2视频 3PDF 9其他',
    `file_size`     BIGINT          DEFAULT NULL COMMENT '文件大小（字节）',
    `uploader_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '上传人ID',
    `uploader_type` TINYINT         DEFAULT NULL COMMENT '1平台 2租户 3技师 5客户',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单附件';

-- -------------------------------------------------------------
-- 18. 派工记录
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_dispatch` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    `technician_id` BIGINT UNSIGNED NOT NULL COMMENT '技师ID',
    `technician_name` VARCHAR(64)   DEFAULT NULL COMMENT '技师姓名（冗余）',
    `dispatcher_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '派单人ID',
    `dispatch_type` TINYINT         NOT NULL DEFAULT 1 COMMENT '1手动 2自动',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '1待接单 2已接单 3已拒绝 4已取消',
    `assign_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '派单时间',
    `accept_time`   DATETIME        DEFAULT NULL COMMENT '接单时间',
    `reject_reason` VARCHAR(500)    DEFAULT NULL COMMENT '拒绝原因',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`),
    KEY `idx_tenant_technician_status` (`tenant_id`, `technician_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '派工记录';

-- -------------------------------------------------------------
-- 19. 工单评价（二期）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_evaluation` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `order_id`      BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    `score`         TINYINT         NOT NULL COMMENT '评分 1-5',
    `content`       VARCHAR(500)    DEFAULT NULL COMMENT '评价内容',
    `evaluator_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '评价人ID',
    `evaluator_type` TINYINT        DEFAULT NULL COMMENT '2租户 5客户',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    UNIQUE KEY `uk_tenant_order` (`tenant_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单评价';

-- -------------------------------------------------------------
-- 20. 请求幂等
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_command` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `request_id`    VARCHAR(64)     NOT NULL COMMENT '请求幂等ID',
    `order_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '工单ID',
    `command_type`  VARCHAR(32)     NOT NULL COMMENT '命令类型：CREATE/ASSIGN/ACCEPT/COMPLETE...',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0处理中 1成功 2失败',
    `result_json`   JSON            DEFAULT NULL COMMENT '结果快照',
    `error_msg`     VARCHAR(500)    DEFAULT NULL COMMENT '失败原因',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_request` (`tenant_id`, `request_id`),
    KEY `idx_tenant_order` (`tenant_id`, `order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请求幂等记录';

-- -------------------------------------------------------------
-- 21. MQ 消费幂等
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mq_consume_log` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     DEFAULT NULL COMMENT '租户编码',
    `topic`         VARCHAR(128)    NOT NULL COMMENT 'Topic',
    `consumer_group` VARCHAR(128)   NOT NULL COMMENT '消费组',
    `msg_id`        VARCHAR(128)    NOT NULL COMMENT '消息ID',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0处理中 1成功 2失败',
    `retry_count`   INT             NOT NULL DEFAULT 0 COMMENT '重试次数',
    `error_msg`     VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    `consume_time`  DATETIME        DEFAULT NULL COMMENT '消费完成时间',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_group_msg` (`topic`, `consumer_group`, `msg_id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'MQ 消费幂等';

-- -------------------------------------------------------------
-- 22. MQ 失败消息
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `msg_fail` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     DEFAULT NULL COMMENT '租户编码',
    `topic`         VARCHAR(128)    NOT NULL COMMENT 'Topic',
    `tag`           VARCHAR(64)     DEFAULT NULL COMMENT 'Tag',
    `msg_id`        VARCHAR(128)    DEFAULT NULL COMMENT '消息ID',
    `msg_body`      TEXT            NOT NULL COMMENT '消息体',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0待重试 1重试中 2成功 3死信',
    `retry_count`   INT             NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME      DEFAULT NULL COMMENT '下次重试时间',
    `error_msg`     VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`),
    KEY `idx_topic_msg` (`topic`, `msg_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'MQ 失败消息';

-- -------------------------------------------------------------
-- 23. 异步导出任务
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `export_task` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `task_name`     VARCHAR(128)    NOT NULL COMMENT '任务名称',
    `export_type`   VARCHAR(64)     NOT NULL COMMENT '导出类型：WORK_ORDER/TECHNICIAN...',
    `query_param`   JSON            DEFAULT NULL COMMENT '查询参数快照',
    `file_key`      VARCHAR(500)    DEFAULT NULL COMMENT '对象存储 key',
    `file_name`     VARCHAR(255)    DEFAULT NULL COMMENT '文件名',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '0待执行 1执行中 2成功 3失败',
    `total_count`   INT             DEFAULT NULL COMMENT '总条数',
    `processed_count` INT           NOT NULL DEFAULT 0 COMMENT '已处理条数',
    `error_msg`     VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    `start_time`    DATETIME        DEFAULT NULL COMMENT '开始时间',
    `finish_time`   DATETIME        DEFAULT NULL COMMENT '完成时间',
    `create_by`     VARCHAR(64)     DEFAULT NULL COMMENT '创建人',
    `update_by`     VARCHAR(64)     DEFAULT NULL COMMENT '更新人',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_tenant_status_create` (`tenant_id`, `status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异步导出任务';

-- -------------------------------------------------------------
-- 24. 站内信（二期）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_notification` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `uid`           VARCHAR(32)     NOT NULL COMMENT '业务ID',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
    `title`         VARCHAR(128)    NOT NULL COMMENT '标题',
    `content`       VARCHAR(1000)   NOT NULL COMMENT '内容',
    `type`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1系统 2工单 3提醒',
    `biz_type`      VARCHAR(32)     DEFAULT NULL COMMENT '业务类型',
    `biz_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '业务ID',
    `read_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `read_time`     DATETIME        DEFAULT NULL COMMENT '阅读时间',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_delete`     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_uid` (`uid`),
    KEY `idx_user_read_create` (`tenant_id`, `user_id`, `read_status`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '站内信';

-- -------------------------------------------------------------
-- 25. 工单每日统计（三期）
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_daily_stat` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)     NOT NULL COMMENT '租户编码',
    `stat_date`     DATE            NOT NULL COMMENT '统计日期',
    `created_count` INT             NOT NULL DEFAULT 0 COMMENT '创建数',
    `completed_count` INT           NOT NULL DEFAULT 0 COMMENT '完工数',
    `canceled_count` INT            NOT NULL DEFAULT 0 COMMENT '取消数',
    `timeout_count` INT             NOT NULL DEFAULT 0 COMMENT '超时数',
    `avg_finish_minutes` INT        DEFAULT NULL COMMENT '平均完工分钟',
    `avg_score`     DECIMAL(3,2)    DEFAULT NULL COMMENT '平均评分',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工单每日统计';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 初始化建议（后续用代码/脚本生成，不建议手写密码）
-- 1. 插入演示租户：T0001
-- 2. 插入平台管理员、租户管理员、技师账号（BCrypt 密码）
-- 3. 插入菜单与权限：
--    platform:tenant:page/create/update/status
--    tenant:user:page/create/update/reset/status
--    tenant:role:page/create/update/menus
--    tenant:technician:page/create/update/status
--    tenant:customer:page/create/update/detail
--    tenant:equipment:page/create/update/detail
--    tenant:work-order:page/create/detail/update/assign/cancel/confirm/reopen/log/attachment/export/statistics
--    tech:work-order:page/detail/accept/reject/start/complete/attachment/today
-- 4. 插入字典：work_order_status、work_order_priority、work_order_source、technician_status
-- =============================================================
