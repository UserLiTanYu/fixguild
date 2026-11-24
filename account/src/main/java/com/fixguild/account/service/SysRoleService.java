package com.fixguild.account.service;

import com.fixguild.account.param.SysRoleCreateParam;
import com.fixguild.account.param.SysRoleMenusParam;
import com.fixguild.account.param.SysRolePageParam;
import com.fixguild.account.param.SysRoleUpdateParam;
import com.fixguild.account.vo.SysRoleVO;
import com.fixguild.common.domain.PageResult;
import com.fixguild.common.exception.BusinessException;

/**
 * 租户端角色管理服务。
 *
 * <p>角色按租户隔离，只管理 client_code=tenant 的角色；系统内置角色只读。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface SysRoleService {

    /**
     * 分页查询当前租户的租户端角色。
     *
     * @param param 分页与筛选参数
     * @return 角色分页结果
     */
    PageResult<SysRoleVO> page(SysRolePageParam param);

    /**
     * 新增自定义角色。
     *
     * @param param 新增参数
     * @return 角色业务 ID
     * @throws BusinessException 角色编码重复时抛出 409
     */
    String create(SysRoleCreateParam param);

    /**
     * 编辑自定义角色。
     *
     * <p>角色编码不可修改；系统内置角色不允许修改。</p>
     *
     * @param param 编辑参数
     * @throws BusinessException 角色不存在抛出 404，系统角色或没有可更新字段抛出 400
     */
    void update(SysRoleUpdateParam param);

    /**
     * 重新分配角色菜单。
     *
     * <p>先删除原有关联，再写入新关联；空列表表示清空菜单。</p>
     *
     * @param param 角色菜单参数
     * @throws BusinessException 角色不存在抛出 404，系统角色或菜单无效抛出 400
     */
    void bindMenus(SysRoleMenusParam param);
}
