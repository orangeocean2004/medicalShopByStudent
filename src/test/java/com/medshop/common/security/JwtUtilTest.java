package com.medshop.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试：令牌签发与解析（公共模块 P3 鉴权）。
 * 直接 new 实例（避开 Spring 注入），密钥须 >=32 字节以满足 HS256。
 */
class JwtUtilTest {

    private static final String SECRET = "medshop-test-secret-key-0123456789-abcdef";

    private JwtUtil newUtil(long expireMinutes) {
        return new JwtUtil(SECRET, expireMinutes);
    }

    @Test
    @DisplayName("签发的令牌可解析出原始用户ID与角色")
    void issueThenParse_roundTrip() {
        JwtUtil util = newUtil(60);
        String token = util.issue(42L, 3);

        Claims claims = util.parse(token);
        assertEquals("42", claims.getSubject());
        assertEquals(3, claims.get("role", Integer.class));
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    @DisplayName("已过期的令牌解析时抛异常")
    void parse_expiredToken_throws() {
        JwtUtil util = newUtil(0); // 过期时间 = 签发时刻，立即失效
        String token = util.issue(1L, 0);
        assertThrows(JwtException.class, () -> util.parse(token));
    }

    @Test
    @DisplayName("被篡改/伪造签名的令牌解析时抛异常")
    void parse_tamperedToken_throws() {
        JwtUtil util = newUtil(60);
        String token = util.issue(1L, 0);
        // 改动末位字符破坏签名
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");
        assertThrows(JwtException.class, () -> util.parse(tampered));
    }

    @Test
    @DisplayName("用其它密钥签发的令牌无法被本实例解析")
    void parse_foreignKey_throws() {
        JwtUtil signer = new JwtUtil("another-completely-different-secret-key-xyz-987654321", 60);
        String token = signer.issue(7L, 1);

        JwtUtil verifier = newUtil(60);
        assertThrows(JwtException.class, () -> verifier.parse(token));
    }

    @Test
    @DisplayName("格式非法的字符串解析时抛异常")
    void parse_malformed_throws() {
        JwtUtil util = newUtil(60);
        assertThrows(JwtException.class, () -> util.parse("not-a-jwt-token"));
    }
}
