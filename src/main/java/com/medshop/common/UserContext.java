package com.medshop.common;

/**
 * 当前登录用户上下文。
 *
 * <p>{@link com.medshop.common.security.AuthInterceptor} 在网关层校验 Token 后，
 * 把当前用户 ID 与角色放入 ThreadLocal，Controller/Service 通过本类读取，
 * 避免在每个方法签名里层层传递 userId。请求结束后由拦截器清理。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, Integer role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    /** 取当前登录用户ID；未登录抛 {@link UnauthorizedException}。 */
    public static Long requireUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return id;
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static Integer getRole() {
        return ROLE.get();
    }

    /**
     * 校验当前用户具备指定角色之一，否则抛 {@link ForbiddenException}。
     * 角色编码：0消费者 1执业药师 2客服 3运营。
     */
    public static void requireRole(Integer... allowed) {
        requireUserId(); // 先确保已登录
        Integer role = ROLE.get();
        for (Integer a : allowed) {
            if (a.equals(role)) {
                return;
            }
        }
        throw new ForbiddenException("无权访问：当前角色不具备该操作权限");
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
