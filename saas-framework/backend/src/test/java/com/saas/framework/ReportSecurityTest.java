package com.saas.framework;

import com.saas.framework.common.context.TenantContext;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.dto.ReportRequest;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.report.RpReport;
import com.saas.framework.mapper.RpReportMapper;
import com.saas.framework.service.ReportService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 报表服务安全测试
 * 测试权限校验、归属校验、租户隔离
 *
 * 前置条件: 数据库 rp_report.template_id 列已修改为允许 NULL (DEFAULT NULL)
 *
 * 注意: 项目使用 MyBatis-Plus TenantLineInnerInterceptor 自动过滤 SQL，
 *       拦截器读取的是 TenantContext（非 UserContext），测试必须同时设置两者。
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportSecurityTest {

    @Resource
    private ReportService reportService;
    @Resource
    private RpReportMapper rpReportMapper;

    private static final Long ADMIN_USER_ID = 1L;   // 超管(admin)
    private static final Long TENANT_A_ID = 1L;       // 租户A
    private static final Long TENANT_B_ID = 999L;     // 租户B（不存在的租户）

    /**
     * 同时设置 UserContext 和 TenantContext
     */
    private void setContext(Long userId, Long tenantId) {
        UserContext.setUserId(userId);
        UserContext.setTenantId(tenantId);
        TenantContext.setTenantId(tenantId);
        UserContext.setPermissions(List.of("report:fill", "report:view"));
    }

    /**
     * 创建测试报表并返回ID
     */
    private Long createTestReport(Long userId, Long tenantId, String periodSuffix) {
        setContext(userId, tenantId);

        ReportRequest request = new ReportRequest();
        request.setReportType("DAILY");
        request.setReportPeriod("2026-06-05-" + periodSuffix);
        request.setContentText("测试内容-" + periodSuffix);
        request.setTemplateId(null);

        RpReport report = reportService.createOrSave(request);
        assertNotNull(report.getId(), "创建报表应成功，检查数据库 template_id 是否已允许 NULL");
        return report.getId();
    }

    @BeforeEach
    void setUp() {
        // 默认上下文：用户1，租户A
        setContext(ADMIN_USER_ID, TENANT_A_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
        TenantContext.remove();
    }

    // ==================== 测试1: resubmit 归属校验 ====================

    @Test
    @Order(1)
    @DisplayName("测试: 用户可以重新提交自己的被驳回报表")
    void testResubmitOwnReport() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t1");

            // 查询时也需设置 TenantContext，否则拦截器加 tenant_id=0 查不到
            setContext(ADMIN_USER_ID, TENANT_A_ID);
            RpReport report = rpReportMapper.selectById(holder[0]);
            assertNotNull(report, "应能查到刚创建的报表");
            report.setStatus("REJECTED");
            rpReportMapper.updateById(report);

            // 重新提交自己的报表
            setContext(ADMIN_USER_ID, TENANT_A_ID);
            ReportRequest req = new ReportRequest();
            req.setContentText("修改后的内容");

            assertDoesNotThrow(() -> reportService.resubmit(holder[0], req),
                    "用户应该可以重新提交自己的被驳回报表");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    @Test
    @Order(2)
    @DisplayName("测试: 用户不能重新提交他人的被驳回报表")
    void testResubmitOtherUserReport() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t2");

            setContext(ADMIN_USER_ID, TENANT_A_ID);
            RpReport report = rpReportMapper.selectById(holder[0]);
            assertNotNull(report);
            report.setStatus("REJECTED");
            rpReportMapper.updateById(report);

            // 切换为另一个用户身份（同租户）
            setContext(999L, TENANT_A_ID);
            ReportRequest req = new ReportRequest();
            req.setContentText("修改后");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.resubmit(holder[0], req),
                    "用户不应该能重新提交他人的报表");
            assertEquals(403, ex.getCode(), "应返回403权限错误");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试: 超管可以重新提交任何人的被驳回报表")
    void testSuperAdminResubmitOtherReport() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t3");

            setContext(ADMIN_USER_ID, TENANT_A_ID);
            RpReport report = rpReportMapper.selectById(holder[0]);
            assertNotNull(report);
            report.setStatus("REJECTED");
            rpReportMapper.updateById(report);

            // 超管(tenantId=0) 重新提交 — isSuperAdmin=true 绕过归属校验
            setContext(ADMIN_USER_ID, 0L);
            ReportRequest req = new ReportRequest();
            req.setContentText("超管修改");

            assertDoesNotThrow(() -> reportService.resubmit(holder[0], req),
                    "超管应该可以重新提交任何人的报表");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    // ==================== 测试2: getApprovalChain 租户隔离 ====================

    @Test
    @Order(4)
    @DisplayName("测试: 用户可以查看本租户报表的审批链")
    void testGetApprovalChainSameTenant() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t4");

            setContext(ADMIN_USER_ID, TENANT_A_ID);
            assertDoesNotThrow(() -> reportService.getApprovalChain(holder[0]),
                    "用户应该可以查看本租户报表的审批链");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    @Test
    @Order(5)
    @DisplayName("测试: 用户不能查看其他租户报表的审批链")
    void testGetApprovalChainOtherTenant() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t5");

            setContext(888L, TENANT_B_ID);  // 不同租户

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.getApprovalChain(holder[0]),
                    "用户不应该能查看其他租户报表的审批链");
            assertEquals(403, ex.getCode(), "应返回403权限错误");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    // ==================== 测试3: getRevisions 租户隔离 ====================

    @Test
    @Order(6)
    @DisplayName("测试: 用户可以查看本租户报表的修改记录")
    void testGetRevisionsSameTenant() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t6");

            setContext(ADMIN_USER_ID, TENANT_A_ID);
            assertDoesNotThrow(() -> reportService.getRevisions(holder[0]),
                    "用户应该可以查看本租户报表的修改记录");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    @Test
    @Order(7)
    @DisplayName("测试: 用户不能查看其他租户报表的修改记录")
    void testGetRevisionsOtherTenant() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t7");

            setContext(888L, TENANT_B_ID);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.getRevisions(holder[0]),
                    "用户不应该能查看其他租户报表的修改记录");
            assertEquals(403, ex.getCode(), "应返回403权限错误");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    // ==================== 测试4: submit 归属校验 ====================

    @Test
    @Order(8)
    @DisplayName("测试: 用户不能提交他人的报表")
    void testSubmitOtherUserReport() {
        final Long[] holder = new Long[1];
        try {
            holder[0] = createTestReport(ADMIN_USER_ID, TENANT_A_ID, "t8");

            setContext(999L, TENANT_A_ID);  // 另一个用户

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.submit(holder[0]),
                    "用户不应该能提交他人的报表");
            assertEquals(403, ex.getCode(), "应返回403权限错误");
        } finally {
            if (holder[0] != null) cleanDelete(holder[0]);
        }
    }

    /**
     * 清理测试数据：先恢复原租户上下文再删除（绕过拦截器）
     */
    private void cleanDelete(Long id) {
        setContext(ADMIN_USER_ID, TENANT_A_ID);
        rpReportMapper.deleteById(id);
    }
}
