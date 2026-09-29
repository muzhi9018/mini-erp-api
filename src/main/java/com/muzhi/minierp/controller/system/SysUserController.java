package com.muzhi.minierp.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.entity.system.SysUserRole;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.system.ISysUserService;
import com.muzhi.minierp.util.Assert;
import com.muzhi.minierp.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 系统用户信息 前端控制器
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026-04-23
 */
@Slf4j
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService sysUserService;

    private final I18nHelper i18nHelper;

    @PostMapping("/create")
    public JsonResult<Boolean> create(@RequestBody SysUser user, HttpServletRequest request) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("system.user.username-password-required", "用户名和密码不能为空");
        }
        sysUserService.create(user);
        return JsonResult.success();
    }

    /**
     * 为用户授权角色。
     *
     * @param userRole 用户与角色的关联信息
     * @return 授权结果
     */
    @PostMapping("/authorize/role")
    public JsonResult<Boolean> authorizeRole(@RequestBody SysUserRole userRole) {
        Assert.isNull(userRole, "system.user-role.request-required", "授权信息不能为空");
        Assert.isTrue(userRole.getUserId() == null || userRole.getUserId() <= 0, "system.user-role.user-id-required", "用户ID不能为空且必须大于0");
        Assert.isTrue(userRole.getRoleId() == null || userRole.getRoleId() <= 0, "system.user-role.role-id-required", "角色ID不能为空且必须大于0");
        sysUserService.authorizeRole(userRole.getUserId(), userRole.getRoleId());
        return JsonResult.success(true);
    }

    /**
     * 分页查询用户及其已授权角色。
     *
     * @param pageNum 当前页
     * @param pageSize 每页数量
     * @param query 用户查询条件
     * @return 用户分页数据
     */
    @GetMapping("/findByPage")
    public JsonResult<IPage<SysUserVO>> findByPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum, @RequestParam(name = "pageSize", defaultValue = "15") Integer pageSize, SysUser query) {
        Assert.isTrue(pageNum == null || pageNum <= 0, "pagination.page-num-invalid", "当前页必须大于0");
        Assert.isTrue(pageSize == null || pageSize <= 0, "pagination.page-size-invalid", "每页数量必须大于0");
        IPage<SysUserVO> page = sysUserService.findByPage(pageNum, pageSize, query);
        return JsonResult.success(page);
    }

}
