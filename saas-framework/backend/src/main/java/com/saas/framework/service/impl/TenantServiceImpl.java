package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.TenantCreateRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysTenant;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysTenantMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 租户服务实现
 * 仅超级账户可操作
 */
@Slf4j
@Service
public class TenantServiceImpl implements TenantService {

    @Resource
    private SysTenantMapper sysTenantMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public IPage<SysTenant> page(int page, int size) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级账户可查看租户列表");
        }

        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysTenant::getCreateTime);

        return sysTenantMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> create(TenantCreateRequest request) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级账户可创建租户");
        }

        log.info("创建租户: name={}, code={}", request.getName(), request.getCode());

        // 检查租户编码是否已存在
        LambdaQueryWrapper<SysTenant> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysTenant::getCode, request.getCode());
        if (sysTenantMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("租户编码已存在");
        }

        // 1. 保存租户信息
        SysTenant tenant = new SysTenant();
        tenant.setName(request.getName());
        tenant.setCode(request.getCode());
        tenant.setStatus(1);
        sysTenantMapper.insert(tenant);

        // 2. 确定管理员用户名和密码
        String adminUsername = request.getAdminUsername();
        if (adminUsername == null || adminUsername.trim().isEmpty()) {
            adminUsername = request.getCode() + "admin";
        }

        String adminPassword = request.getAdminPassword();
        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            adminPassword = generateRandomPassword(6);
        }

        // 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(adminUsername);
        if (existUser != null) {
            throw new BusinessException("管理员用户名 " + adminUsername + " 已存在");
        }

        // 3. 为租户创建默认角色
        SysRole defaultRole = new SysRole();
        defaultRole.setName("租户管理员");
        defaultRole.setTenantId(tenant.getId());
        sysRoleMapper.insert(defaultRole);

        // 4. 创建租户管理员用户
        SysUser adminUser = new SysUser();
        adminUser.setUsername(adminUsername);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setRoleId(defaultRole.getId());
        adminUser.setTenantId(tenant.getId());
        adminUser.setRealName(request.getName() + "管理员");
        adminUser.setStatus(1);
        sysUserMapper.insert(adminUser);

        // 5. 更新租户的管理员信息
        tenant.setAdminUserId(adminUser.getId());
        tenant.setAdminPassword(adminPassword);
        sysTenantMapper.updateById(tenant);

        log.info("租户创建成功: tenantId={}, adminUsername={}", tenant.getId(), adminUsername);

        Map<String, String> result = new HashMap<>();
        result.put("tenantId", String.valueOf(tenant.getId()));
        result.put("adminUsername", adminUsername);
        result.put("adminPassword", adminPassword);
        return result;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级账户可修改租户状态");
        }

        SysTenant tenant = sysTenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(404, "租户不存在");
        }

        tenant.setStatus(status);
        sysTenantMapper.updateById(tenant);

        log.info("租户状态更新: tenantId={}, status={}", id, status);
    }

    /**
     * 生成随机密码
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
