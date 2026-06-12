package com.medshop.common.security;

import com.medshop.common.UnauthorizedException;
import com.medshop.common.UserContext;
import com.medshop.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器（网关层职责）：校验请求头 {@code Authorization: Bearer <token>}，
 * 解析出用户身份放入 {@link UserContext}。对应架构中"网关层统一鉴权"。
 *
 * <p>登录/注册等公开接口在 {@link WebConfig} 中被排除，不经过本拦截器。
 *
 * <p>同时充当坐席「在线心跳」：每个携带有效 Token 的请求都会刷新该用户的
 * 最近活跃时间，使药师只要在操作（含工作台轮询）即被判定为在线，
 * 关闭页面/超时未活动则自动转为离线，无需可靠的登出信号。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少访问令牌");
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            Integer role = claims.get("role", Integer.class);
            UserContext.set(userId, role);
            // 刷新在线心跳（失败不影响主流程）
            try {
                userMapper.touchActive(userId);
            } catch (Exception ignore) {
                // 心跳更新失败不应阻断业务请求
            }
            return true;
        } catch (UnauthorizedException ue) {
            throw ue;
        } catch (Exception e) {
            throw new UnauthorizedException("令牌无效或已过期");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，防止线程复用导致身份串号
        UserContext.clear();
    }
}
