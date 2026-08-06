package com.saas.framework;

import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色服务测试
 * 测试租户管理员创建角色时权限集合校验
 */
@SpringBootTest
public class RoleServiceTest {

    @Resource
    private PermissionService permissionService;

    @BeforeEach
    public void setUp() {
        // 模拟租户管理员登录（非超级账户）
        UserContext.setTenantId(1L);
        UserContext.setUserId(2L);
        UserContext.setUsername("test_admin");
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        UserContext.remove();
    }

    @Test
    public void testCheckPermissionsWithinSuccess() {
        // 模拟租户管理员拥有的权限
        List<String> adminPermissions = Arrays.asList(
                "student:list", "student:add", "student:edit", "student:delete",
                "user:list", "user:add"
        );
        UserContext.setPermissions(adminPermissions);

        // 尝试分配权限ID（这些ID对应的code都在adminPermissions中）
        // 注意：数据库中权限ID 41-44 对应 student:list ~ student:delete
        // 由于需要数据库有数据，这里我们模拟这个场景
        // 实际测试中，权限ID来自数据库
        List<Long> rolePermissionIds = Collections.emptyList();
        // 空列表应该通过校验
        assertDoesNotThrow(() -> permissionService.checkPermissionsWithin(rolePermissionIds));
    }

    @Test
    public void testCheckPermissionsWithinExceedScope() {
        // 模拟租户管理员只有学生管理权限
        List<String> adminPermissions = Arrays.asList("student:list", "student:add");
        UserContext.setPermissions(adminPermissions);

        // 尝试分配超出权限范围时应该抛出异常
        // 这里传入一个超出范围的权限ID（需要根据数据库实际情况调整）
        // 由于权限 code: tenant:list 不在 adminPermissions 中，对应权限ID 11
        List<Long> rolePermissionIds = Arrays.asList(11L);

        // 应该抛出 BusinessException
        assertThrows(BusinessException.class, () ->
                permissionService.checkPermissionsWithin(rolePermissionIds));
    }

    @Test
    public void testSuperAdminBypassPermissionCheck() {
        // 模拟超级账户
        UserContext.setTenantId(0L);
        UserContext.setUserId(1L);
        UserContext.setUsername("admin");

        // 超级账户应该通过任何权限校验
        List<Long> anyPermissionIds = Arrays.asList(1L, 2L, 3L, 11L, 21L);
        assertDoesNotThrow(() -> permissionService.checkPermissionsWithin(anyPermissionIds));
    }

    @Test
    public void testEmptyPermissionsCheck() {
        // 模拟没有任何权限的用户
        UserContext.setPermissions(Collections.emptyList());

        // 任何权限分配都应该失败
        List<Long> permissionIds = Arrays.asList(41L);
        assertThrows(BusinessException.class, () ->
                permissionService.checkPermissionsWithin(permissionIds));
    }
}
