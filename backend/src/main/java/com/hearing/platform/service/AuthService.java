package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.dao.UserMapper;
import com.hearing.platform.dto.LoginRequest;
import com.hearing.platform.dto.LoginResponse;
import com.hearing.platform.dto.UserDto;
import com.hearing.platform.entity.AppUser;
import com.hearing.platform.security.AuthUser;
import com.hearing.platform.security.JwtService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        AuthUser authUser = new AuthUser(user.getId(), user.getUsername(), user.getName(), user.getRole());
        return new LoginResponse(jwtService.createToken(authUser), UserDto.from(user));
    }

    public UserDto current(AuthUser user) {
        return UserDto.from(user);
    }
}