package com.muzhi.minierp.service;

import com.muzhi.minierp.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
