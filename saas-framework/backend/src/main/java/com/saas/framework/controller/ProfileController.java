package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.PasswordChangeRequest;
import com.saas.framework.common.dto.ProfileUpdateRequest;
import com.saas.framework.entity.SysUser;
import com.saas.framework.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 个人信息管理控制器（手机端）
 * 当前登录用户操作自己的个人信息
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@Tag(name = "个人信息管理", description = "手机端个人信息修改、头像、密码")
public class ProfileController {

    @Resource
    private ProfileService profileService;

    @Operation(summary = "获取当前用户个人信息")
    @GetMapping
    public Result<SysUser> getProfile() {
        SysUser user = profileService.getProfile();
        return Result.ok(user);
    }

    @Operation(summary = "修改个人信息")
    @PutMapping
    public Result<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        log.info("修改个人信息: userId={}", com.saas.framework.common.context.UserContext.getUserId());
        profileService.updateProfile(request);
        return Result.ok("个人信息修改成功");
    }

    @Operation(summary = "上传/修改头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> updateAvatar(@RequestParam("file") MultipartFile file) {
        log.info("上传头像: userId={}", com.saas.framework.common.context.UserContext.getUserId());
        String avatarUrl = profileService.updateAvatar(file);
        return Result.ok(Map.of("avatarUrl", avatarUrl));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        log.info("修改密码: userId={}", com.saas.framework.common.context.UserContext.getUserId());
        profileService.changePassword(request);
        return Result.ok("密码修改成功");
    }
}
