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
     * 重置指定用户的密码。
     *
     * @param userId 用户 ID
     * @return 系统生成的八位数字密码
     */
    String resetPassword(Long userId);

    /**
     * 删除用户及其授权角色。
     *
     * @param userId 用户 ID
     */
    void delete(Long userId);

    /**
     * 切换用户的启用状态。
     *
     * @param userId 用户 ID
     * @return 切换后的用户状态
     */
    Integer changeStatus(Long userId);

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
