package com.saas.framework.service;

import com.saas.framework.common.dto.LoginRequest;

import java.util.Map;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * @return { token, userInfo }
     */
    Map<String, Object> login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     * @return userInfo
     */
    Map<String, Object> getUserInfo();
}
