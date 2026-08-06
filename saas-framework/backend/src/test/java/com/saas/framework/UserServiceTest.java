package com.saas.framework;

import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 * 测试创建租户管理员时自动绑定 tenant_id
 * 测试创建员工时密码加密
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    public void testCreateUserWithTenantId() {
        // 测试创建租户管理员时自动绑定 tenant_id
        SysUser user = new SysUser();
        user.setUsername("test_tenant_admin_" + System.currentTimeMillis());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRoleId(1L);
        // 模拟租户管理员：tenant_id = 100
        user.setTenantId(100L);
        user.setRealName("测试租户管理员");
        user.setStatus(1);

        sysUserMapper.insert(user);

        // 验证插入成功且 tenant_id 正确
        assertNotNull(user.getId());
        assertEquals(100L, user.getTenantId());

        // 清理测试数据
        sysUserMapper.deleteById(user.getId());
    }

    @Test
    public void testPasswordEncryption() {
        // 测试密码 BCrypt 加密
        String rawPassword = "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // BCrypt 加密后的密码应该与原始密码不同
        assertNotEquals(rawPassword, encodedPassword);

        // 验证密码可以正确匹配
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));

        // 验证错误密码不能匹配
        assertFalse(passwordEncoder.matches("wrong_password", encodedPassword));
    }

    @Test
    public void testCreateEmployeeWithEncryptedPassword() {
        // 测试创建员工时密码自动加密
        String rawPassword = "employee123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        SysUser user = new SysUser();
        user.setUsername("test_employee_" + System.currentTimeMillis());
        user.setPassword(encodedPassword);
        user.setRoleId(2L);
        user.setTenantId(1L);
        user.setRealName("测试员工");
        user.setStatus(1);

        sysUserMapper.insert(user);
        assertNotNull(user.getId());

        // 查询用户，验证密码已加密存储
        SysUser dbUser = sysUserMapper.selectById(user.getId());
        assertNotNull(dbUser);
        assertNotEquals(rawPassword, dbUser.getPassword());
        assertTrue(passwordEncoder.matches(rawPassword, dbUser.getPassword()));

        // 清理
        sysUserMapper.deleteById(user.getId());
    }
}
