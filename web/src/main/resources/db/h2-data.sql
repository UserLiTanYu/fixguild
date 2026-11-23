-- H2 本地开发/测试种子数据
-- 统一密码：admin123（BCrypt strength 10）

INSERT INTO sys_tenant (id, uid, tenant_code, tenant_name, contact_name, status, max_user_count, remark, create_by, update_by)
VALUES (1, 'TENANT0001', 'T0001', '演示维保公司', '张三', 1, 50, 'H2 本地演示数据', 'system', 'system'),
       (2, 'TENANT0002', 'T0002', '第二维保公司', '李四', 1, 50, 'H2 租户隔离测试数据', 'system', 'system');

INSERT INTO sys_user (id, uid, tenant_id, client_code, user_code, username, password, nickname, email, status, create_by, update_by)
VALUES (1, 'USER0001', '0', 'platform', 'U0001', 'admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '平台管理员', 'admin@example.com', 1, 'system', 'system'),
       (2, 'USER1001', 'T0001', 'tenant', 'U1001', 'tenant_admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '演示租户管理员', 'tenant1@example.com', 1, 'system', 'system'),
       (3, 'USER2001', 'T0002', 'tenant', 'U2001', 'tenant_admin',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户管理员', 'tenant2@example.com', 1, 'system', 'system'),
       (4, 'USER1002', 'T0001', 'tenant', 'U1002', 'lock_test',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '登录锁定测试账号', NULL, 1, 'system', 'system'),
       (5, 'USER1003', 'T0001', 'tenant', 'U1003', 'tenant_user',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '租户普通用户', NULL, 1, 'system', 'system'),
       (6, 'USER2002', 'T0002', 'tenant', 'U2002', 'tenant_user',
        '$2a$10$BG4QRox2wAnZ0aQ3jmYWQ.eTv5bmnkYjJNjcjiITC1MuSXwQ5jrFm', '第二租户普通用户', NULL, 1, 'system', 'system');
