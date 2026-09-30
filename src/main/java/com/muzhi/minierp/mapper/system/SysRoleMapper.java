package com.muzhi.minierp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysRole;
import com.muzhi.minierp.vo.SysRoleVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统角色信息 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-08-13
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 缺失时插入系统默认角色。
     *
     * @param role 默认角色
     * @return 插入行数
     */
    int insertDefaultRoleIfAbsent(@Param("role") SysRole role);

    /**
     * 按固定 ID 或角色编码查询，包括逻辑删除记录。
     *
     * @param id 固定 ID
     * @param roleCode 角色编码
     * @return 冲突或已存在的角色
     */
    List<SysRole> findByIdOrCodeIncludingDeleted(@Param("id") Long id, @Param("roleCode") String roleCode);

    /**
     * 分页查询
     * @author Mr.Muzhi
     * @since 2023/12/19 10:49
     * @param page 分页对象
     * @param query 查询条件
     * @return 返回结果
     */
    IPage<SysRoleVO> findByPage(IPage<SysRoleVO> page, @Param("query") SysRoleVO query);

    /**
     * 根据角色编码列表获取
     * @author Mr.Muzhi
     * @since 2024/4/15 09:23
     * @param roleCodes 角色编码列表
     * @return 返回结果
     */
    List<SysRole> findByCodes(@Param("roleCodes") Set<String> roleCodes);

}
