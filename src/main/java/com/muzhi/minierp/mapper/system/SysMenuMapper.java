package com.muzhi.minierp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysMenu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 系统菜单 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 分页查询顶级菜单
     * @author Mr.Muzhi
     * @since 2023/12/12 12:04
     * @param page 分页对象
     * @return 返回结果
     */
    IPage<SysMenu> findTopLevelMenu(IPage<SysMenu> page);

    /**
     * 根据父级id分页查询
     * @author Mr.Muzhi
     * @since 2023/12/12 12:08
     * @param parentId 父级id
     * @return 返回结果
     */
    List<SysMenu> findByParentId(@Param("parentId") Long parentId);

    /**
     * 根据所属客户端 和 父级id 查询
     * @author Mr.Muzhi
     * @since 2023/12/12 12:58
     * @param clientId 所属客户端
     * @param parentId 父级id
     * @return 返回结果
     */
    SysMenu findByClientIdAndParentId(@Param("clientId") String clientId, @Param("parentId") Long parentId);

    /**
     * 根据 id 删除(这里使用物理删除)
     * @author Mr.Muzhi
     * @since 2023/12/13 09:18
     * @param id id
     * @return 返回结果
     */
    int delById(@Param("id") Long id);

    /**
     * 根据id列表查询
     * @author Mr.Muzhi
     * @since 2023/12/21 16:30
     * @param ids id列表
     * @return 返回结果
     */
    List<SysMenu> findByIds(@Param("ids") List<Long> ids);

}
