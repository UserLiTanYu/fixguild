package com.fixguild.common.listener;

import cn.hutool.core.util.StrUtil;
import com.fixguild.common.auth.LoginContext;
import com.fixguild.common.domain.BaseEntity;
import com.mybatisflex.annotation.InsertListener;
import com.mybatisflex.annotation.UpdateListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段填充：createBy/updateBy/createTime/updateTime/isDelete。
 *
 * <p>createBy/updateBy 优先取当前登录用户（LoginContext），未登录场景兜底 system。
 * 插入时只补空值，更新时强制刷新 updateTime、updateBy 为空时兜底，
 * create* 字段保持不变。全局注册见 MyBatisFlexConfig。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Component
public class AuditFieldListener implements InsertListener, UpdateListener {

    /** 未登录场景的兜底操作人。 */
    private static final String SYSTEM_USER = "system";

    /**
     * 插入前填充审计字段。
     *
     * @param entity 待插入实体
     */
    @Override
    public void onInsert(Object entity) {
        if (entity instanceof BaseEntity base) {
            LocalDateTime now = LocalDateTime.now();
            // 只补空值，允许调用方显式指定创建时间/创建人
            if (base.getCreateTime() == null) {
                base.setCreateTime(now);
            }
            if (base.getUpdateTime() == null) {
                base.setUpdateTime(now);
            }
            if (base.getIsDelete() == null) {
                base.setIsDelete(0);
            }
            // 未登录时统一用 system 兜底
            if (StrUtil.isBlank(base.getCreateBy())) {
                base.setCreateBy(currentUser());
            }
            if (StrUtil.isBlank(base.getUpdateBy())) {
                base.setUpdateBy(currentUser());
            }
        }
    }

    /**
     * 更新前刷新修改时间和修改人，创建字段保持不变。
     *
     * @param entity 待更新实体
     */
    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity base) {
            // 更新只刷新 updateTime/updateBy，create* 字段不可变
            base.setUpdateTime(LocalDateTime.now());
            if (StrUtil.isBlank(base.getUpdateBy())) {
                base.setUpdateBy(currentUser());
            }
        }
    }

    private String currentUser() {
        String userCode = LoginContext.getUserCode();
        return StrUtil.isBlank(userCode) ? SYSTEM_USER : userCode;
    }
}
