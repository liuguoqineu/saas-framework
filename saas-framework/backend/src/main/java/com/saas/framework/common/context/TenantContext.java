package com.saas.framework.common.context;

/**
 * 租户上下文 - 使用 ThreadLocal 存储当前请求的租户ID
 * 用于 MyBatis-Plus 多租户插件自动填充 tenant_id
 * <p>
 * 使用方式：
 * - 请求进来时由拦截器或过滤器设置 tenantId
 * - 多租户插件读取此值进行 SQL 自动拼接
 * - 请求结束后必须调用 remove() 防止内存泄漏
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的租户ID
     */
    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    /**
     * 获取当前线程的租户ID，未设置时返回 null
     */
    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    /**
     * 清除当前线程的租户ID，防止内存泄漏
     */
    public static void remove() {
        TENANT_HOLDER.remove();
    }
}
