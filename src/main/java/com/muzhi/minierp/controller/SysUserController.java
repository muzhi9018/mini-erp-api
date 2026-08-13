package com.muzhi.minierp.controller;

import com.muzhi.minierp.entity.SysUser;
import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
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
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService sysUserService;

    private final I18nHelper i18nHelper;

    @PostMapping("/create")
    public JsonResult<Boolean> create(@RequestBody SysUser user, HttpServletRequest request) {
        if (user == null || !StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            return JsonResult.error(
                    HttpStatus.BAD_REQUEST_MISSING_PARAM.value(),
                    this.i18nHelper.getMessage("user.username-password-required", "用户名和密码不能为空", request)
            );
        }
        sysUserService.create(user);
        return JsonResult.success();
    }

}
