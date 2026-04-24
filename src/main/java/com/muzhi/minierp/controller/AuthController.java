package com.muzhi.minierp.controller;

import com.muzhi.minierp.annotation.OpenApi;
import com.muzhi.minierp.model.JsonResult;
import com.muzhi.minierp.model.LoginQuery;
import com.muzhi.minierp.model.LoginUser;
import com.muzhi.minierp.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authenticate;

    @OpenApi
    @PostMapping("/login")
    public JsonResult<LoginUser> login(@RequestBody LoginQuery query) {
        LoginUser user = authenticate.login(query);
        return JsonResult.success(user);
    }

    @GetMapping("/getCurrentUser")
    public JsonResult<LoginUser> getCurrentUser() {
        LoginUser currentUser = authenticate.getCurrentUser();
        return JsonResult.success(currentUser);
    }
}
