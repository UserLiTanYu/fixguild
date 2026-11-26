-- MySQL 容器首次初始化时执行
-- 注意：该脚本只在数据卷为空时执行一次；重置数据请删除 mysql-data 卷
-- 统一密码：admin123（BCrypt strength 10）
-- 生成格式约定：uid 为 32 位小写 hex（同 IdUtil.fastSimpleUUID）；user_code 为 U + 16 位大写 hex（同 AccountCredentialGenerator）

-- 如果数据库名不是 fixguild，请同步修改下面一行
USE `fixguild`;

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
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户普通用户', NULL, 1, 'system', 'system');
