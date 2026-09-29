package com.muzhi.minierp.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.vo.SysUserVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 系统用户信息 Mapper 接口
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-04-23
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户信息，不读取密码。
     *
     * @param page 分页对象
     * @param query 查询条件
     * @return 用户分页数据
     */
    IPage<SysUserVO> findByPage(IPage<SysUserVO> page, @Param("query") SysUser query);
}
