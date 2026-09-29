package com.muzhi.minierp.service.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhi.minierp.vo.SysUserVO;

/**
 * <p>
 * 系统用户信息 服务类
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-04-23
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询
     * @author Mr.Muzhi
     * @since 2026/4/23 16:47
     * @param username 用户名
     * @return 返回结果
     */
    SysUser findByUsername(String username);

    /**
     * 创建用户
     * @author Mr.Muzhi
     * @since 2026/4/23 18:28
     * @param user 用户
     */
    void create(SysUser user);

    /**
     * 为用户授权角色。
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    void authorizeRole(Long userId, Long roleId);

    /**
     * 分页查询用户及其已授权角色。
     *
     * @param pageNum 当前页
     * @param pageSize 每页数量
     * @param query 用户查询条件
     * @return 用户分页数据
     */
    IPage<SysUserVO> findByPage(Integer pageNum, Integer pageSize, SysUser query);
}
