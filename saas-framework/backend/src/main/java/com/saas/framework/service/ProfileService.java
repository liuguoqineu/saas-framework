package com.saas.framework.service;

import com.saas.framework.common.dto.PasswordChangeRequest;
import com.saas.framework.common.dto.ProfileUpdateRequest;
import com.saas.framework.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人信息管理服务接口
 */
public interface ProfileService {

    /**
     * 获取当前用户个人信息
     */
    SysUser getProfile();

    /**
     * 修改个人信息
     */
    void updateProfile(ProfileUpdateRequest request);

    /**
     * 上传/修改头像
     * @return 头像URL
     */
    String updateAvatar(MultipartFile file);

    /**
     * 修改密码
     */
    void changePassword(PasswordChangeRequest request);
}
