-- MySQL 容器首次初始化时执行
-- 注意：该脚本只在数据卷为空时执行一次；重置数据请删除 mysql-data 卷
-- 统一密码：admin123（BCrypt strength 10）
-- 生成格式约定：uid 为 32 位小写 hex（同 IdUtil.fastSimpleUUID）；user_code 为 U + 16 位大写 hex（同 AccountCredentialGenerator）
-- 与 H2 种子 web/src/main/resources/db/h2-data.sql 保持一致

-- 如果数据库名不是 fixguild，请同步修改下面一行
USE `fixguild`;
SET NAMES utf8mb4;

INSERT INTO sys_tenant (id, uid, tenant_code, tenant_name, contact_name, status, max_user_count, remark, create_by, update_by)
VALUES (1, '00000000000000000000000000000001', 'T0001', '演示维保公司', '张三', 1, 50, '演示数据', 'system', 'system'),
       (2, '00000000000000000000000000000002', 'T0002', '第二维保公司', '李四', 1, 50, '租户隔离测试数据', 'system', 'system');

INSERT INTO sys_user (id, uid, tenant_id, client_code, user_code, username, password, nickname, email, status, create_by, update_by)
VALUES (1, '00000000000000000000000000000101', '0', 'platform', 'U0000000000000001', 'admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '平台管理员', 'admin@example.com', 1, 'system', 'system'),
       (2, '00000000000000000000000000000102', 'T0001', 'tenant', 'U0000000000000002', 'tenant_admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '演示租户管理员', 'tenant1@example.com', 1, 'system', 'system'),
       (3, '00000000000000000000000000000103', 'T0002', 'tenant', 'U0000000000000003', 'tenant_admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户管理员', 'tenant2@example.com', 1, 'system', 'system'),
       (4, '00000000000000000000000000000104', 'T0001', 'tenant', 'U0000000000000004', 'lock_test',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '登录锁定测试账号', NULL, 1, 'system', 'system'),
       (5, '00000000000000000000000000000105', 'T0001', 'tenant', 'U0000000000000005', 'tenant_user',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '租户普通用户', NULL, 1, 'system', 'system'),
       (6, '00000000000000000000000000000106', 'T0002', 'tenant', 'U0000000000000006', 'tenant_user',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户普通用户', NULL, 1, 'system', 'system'),
       (7, '00000000000000000000000000000107', 'T0001', 'technician', 'U0000000000000007', 'technician',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '演示技师', NULL, 1, 'system', 'system'),
       (8, '00000000000000000000000000000108', 'T0002', 'technician', 'U0000000000000008', 'technician',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户技师', NULL, 1, 'system', 'system');

INSERT INTO sys_menu (id, uid, client_code, parent_id, menu_name, menu_type, path, component, perms, icon, sort, visible, status, is_system, create_by, update_by)
VALUES (1, '00000000000000000000000000000301', 'platform', 0, '租户管理', 'M', '/platform/tenant', NULL, NULL, 'team', 10, 1, 1, 1, 'system', 'system'),
       (2, '00000000000000000000000000000302', 'platform', 1, '租户分页', 'F', NULL, NULL, 'platform:tenant:page', NULL, 1, 1, 1, 1, 'system', 'system'),
       (3, '00000000000000000000000000000303', 'platform', 1, '租户新增', 'F', NULL, NULL, 'platform:tenant:create', NULL, 2, 1, 1, 1, 'system', 'system'),
       (4, '00000000000000000000000000000304', 'platform', 1, '租户编辑', 'F', NULL, NULL, 'platform:tenant:update', NULL, 3, 1, 1, 1, 'system', 'system'),
       (5, '00000000000000000000000000000305', 'platform', 1, '租户启停', 'F', NULL, NULL, 'platform:tenant:status', NULL, 4, 1, 1, 1, 'system', 'system'),
       (6, '00000000000000000000000000000306', 'platform', 1, '租户详情', 'F', NULL, NULL, 'platform:tenant:detail', NULL, 5, 1, 1, 1, 'system', 'system'),
       (10, '0000000000000000000000000000030a', 'tenant', 0, '系统管理', 'M', '/tenant/system', NULL, NULL, 'setting', 90, 1, 1, 1, 'system', 'system'),
       (11, '0000000000000000000000000000030b', 'tenant', 10, '用户管理', 'C', '/tenant/user', NULL, NULL, 'team', 10, 1, 1, 1, 'system', 'system'),
       (12, '0000000000000000000000000000030c', 'tenant', 11, '用户分页', 'F', NULL, NULL, 'tenant:user:page', NULL, 1, 1, 1, 1, 'system', 'system'),
       (13, '0000000000000000000000000000030d', 'tenant', 11, '用户新增', 'F', NULL, NULL, 'tenant:user:create', NULL, 2, 1, 1, 1, 'system', 'system'),
       (14, '0000000000000000000000000000030e', 'tenant', 11, '用户编辑', 'F', NULL, NULL, 'tenant:user:update', NULL, 3, 1, 1, 1, 'system', 'system'),
       (15, '0000000000000000000000000000030f', 'tenant', 11, '重置密码', 'F', NULL, NULL, 'tenant:user:reset', NULL, 4, 1, 1, 1, 'system', 'system'),
       (16, '00000000000000000000000000000310', 'tenant', 11, '用户启停', 'F', NULL, NULL, 'tenant:user:status', NULL, 5, 1, 1, 1, 'system', 'system'),
       (17, '00000000000000000000000000000311', 'tenant', 11, '删除用户', 'F', NULL, NULL, 'tenant:user:delete', NULL, 6, 1, 1, 1, 'system', 'system'),
       (20, '00000000000000000000000000000314', 'tenant', 10, '角色管理', 'C', '/tenant/role', NULL, NULL, 'peoples', 20, 1, 1, 1, 'system', 'system'),
       (21, '00000000000000000000000000000315', 'tenant', 20, '角色分页', 'F', NULL, NULL, 'tenant:role:page', NULL, 1, 1, 1, 1, 'system', 'system'),
       (22, '00000000000000000000000000000316', 'tenant', 20, '角色新增', 'F', NULL, NULL, 'tenant:role:create', NULL, 2, 1, 1, 1, 'system', 'system'),
       (23, '00000000000000000000000000000317', 'tenant', 20, '角色编辑', 'F', NULL, NULL, 'tenant:role:update', NULL, 3, 1, 1, 1, 'system', 'system'),
       (24, '00000000000000000000000000000318', 'tenant', 20, '分配菜单', 'F', NULL, NULL, 'tenant:role:menus', NULL, 4, 1, 1, 1, 'system', 'system'),
       (25, '00000000000000000000000000000319', 'tenant', 20, '删除角色', 'F', NULL, NULL, 'tenant:role:delete', NULL, 5, 1, 1, 1, 'system', 'system'),
       (30, '0000000000000000000000000000031e', 'tenant', 10, '菜单管理', 'C', '/tenant/menu', NULL, NULL, 'tree', 30, 1, 1, 1, 'system', 'system'),
       (31, '0000000000000000000000000000031f', 'tenant', 30, '菜单树', 'F', NULL, NULL, 'tenant:menu:tree', NULL, 1, 1, 1, 1, 'system', 'system'),
       (40, '00000000000000000000000000000328', 'technician', 0, '我的工单', 'C', '/technician/work-order', NULL, NULL, 'tool', 10, 1, 1, 1, 'system', 'system'),
       (41, '00000000000000000000000000000329', 'technician', 40, '工单分页', 'F', NULL, NULL, 'technician:work-order:page', NULL, 1, 1, 1, 1, 'system', 'system'),
       (42, '0000000000000000000000000000032a', 'technician', 40, '接单', 'F', NULL, NULL, 'technician:work-order:accept', NULL, 2, 1, 1, 1, 'system', 'system'),
       (43, '0000000000000000000000000000032b', 'technician', 40, '完工', 'F', NULL, NULL, 'technician:work-order:complete', NULL, 3, 1, 1, 1, 'system', 'system');

INSERT INTO sys_role (id, uid, tenant_id, client_code, role_code, role_name, data_scope, status, is_system, remark, create_by, update_by)
VALUES (1, '00000000000000000000000000000201', '0', 'platform', 'platform_admin', '平台管理员', 1, 1, 1, '平台内置角色', 'system', 'system'),
       (2, '00000000000000000000000000000202', 'T0001', 'tenant', 'tenant_admin', '租户管理员', 2, 1, 1, '租户内置角色', 'system', 'system'),
       (3, '00000000000000000000000000000203', 'T0001', 'tenant', 'tenant_user', '租户普通用户', 2, 1, 1, '租户内置角色', 'system', 'system'),
       (4, '00000000000000000000000000000204', 'T0002', 'tenant', 'tenant_admin', '租户管理员', 2, 1, 1, '租户内置角色', 'system', 'system'),
       (5, '00000000000000000000000000000205', 'T0002', 'tenant', 'tenant_user', '租户普通用户', 2, 1, 1, '租户内置角色', 'system', 'system'),
       (6, '00000000000000000000000000000206', 'T0001', 'technician', 'technician', '技师', 3, 1, 1, '技师内置角色', 'system', 'system'),
       (7, '00000000000000000000000000000207', 'T0002', 'technician', 'technician', '技师', 3, 1, 1, '技师内置角色', 'system', 'system');

INSERT INTO sys_user_role (tenant_id, user_id, role_id, create_by)
VALUES ('0', 1, 1, 'system'),
       ('T0001', 2, 2, 'system'),
       ('T0002', 3, 4, 'system'),
       ('T0001', 4, 3, 'system'),
       ('T0001', 5, 3, 'system'),
       ('T0002', 6, 5, 'system'),
       ('T0001', 7, 6, 'system'),
       ('T0002', 8, 7, 'system');

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id, create_by)
VALUES ('0', 1, 1, 'system'),
       ('0', 1, 2, 'system'),
       ('0', 1, 3, 'system'),
       ('0', 1, 4, 'system'),
       ('0', 1, 5, 'system'),
       ('0', 1, 6, 'system'),
       ('T0001', 2, 10, 'system'),
       ('T0001', 2, 11, 'system'),
       ('T0001', 2, 12, 'system'),
       ('T0001', 2, 13, 'system'),
       ('T0001', 2, 14, 'system'),
       ('T0001', 2, 15, 'system'),
       ('T0001', 2, 16, 'system'),
       ('T0001', 2, 17, 'system'),
       ('T0001', 2, 20, 'system'),
       ('T0001', 2, 21, 'system'),
       ('T0001', 2, 22, 'system'),
       ('T0001', 2, 23, 'system'),
       ('T0001', 2, 24, 'system'),
       ('T0001', 2, 25, 'system'),
       ('T0001', 2, 30, 'system'),
       ('T0001', 2, 31, 'system'),
       ('T0002', 4, 10, 'system'),
       ('T0002', 4, 11, 'system'),
       ('T0002', 4, 12, 'system'),
       ('T0002', 4, 13, 'system'),
       ('T0002', 4, 14, 'system'),
       ('T0002', 4, 15, 'system'),
       ('T0002', 4, 16, 'system'),
       ('T0002', 4, 17, 'system'),
       ('T0002', 4, 20, 'system'),
       ('T0002', 4, 21, 'system'),
       ('T0002', 4, 22, 'system'),
       ('T0002', 4, 23, 'system'),
       ('T0002', 4, 24, 'system'),
       ('T0002', 4, 25, 'system'),
       ('T0002', 4, 30, 'system'),
       ('T0002', 4, 31, 'system'),
       ('T0001', 3, 10, 'system'),
       ('T0001', 3, 11, 'system'),
       ('T0001', 3, 12, 'system'),
       ('T0002', 5, 10, 'system'),
       ('T0002', 5, 11, 'system'),
       ('T0002', 5, 12, 'system'),
       ('T0001', 6, 40, 'system'),
       ('T0001', 6, 41, 'system'),
       ('T0001', 6, 42, 'system'),
       ('T0001', 6, 43, 'system'),
       ('T0002', 7, 40, 'system'),
       ('T0002', 7, 41, 'system'),
       ('T0002', 7, 42, 'system'),
       ('T0002', 7, 43, 'system');
