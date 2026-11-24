-- H2 本地开发/测试用最小 schema
-- 当前包含 M1 租户表 + M2 登录/RBAC 表；完整 MySQL 表结构见 docs/sql/schema.sql

DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_login_log;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_tenant;

CREATE TABLE sys_tenant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(32) NOT NULL,
    tenant_code VARCHAR(32) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    contact_name VARCHAR(64),
    contact_mobile VARCHAR(128),
    status TINYINT NOT NULL DEFAULT 1,
    expire_time TIMESTAMP,
    max_user_count INT NOT NULL DEFAULT 50,
    remark VARCHAR(500),
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_tenant_uid ON sys_tenant (uid);
CREATE UNIQUE INDEX uk_sys_tenant_code ON sys_tenant (tenant_code);

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(32) NOT NULL DEFAULT '0',
    client_code VARCHAR(32) NOT NULL,
    user_code VARCHAR(32) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    mobile_cipher VARCHAR(128),
    mobile_hash VARCHAR(64),
    email VARCHAR(128),
    avatar VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 1,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(64),
    remark VARCHAR(500),
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_uid ON sys_user (uid);
CREATE UNIQUE INDEX uk_sys_user_code ON sys_user (user_code);
CREATE UNIQUE INDEX uk_sys_user_tenant_client_username ON sys_user (tenant_id, client_code, username);
CREATE INDEX idx_sys_user_tenant_client_status ON sys_user (tenant_id, client_code, status);

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(32) NOT NULL,
    tenant_id VARCHAR(32) NOT NULL DEFAULT '0',
    client_code VARCHAR(32) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    data_scope TINYINT NOT NULL DEFAULT 2,
    status TINYINT NOT NULL DEFAULT 1,
    is_system TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_role_uid ON sys_role (uid);
CREATE UNIQUE INDEX uk_sys_role_tenant_client_code ON sys_role (tenant_id, client_code, role_code);
CREATE INDEX idx_sys_role_tenant_client_status ON sys_role (tenant_id, client_code, status);

CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(32) NOT NULL,
    client_code VARCHAR(32) NOT NULL,
    parent_id BIGINT NOT NULL DEFAULT 0,
    menu_name VARCHAR(64) NOT NULL,
    menu_type CHAR(1) NOT NULL,
    path VARCHAR(200),
    component VARCHAR(200),
    perms VARCHAR(128),
    icon VARCHAR(64),
    sort INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    is_system TINYINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_by VARCHAR(64),
    update_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_delete TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_menu_uid ON sys_menu (uid);
CREATE UNIQUE INDEX uk_sys_menu_client_perms ON sys_menu (client_code, perms);
CREATE INDEX idx_sys_menu_client_parent_sort ON sys_menu (client_code, parent_id, sort);

CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (tenant_id, user_id, role_id);
CREATE INDEX idx_sys_user_role_role ON sys_user_role (tenant_id, role_id);

CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_by VARCHAR(64),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (tenant_id, role_id, menu_id);
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);

CREATE TABLE sys_login_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(32) NOT NULL DEFAULT '0',
    user_id BIGINT,
    user_code VARCHAR(32),
    client_code VARCHAR(32) NOT NULL,
    login_type VARCHAR(32) NOT NULL DEFAULT 'PASSWORD',
    ip VARCHAR(64),
    user_agent VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    msg VARCHAR(500),
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_login_log_tenant_time ON sys_login_log (tenant_id, login_time);
CREATE INDEX idx_sys_login_log_user_time ON sys_login_log (user_code, login_time);
