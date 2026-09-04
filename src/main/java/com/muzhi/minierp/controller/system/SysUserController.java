package com.muzhi.minierp.controller.system;

import com.muzhi.minierp.entity.system.SysUser;
import com.muzhi.minierp.exception.BusinessException;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.system.ISysUserService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
            throw new BusinessException("user.username-password-required", "用户名和密码不能为空");
        }
        sysUserService.create(user);
        return JsonResult.success();
    }

}
