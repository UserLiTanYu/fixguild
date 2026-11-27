/**
 * 账号与租户模块：租户、用户、角色、菜单、登录。
 *
 * <p>租户表 sys_tenant 为平台全局表；用户/角色/菜单等业务表按 tenant_id 隔离。
 * M1 提供租户 CRUD，M2 已接入 Sa-Token 登录与 RBAC。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
package com.fixguild.account;
