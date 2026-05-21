package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.LoginRequest;
import com.saas.framework.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录和用户信息查询
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "登录、获取用户信息")
public class AuthController {

    @Resource
    private AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        log.info("登录请求: username={}", request.getUsername());
        Map<String, Object> data = authService.login(request);
        return Result.ok(data);
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> userInfo = authService.getUserInfo();
        return Result.ok(userInfo);
    }
}
