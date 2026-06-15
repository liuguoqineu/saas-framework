package com.saas.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.saas.framework.common.context.TenantContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * 核心配置：多租户插件 + 自动填充
 * <p>
 * 设计说明：
 * - sys_permission、sys_role_permission、sys_tenant 为全局表（无租户隔离）
 * - sys_user、sys_role 在忽略表中，由 Service 层手动控制租户过滤
 * - 这样超级账户(tenant_id=0)可查询全部数据，租户用户由 Service 层 wrapper 限制范围
 */
@Slf4j
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器
     * 配置多租户插件：自动在 SQL 中追加 tenant_id = ? 条件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件（必须添加，否则 selectPage 的 count 查询不会执行）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(com.baomidou.mybatisplus.annotation.DbType.MYSQL);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 多租户插件
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从 ThreadLocal 中获取当前租户ID
                Long tenantId = TenantContext.getTenantId();
                if (tenantId == null) {
                    tenantId = 0L;
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                // 指定租户字段名
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 全局表（无租户隔离）+ 关键表（由 Service 层手动控制租户过滤）
                // sys_user 必须忽略：登录时 TenantContext 未设置，默认 tenant_id=0 会阻止租户用户登录
                // sys_role 忽略：超级账户需要看到所有租户的角色
                return "sys_permission".equalsIgnoreCase(tableName)
                        || "sys_role_permission".equalsIgnoreCase(tableName)
                        || "sys_tenant".equalsIgnoreCase(tableName)
                        || "sys_user".equalsIgnoreCase(tableName)
                        || "sys_role".equalsIgnoreCase(tableName)
                        || "biz_customer".equalsIgnoreCase(tableName)
                        || "biz_customer_attachment".equalsIgnoreCase(tableName)
                        || "biz_customer_modify_log".equalsIgnoreCase(tableName)
                        || "biz_follow_up_record".equalsIgnoreCase(tableName)
                        || "biz_follow_up_reminder".equalsIgnoreCase(tableName)
                        || "biz_customer_status_log".equalsIgnoreCase(tableName)
                        || "biz_repair_order".equalsIgnoreCase(tableName)
                || "biz_repair_attachment".equalsIgnoreCase(tableName)
                || "biz_repair_process_log".equalsIgnoreCase(tableName)
                || "biz_contract_reminder".equalsIgnoreCase(tableName)
                || "sys_operation_log".equalsIgnoreCase(tableName)
                        || "sys_dict".equalsIgnoreCase(tableName)
                        || "sys_dict_item".equalsIgnoreCase(tableName)
                        || "rp_template".equalsIgnoreCase(tableName)
                        || "rp_approval".equalsIgnoreCase(tableName)
                        || "rp_config".equalsIgnoreCase(tableName)
                        || "rp_report".equalsIgnoreCase(tableName)
                        || "rp_report_revision".equalsIgnoreCase(tableName)
                        || "biz_check_in".equalsIgnoreCase(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);

        return interceptor;
    }

    /**
     * 自动填充处理器
     * 插入时自动填充 create_time 和 update_time
     * 更新时自动填充 update_time
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
