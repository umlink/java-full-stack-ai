package com.example.mall.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.PageResult;
import com.example.mall.common.Result;
import com.example.mall.module.user.entity.User;
import com.example.mall.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
public class AdminUserController {

    private final UserMapper userMapper;

    @GetMapping
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer role) {
        Page<User> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(User::getUsername, keyword);
        }
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        IPage<User> result = userMapper.selectPage(p, wrapper);
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = new User();
        user.setId(id);
        user.setRole(body.get("role"));
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        User user = new User();
        user.setId(id);
        user.setStatus(Boolean.TRUE.equals(body.get("enabled")) ? 1 : 0);
        userMapper.updateById(user);
        return Result.success();
    }
}
