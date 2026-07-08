package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.EmployeeRequestBatchReviewDTO;
import com.saas.framework.common.dto.EmployeeRequestCreateDTO;
import com.saas.framework.common.dto.EmployeeRequestReviewDTO;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysEmployeeRequest;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysEmployeeRequestMapper;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.EmployeeRequestService;
import com.saas.framework.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工申请服务实现
 */
@Slf4j
@Service
public class EmployeeRequestServiceImpl implements EmployeeRequestService {

    @Resource
    private SysEmployeeRequestMapper sysEmployeeRequestMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private PermissionService permissionService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(EmployeeRequestCreateDTO request) {
        if (UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "超级管理员请直接创建员工，无需提交申请");
        }

        log.info("租户管理员提交员工申请: username={}, realName={}", request.getUsername(), request.getRealName());

        // 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名 " + request.getUsername() + " 已存在");
        }

        // 检查是否有重复待审核的申请
        LambdaQueryWrapper<SysEmployeeRequest> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(SysEmployeeRequest::getUsername, request.getUsername());
        checkWrapper.eq(SysEmployeeRequest::getStatus, "PENDING");
        if (sysEmployeeRequestMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("该用户名已有待审核的申请");
        }

        // 校验角色权限范围
        Long roleId = request.getRoleId();
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 创建申请记录
        SysEmployeeRequest employeeRequest = new SysEmployeeRequest();
        employeeRequest.setUsername(request.getUsername());
        employeeRequest.setRealName(request.getRealName());
        employeeRequest.setRoleId(roleId);
        employeeRequest.setPostType(request.getPostType());
        employeeRequest.setPassword(request.getPassword());
        employeeRequest.setZhizhiContent(request.getZhizhiContent());
        employeeRequest.setZhizhiImageUrl(request.getZhizhiImageUrl());
        employeeRequest.setTenantId(UserContext.getTenantId());
        employeeRequest.setApplicantId(UserContext.getUserId());
        // 查询申请人真实姓名
        SysUser applicant = sysUserMapper.selectById(UserContext.getUserId());
        employeeRequest.setApplicantName(applicant != null ? applicant.getRealName() : UserContext.getUsername());
        employeeRequest.setStatus("PENDING");
        sysEmployeeRequestMapper.insert(employeeRequest);

        log.info("员工申请提交成功: requestId={}, username={}", employeeRequest.getId(), request.getUsername());
    }

    @Override
    public IPage<SysEmployeeRequest> page(int page, int size, String status) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可查看所有员工申请");
        }

        LambdaQueryWrapper<SysEmployeeRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysEmployeeRequest::getStatus, status);
        }
        wrapper.orderByDesc(SysEmployeeRequest::getCreateTime);
        return sysEmployeeRequestMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<SysEmployeeRequest> myRequests(int page, int size, String status) {
        if (UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "超级管理员请使用审核列表查看");
        }

        Long tenantId = UserContext.getTenantId();
        LambdaQueryWrapper<SysEmployeeRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployeeRequest::getTenantId, tenantId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysEmployeeRequest::getStatus, status);
        }
        wrapper.orderByDesc(SysEmployeeRequest::getCreateTime);
        return sysEmployeeRequestMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public SysEmployeeRequest getById(Long id) {
        SysEmployeeRequest request = sysEmployeeRequestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(404, "申请不存在");
        }
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, EmployeeRequestReviewDTO request) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可审核员工申请");
        }

        String action = request.getAction();
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            throw new BusinessException("审核动作只能为 APPROVED 或 REJECTED");
        }

        SysEmployeeRequest employeeRequest = sysEmployeeRequestMapper.selectById(id);
        if (employeeRequest == null) {
            throw new BusinessException(404, "申请不存在");
        }

        if (!"PENDING".equals(employeeRequest.getStatus())) {
            throw new BusinessException("该申请已审核，无法重复审核");
        }

        // 更新申请状态
        employeeRequest.setStatus(action);
        employeeRequest.setReviewerId(UserContext.getUserId());
        // 查询审核人真实姓名
        SysUser reviewer = sysUserMapper.selectById(UserContext.getUserId());
        employeeRequest.setReviewerName(reviewer != null ? reviewer.getRealName() : UserContext.getUsername());
        employeeRequest.setReviewComment(request.getReviewComment());
        employeeRequest.setReviewTime(LocalDateTime.now());
        sysEmployeeRequestMapper.updateById(employeeRequest);

        // 如果审核通过，创建员工账号
        if ("APPROVED".equals(action)) {
            // 再次检查用户名是否已被占用
            SysUser existUser = sysUserMapper.selectByUsername(employeeRequest.getUsername());
            if (existUser != null) {
                throw new BusinessException("用户名 " + employeeRequest.getUsername() + " 已存在，审核通过但创建失败，请手动创建");
            }

            SysUser user = new SysUser();
            user.setUsername(employeeRequest.getUsername());
            String password = StringUtils.hasText(employeeRequest.getPassword()) ? employeeRequest.getPassword() : "123456";
            user.setPassword(passwordEncoder.encode(password));
            user.setRoleId(employeeRequest.getRoleId());
            user.setTenantId(employeeRequest.getTenantId());
            user.setRealName(employeeRequest.getRealName());
            user.setPostType(employeeRequest.getPostType());
            user.setZhizhiContent(employeeRequest.getZhizhiContent());
            user.setZhizhiImageUrl(employeeRequest.getZhizhiImageUrl());
            user.setStatus(1);
            sysUserMapper.insert(user);

            log.info("员工申请审核通过，员工创建成功: userId={}, username={}", user.getId(), user.getUsername());
        }

        log.info("员工申请审核完成: requestId={}, action={}", id, action);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReview(EmployeeRequestBatchReviewDTO request) {
        if (!UserContext.isSuperAdmin()) {
            throw new BusinessException(403, "仅超级管理员可审核员工申请");
        }

        String action = request.getAction();
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            throw new BusinessException("审核动作只能为 APPROVED 或 REJECTED");
        }

        List<Long> ids = request.getIds();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("申请ID列表不能为空");
        }

        // 查询审核人真实姓名
        SysUser reviewer = sysUserMapper.selectById(UserContext.getUserId());
        String reviewerName = reviewer != null ? reviewer.getRealName() : UserContext.getUsername();
        LocalDateTime reviewTime = LocalDateTime.now();

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        for (Long id : ids) {
            try {
                SysEmployeeRequest employeeRequest = sysEmployeeRequestMapper.selectById(id);
                if (employeeRequest == null) {
                    failCount++;
                    errorMsg.append("ID ").append(id).append(": 申请不存在; ");
                    continue;
                }

                if (!"PENDING".equals(employeeRequest.getStatus())) {
                    failCount++;
                    errorMsg.append("ID ").append(id).append(": 已审核; ");
                    continue;
                }

                // 更新申请状态
                employeeRequest.setStatus(action);
                employeeRequest.setReviewerId(UserContext.getUserId());
                employeeRequest.setReviewerName(reviewerName);
                employeeRequest.setReviewComment(request.getReviewComment());
                employeeRequest.setReviewTime(reviewTime);
                sysEmployeeRequestMapper.updateById(employeeRequest);

                // 如果审核通过，创建员工账号
                if ("APPROVED".equals(action)) {
                    SysUser existUser = sysUserMapper.selectByUsername(employeeRequest.getUsername());
                    if (existUser != null) {
                        failCount++;
                        errorMsg.append("ID ").append(id).append(": 用户名已存在; ");
                        // 回滚状态
                        employeeRequest.setStatus("PENDING");
                        sysEmployeeRequestMapper.updateById(employeeRequest);
                        continue;
                    }

                    SysUser user = new SysUser();
                    user.setUsername(employeeRequest.getUsername());
                    String password = StringUtils.hasText(employeeRequest.getPassword()) ? employeeRequest.getPassword() : "123456";
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRoleId(employeeRequest.getRoleId());
                    user.setTenantId(employeeRequest.getTenantId());
                    user.setRealName(employeeRequest.getRealName());
                    user.setPostType(employeeRequest.getPostType());
                    user.setZhizhiContent(employeeRequest.getZhizhiContent());
                    user.setZhizhiImageUrl(employeeRequest.getZhizhiImageUrl());
                    user.setStatus(1);
                    sysUserMapper.insert(user);
                }

                successCount++;
            } catch (Exception e) {
                failCount++;
                errorMsg.append("ID ").append(id).append(": ").append(e.getMessage()).append("; ");
            }
        }

        log.info("批量审核完成: 总数={}, 成功={}, 失败={}", ids.size(), successCount, failCount);

        if (failCount > 0) {
            throw new BusinessException("批量审核完成，成功 " + successCount + " 条，失败 " + failCount + " 条。失败原因: " + errorMsg.toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SysEmployeeRequest employeeRequest = sysEmployeeRequestMapper.selectById(id);
        if (employeeRequest == null) {
            throw new BusinessException(404, "申请不存在");
        }

        if (!"PENDING".equals(employeeRequest.getStatus())) {
            throw new BusinessException("只能撤销待审核的申请");
        }

        // 校验是否属于同一租户
        if (!UserContext.isSuperAdmin() && !UserContext.getTenantId().equals(employeeRequest.getTenantId())) {
            throw new BusinessException(403, "无权撤销其他租户的申请");
        }

        employeeRequest.setStatus("CANCELLED");
        sysEmployeeRequestMapper.updateById(employeeRequest);

        log.info("员工申请已撤销: requestId={}", id);
    }
}
