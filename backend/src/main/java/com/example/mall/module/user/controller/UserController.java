package com.example.mall.module.user.controller;

import com.example.mall.common.Result;
import com.example.mall.module.user.dto.ChangePasswordReq;
import com.example.mall.module.user.dto.UpdateProfileReq;
import com.example.mall.module.user.dto.UserVO;
import com.example.mall.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息控制器（需登录认证）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户个人信息
     */
    @GetMapping("/profile")
    public Result<UserVO> getProfile() {
        Long userId = getCurrentUserId();
        UserVO userVO = userService.getProfile(userId);
        return Result.success(userVO);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileReq req) {
        Long userId = getCurrentUserId();
        userService.updateProfile(userId, req);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, req);
        return Result.success();
    }

    /**
     * 从 SecurityContextHolder 获取当前登录用户 ID
     * <p>
     * principal 由 JwtAuthFilter 注入，类型为 Long（用户ID）
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
