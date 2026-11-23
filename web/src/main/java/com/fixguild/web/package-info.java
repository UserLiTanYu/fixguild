/**
 * Web 启动模块：应用入口、跨模块配置和公共接口。
 *
 * <p>依赖 common/account/core，负责组装 Spring 上下文；业务接口按端划分到
 * /platform、/tenant、/technician 等前缀下。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
package com.fixguild.web;
