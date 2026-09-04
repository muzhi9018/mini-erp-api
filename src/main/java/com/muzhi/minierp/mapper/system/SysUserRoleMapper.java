package com.muzhi.minierp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhi.minierp.entity.system.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户角色关联表 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {


    /**
     * 根据用户id 查询所绑定的角色编码
     * @author Mr.Muzhi
     * @since 2023/8/25 14:18
     * @param userId 用户id
     * @return 角色编码列表
     */
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色id统计数量
     * @author Mr.Muzhi
     * @since 2023/12/19 17:07
     * @param roleId 角色id
     * @return 返回结果
     */
    Long countByRoleId(@Param("roleId") Long roleId);

}
