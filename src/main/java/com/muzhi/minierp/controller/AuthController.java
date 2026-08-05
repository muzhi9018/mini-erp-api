package com.muzhi.minierp.controller;

import com.muzhi.minierp.annotation.OpenApi;
import com.muzhi.minierp.enums.HttpStatus;
import com.muzhi.minierp.i18n.I18nHelper;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.service.IAuthService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authenticate;
    private final I18nHelper i18nHelper;

    @OpenApi
    @PostMapping("/login")
    public JsonResult<LoginUser> login(@RequestBody LoginQuery query, HttpServletRequest request) {
        LoginUser user = authenticate.login(query);
        return JsonResult.success(user, this.i18nHelper.getMessage(HttpStatus.OK, request));
    }

    @GetMapping("/getCurrentUser")
    public JsonResult<LoginUser> getCurrentUser(HttpServletRequest request) {
        LoginUser currentUser = authenticate.getCurrentUser();
        return JsonResult.success(currentUser, this.i18nHelper.getMessage(HttpStatus.OK, request));
    }
}
