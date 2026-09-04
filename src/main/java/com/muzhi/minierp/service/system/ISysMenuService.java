package com.muzhi.minierp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 系统菜单 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface ISysMenuService extends IService<SysMenu> {

    /**
     * 分页查询顶级目录
     * @author Mr.Muzhi
     * @since 2023/12/12 11:54
     * @param pageNum 当前页
     * @param pageSize 一页数量
     * @return 返回结果
     */
    IPage<SysMenu> findTopLevelMenu(Integer pageNum, Integer pageSize);

    /**
     * 根据父级id分页查询
     * @author Mr.Muzhi
     * @since 2023/12/12 12:06
     * @param parentId 父级id
     * @return 返回结果
     */
    List<SysMenu> findByParentId(Long parentId);

    /**
     * 添加菜单
     * @author Mr.Muzhi
     * @since 2023/12/12 12:44
     * @param sysMenu 添加对象
     */
    void addMenu(SysMenu sysMenu);

    /**
     * 更新菜单
     * @author Mr.Muzhi
     * @since 2023/12/12 13:35
     * @param sysMenu 更新对象
     */
    void updateMenu(SysMenu sysMenu);

    /**
     * 根据 id 删除
     * @author Mr.Muzhi
     * @since 2023/12/13 09:12
     * @param id id
     */
    void delById(Long id);

    /**
     * 根据 ids 查询
     * @author Mr.Muzhi
     * @since 2023/12/21 16:29
     * @param ids ids
     * @return 返回结果
     */
    List<SysMenu> findByIds(List<Long> ids);

}
