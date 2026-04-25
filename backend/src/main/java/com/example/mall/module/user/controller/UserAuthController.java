package com.example.mall.module.user.controller;

import com.example.mall.common.Result;
import com.example.mall.module.user.dto.LoginReq;
import com.example.mall.module.user.dto.LoginVO;
import com.example.mall.module.user.dto.RegisterReq;
import com.example.mall.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证控制器（无需登录）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        userService.register(req);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        LoginVO loginVO = userService.login(req);
        return Result.success(loginVO);
    }
}
