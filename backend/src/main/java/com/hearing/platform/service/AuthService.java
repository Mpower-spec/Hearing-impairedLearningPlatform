package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dao.UserMapper;           // 用户数据访问
import com.hearing.platform.dto.LoginRequest;         // 登录请求参数
import com.hearing.platform.dto.LoginResponse;        // 登录返回体
import com.hearing.platform.dto.UserDto;              // 用户展示对象
import com.hearing.platform.entity.AppUser;           // 用户实体
import com.hearing.platform.security.AuthUser;        // 已认证用户
import com.hearing.platform.security.JwtService;      // JWT 服务

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 类型安全条件构造
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;      // 密码编码器
import org.springframework.stereotype.Service;

/**
 * 认证业务服务。
 * 承载登录凭据校验与令牌签发逻辑（从控制器下沉到服务层）。
 */
@Service
public class AuthService {

    // 用户数据访问
    private final UserMapper userMapper;
    // 密码编码器（BCrypt），用于校验密码
    private final PasswordEncoder passwordEncoder;
    // JWT 服务，用于签发令牌
    private final JwtService jwtService;

    // 构造器注入依赖
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * 登录：按用户名查库并比对密码，成功后签发令牌。
     *
     * @param request 登录请求（用户名、密码）
     * @return 令牌与用户信息
     */
    public LoginResponse login(LoginRequest request) {
        // 根据用户名查询用户
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, request.username()));
        // 用户不存在或密码不匹配，统一返回"用户名或密码错误"（不暴露具体原因）
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        // 构造认证用户并签发 JWT，同时返回用户资料
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), user.getName(), user.getRole());
        return new LoginResponse(jwtService.createToken(authUser), UserDto.from(user));
    }

    /**
     * 将当前认证用户转换为展示对象。
     *
     * @param user 当前登录用户
     * @return 用户信息
     */
    public UserDto current(AuthUser user) {
        return UserDto.from(user);
    }
}
