package com.saas.framework.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.framework.common.Result;
import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.util.JwtUtil;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证过滤器
 * 拦截所有 /api/** 请求，验证 JWT Token 并设置用户上下文
 */
@Slf4j
@Component
public class JwtAuthFilter implements Filter {

    /** 不需要认证的路径 */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/auth/login",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();

        // 放行不需要认证的路径
        if (isExcludePath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        // 从请求头获取 Token
        String token = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            // 如果 Header 中没有，尝试从 URL 参数获取（用于文件下载等场景）
            token = httpRequest.getParameter("token");
            if (!StringUtils.hasText(token)) {
                writeUnauthorized(httpResponse, "未登录或 Token 格式错误");
                return;
            }
        } else {
            token = token.substring(7);
        }

        // 验证 Token
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(httpResponse, "Token 已过期，请重新登录");
            return;
        }

        // 从 Token 中解析用户信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        Long tenantId = jwtUtil.getTenantIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);

        if (userId == null) {
            writeUnauthorized(httpResponse, "Token 解析失败");
            return;
        }

        // 查询用户确认是否存在且状态正常
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            writeUnauthorized(httpResponse, "用户已被禁用或不存在");
            return;
        }

        // 设置租户上下文（用于 MyBatis-Plus 多租户插件）
        TenantContext.setTenantId(tenantId);

        // 设置用户上下文
        UserContext.setUserId(userId);
        UserContext.setUsername(username);
        UserContext.setTenantId(tenantId);

        // 查询用户权限列表并设置
        List<String> permissions = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());
        UserContext.setPermissions(permissions);

        try {
            chain.doFilter(request, response);
        } finally {
            // 请求结束后清除 ThreadLocal，防止内存泄漏
            TenantContext.remove();
            UserContext.remove();
        }
    }

    /**
     * 判断是否为不需要认证的路径
     */
    private boolean isExcludePath(String path) {
        return EXCLUDE_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    /**
     * 返回 401 未登录响应
     */
    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(401, msg);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
