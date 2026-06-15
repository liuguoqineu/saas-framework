package com.saas.framework.service.impl;

import com.saas.framework.common.dto.LoginRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.common.util.JwtUtil;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysTenant;
import com.saas.framework.entity.SysUser;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysTenantMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.mapper.RpReportMapper;
import com.saas.framework.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 认证服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private SysTenantMapper sysTenantMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RpReportMapper rpReportMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Map<String, Object> login(LoginRequest request) {
        log.info("用户登录: username={}", request.getUsername());

        // 查询用户
        SysUser user = sysUserMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账户已被禁用，请联系管理员");
        }

        // 检查租户状态（非超级账户需验证所属租户是否被禁用）
        if (user.getTenantId() != null && user.getTenantId() != 0) {
            SysTenant tenant = sysTenantMapper.selectById(user.getTenantId());
            if (tenant != null && tenant.getStatus() != null && tenant.getStatus() == 0) {
                throw new BusinessException("所属租户已被禁用，请联系平台管理员");
            }
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

        // 查询用户权限
        List<String> permissions = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());

        // 所有员工默认拥有打卡权限
        ensureCheckInPermissions(permissions);

        // 查询角色名称
        String roleName = "普通用户";
        if (user.getTenantId() != null && user.getTenantId() == 0) {
            roleName = "超级管理员";
        } else if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null && role.getName() != null) {
                roleName = role.getName();
            }
        }

        // 构建返回数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleName", roleName);
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("postType", user.getPostType());
        userInfo.put("permissions", permissions);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);

        // 检查未填报的报表
        List<Map<String, String>> pendingReports = checkPendingReports(user.getId(), user.getPostType());
        if (!pendingReports.isEmpty()) {
            result.put("pendingReports", pendingReports);
            log.info("用户 {} 有 {} 条未填报报表需要提醒", user.getUsername(), pendingReports.size());
        }

        log.info("用户 {} 登录成功, tenantId={}", user.getUsername(), user.getTenantId());
        return result;
    }

    /**
     * 检查用户未填报的报表
     */
    private List<Map<String, String>> checkPendingReports(Long userId, String postType) {
        List<Map<String, String>> pendingList = new ArrayList<>();

        log.info("检查用户未填报报表: userId={}, postType={}", userId, postType);

        // 即使postType为空也检查，因为所有用户都可能需要填报表
        LocalDate today = LocalDate.now();

        // 检查日报
        checkReportForType(userId, "DAILY", today.format(DATE_FMT), "日报", pendingList);

        // 检查周报
        String weeklyPeriod = getWeeklyPeriod(today);
        checkReportForType(userId, "WEEKLY", weeklyPeriod, "周报", pendingList);

        // 检查月报
        checkReportForType(userId, "MONTHLY", today.format(DateTimeFormatter.ofPattern("yyyy-MM")), "月报", pendingList);

        log.info("用户未填报报表检查完成: userId={}, 待填报数量={}", userId, pendingList.size());

        return pendingList;
    }

    /**
     * 检查特定类型的报表是否未填报
     * 规则：只有当用户从未创建过该周期的报表时才提醒
     * 如果用户创建后删除了（主动放弃），则不提醒
     */
    private void checkReportForType(Long userId, String reportType, String period,
                                     String typeName, List<Map<String, String>> pendingList) {
        // 1. 查询已提交的报表（排除草稿）
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RpReport> submittedWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        submittedWrapper.eq(RpReport::getUserId, userId)
                        .eq(RpReport::getReportType, reportType)
                        .eq(RpReport::getReportPeriod, period)
                        .ne(RpReport::getStatus, "DRAFT");
        List<RpReport> submittedList = rpReportMapper.selectList(submittedWrapper);
        boolean hasSubmitted = !submittedList.isEmpty();

        // 2. 查询所有报表（包括已删除的），判断用户是否曾经创建过
        long totalCount = rpReportMapper.countAllIncludingDeleted(userId, reportType, period);

        log.info("检查报表: userId={}, type={}, period={}, 已提交={}, 总数(含删除)={}",
            userId, reportType, period, hasSubmitted, totalCount);

        // 3. 判断是否需要提醒
        // 条件：没有已提交的 且 从未创建过（包括已删除的）→ 才提醒
        if (!hasSubmitted && totalCount == 0) {
            Map<String, String> pending = new HashMap<>();
            pending.put("type", reportType);
            pending.put("typeName", typeName);
            pending.put("period", period);
            pendingList.add(pending);
            log.info("添加到待提醒列表: {} ({})", typeName, period);
        }
    }

    /**
     * 获取当前周期的周报周期字符串
     */
    private String getWeeklyPeriod(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        return String.format("%d-W%02d", monday.getYear(), 
            (monday.getDayOfYear() - 1) / 7 + 1);
    }

    @Override
    public Map<String, Object> getUserInfo() {
        Long userId = com.saas.framework.common.context.UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        List<String> permissions = sysRolePermissionMapper.selectPermissionCodesByRoleId(user.getRoleId());

        // 查询角色名称
        String roleName = "普通用户";
        if (user.getTenantId() != null && user.getTenantId() == 0) {
            roleName = "超级管理员";
        } else if (user.getRoleId() != null) {
            SysRole role = sysRoleMapper.selectById(user.getRoleId());
            if (role != null && role.getName() != null) {
                roleName = role.getName();
            }
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleName", roleName);
        userInfo.put("tenantId", user.getTenantId());
        userInfo.put("postType", user.getPostType());
        userInfo.put("permissions", permissions);

        return userInfo;
    }

    /**
     * 确保权限列表中包含打卡相关权限（所有员工默认拥有）
     */
    private void ensureCheckInPermissions(List<String> permissions) {
        if (permissions == null) {
            return;
        }
        if (!permissions.contains("checkin:add")) {
            permissions.add("checkin:add");
        }
        if (!permissions.contains("checkin:list")) {
            permissions.add("checkin:list");
        }
    }
}
