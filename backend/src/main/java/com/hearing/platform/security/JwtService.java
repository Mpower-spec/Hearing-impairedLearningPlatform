package com.hearing.platform.security;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AppProperties;     // 应用配置（含 JWT 密钥等）

import io.jsonwebtoken.Claims;      // 令牌载荷
import io.jsonwebtoken.Jwts;        // JWT 构建/解析入口
import io.jsonwebtoken.security.Keys; // 密钥工具
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * JWT 令牌服务。
 * 使用 HS256 对称签名负责令牌的签发与解析，密钥与有效期来自 AppProperties。
 */
@Service
public class JwtService {

    // 签名密钥（由配置密钥派生）
    private final SecretKey key;
    // 令牌有效时长（小时）
    private final long expirationHours;

    // 构造器注入配置并初始化密钥
    public JwtService(AppProperties properties) {
        String secret = properties.getJwtSecret();
        // HS256 要求密钥不少于 32 字节，否则启动即失败
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        // 将字符串密钥转为 HMAC-SHA 密钥对象
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = properties.getJwtExpirationHours();
    }

    /**
     * 为已认证用户签发令牌。
     *
     * @param user 当前登录用户
     * @return 签名后的 JWT 字符串
     */
    public String createToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))   // 主题：用户 ID
                .claim("username", user.username())  // 自定义声明：用户名
                .claim("name", user.name())          // 自定义声明：昵称
                .claim("role", user.role())          // 自定义声明：角色
                .issuedAt(Date.from(now))            // 签发时间
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS))) // 过期时间
                .signWith(key)                       // 使用密钥签名
                .compact();                          // 压缩为字符串
    }

    /**
     * 解析并校验令牌，还原登录用户。
     *
     * @param token JWT 字符串
     * @return 令牌对应的认证用户
     */
    public AuthUser parseToken(String token) {
        try {
            // 验签并解析载荷；任何异常（过期/篡改/格式错误）都视为未授权
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            // 从载荷还原用户信息
            return new AuthUser(
                    Long.parseLong(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("name", String.class),
                    claims.get("role", String.class)
            );
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录");
        }
    }
}
