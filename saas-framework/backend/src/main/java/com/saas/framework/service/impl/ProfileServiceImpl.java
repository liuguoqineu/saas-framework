package com.saas.framework.service.impl;

import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.PasswordChangeRequest;
import com.saas.framework.common.dto.ProfileUpdateRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.config.FilePathConfig;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 个人信息管理服务实现
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private FilePathConfig filePathConfig;

    @Override
    public SysUser getProfile() {
        Long userId = UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        Long userId = UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        sysUserMapper.updateById(user);
        log.info("用户修改个人信息: userId={}", userId);
    }

    @Override
    public String updateAvatar(MultipartFile file) {
        Long userId = UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }

        // 校验文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过5MB");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 保存文件
        String avatarDir = filePathConfig.getUploadPath() + "avatar" + File.separator + datePath + File.separator;
        File dir = new File(avatarDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(avatarDir + fileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new BusinessException("头像上传失败");
        }

        // 生成访问URL
        String avatarUrl = "/uploads/avatar/" + datePath + "/" + fileName;
        user.setAvatar(avatarUrl);
        sysUserMapper.updateById(user);

        log.info("用户上传头像: userId={}, avatarUrl={}", userId, avatarUrl);
        return avatarUrl;
    }

    @Override
    public void changePassword(PasswordChangeRequest request) {
        Long userId = UserContext.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 校验旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 校验新密码不能与旧密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
        log.info("用户修改密码: userId={}", userId);
    }
}
