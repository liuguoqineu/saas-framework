package com.saas.framework.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.entity.SysOperationLog;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysUserMapper;
import com.saas.framework.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private SysUserMapper sysUserMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.saas.framework.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Exception caught = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            caught = e;
            throw e;
        } finally {
            try {
                saveLog(joinPoint, caught);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Exception ex) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);

        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setOperation(annotation.operation());
        logEntity.setModule(annotation.module());
        logEntity.setDescription(annotation.description() + (ex != null ? "（失败）" : ""));
        logEntity.setUserId(UserContext.getUserId());
        logEntity.setUsername(UserContext.getUsername());
        logEntity.setTenantId(UserContext.getTenantId());

        SysUser user = sysUserMapper.selectById(UserContext.getUserId());
        if (user != null) {
            logEntity.setRealName(user.getRealName());
        }

        logEntity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setIp(getIpAddress(request));
        }

        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String params = objectMapper.writeValueAsString(args);
                if (params.length() > 2000) {
                    params = params.substring(0, 2000);
                }
                logEntity.setRequestParams(params);
            }
        } catch (Exception e) {
            logEntity.setRequestParams("[]");
        }

        operationLogService.save(logEntity);
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
