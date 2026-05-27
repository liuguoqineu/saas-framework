package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.LoginReminderVO;
import com.saas.framework.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/reminder")
@Tag(name = "提醒中心", description = "登录聚合提醒、各类待办事项")
public class ReminderController {

    @Resource
    private ReminderService reminderService;

    @Operation(summary = "获取登录时的所有待处理提醒")
    @GetMapping("/login")
    public Result<LoginReminderVO> getLoginReminders() {
        log.info("获取登录聚合提醒");
        LoginReminderVO reminders = reminderService.getLoginReminders();
        return Result.ok(reminders);
    }
}
