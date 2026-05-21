package com.saas.framework.common.context;

import java.util.List;

/**
 * 用户上下文 - 使用 ThreadLocal 存储当前登录用户信息
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> PERMISSIONS_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    public static void setPermissions(List<String> permissions) {
        PERMISSIONS_HOLDER.set(permissions);
    }

    public static List<String> getPermissions() {
        return PERMISSIONS_HOLDER.get();
    }

    /**
     * 判断当前用户是否为超级账户（tenant_id == 0）
     */
    public static boolean isSuperAdmin() {
        Long tenantId = getTenantId();
        return tenantId != null && tenantId == 0;
    }

    /**
     * 清除所有 ThreadLocal，防止内存泄漏
     */
    public static void remove() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        TENANT_ID_HOLDER.remove();
        PERMISSIONS_HOLDER.remove();
    }
}
