package com.example.mall.module.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.mall.common.BizCode;
import com.example.mall.common.BusinessException;
import com.example.mall.auth.JwtProvider;
import com.example.mall.module.user.dto.*;
import com.example.mall.module.user.entity.User;
import com.example.mall.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 用户业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterReq req) {
        // 校验用户名唯一性
        Long usernameCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(BizCode.USER_EXISTS);
        }

        // 校验邮箱唯一性
        Long emailCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getEmail, req.getEmail())
        );
        if (emailCount > 0) {
            throw new BusinessException(BizCode.USER_EXISTS);
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setRole(1);      // 默认普通用户
        user.setStatus(1);    // 默认正常
        user.setLoginAttempts(0);

        userMapper.insert(user);
    }

    /**
     * 用户登录
     */
    @Transactional
    public LoginVO login(LoginReq req) {
        // 查询用户
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, req.getUsername())
        );

        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        // 检查账号是否被锁定
        if (user.getLockUntil() != null && user.getLockUntil().after(new Date())) {
            throw new BusinessException(BizCode.ACCOUNT_DISABLED);
        }

        // 校验用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException(BizCode.ACCOUNT_DISABLED);
        }

        // 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 登录失败，递增失败次数
            int attempts = (user.getLoginAttempts() == null ? 0 : user.getLoginAttempts()) + 1;
            user.setLoginAttempts(attempts);

            if (attempts >= 5) {
                // 超过5次，锁定15分钟
                user.setLockUntil(new Date(System.currentTimeMillis() + 15 * 60 * 1000));
            }

            userMapper.updateById(user);
            throw new BusinessException(BizCode.PASSWORD_ERROR);
        }

        // 登录成功，重置失败次数和锁定时间
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        userMapper.updateById(user);

        // 生成 JWT
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建返回 VO
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(toUserVO(user));

        return loginVO;
    }

    /**
     * 获取用户个人信息
     */
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }
        return toUserVO(user);
    }

    /**
     * 更新用户个人信息
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileReq req) {
        User user = new User();
        user.setId(userId);
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setAvatar(req.getAvatar());
        userMapper.updateById(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordReq req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizCode.USER_NOT_FOUND);
        }

        // 校验旧密码
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(BizCode.OLD_PASSWORD_ERROR);
        }

        // 校验新密码与旧密码不同
        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new BusinessException(BizCode.PASSWORD_SAME);
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
    }

    // ========== 内部辅助方法 ==========

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
