package com.saas.framework.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.entity.SysDict;
import com.saas.framework.entity.SysDictItem;
import com.saas.framework.entity.SysPermission;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysRolePermission;
import com.saas.framework.entity.SysUser;
import com.saas.framework.mapper.SysDictItemMapper;
import com.saas.framework.mapper.SysDictMapper;
import com.saas.framework.mapper.SysPermissionMapper;
import com.saas.framework.mapper.SysRoleMapper;
import com.saas.framework.mapper.SysRolePermissionMapper;
import com.saas.framework.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysPermissionMapper sysPermissionMapper;

    @Resource
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private DataSource dataSource;

    @Resource
    private SysDictMapper sysDictMapper;

    @Resource
    private SysDictItemMapper sysDictItemMapper;

    @Override
    public void run(String... args) {
        log.info("========== 开始检查并初始化默认数据 ==========");

        initOperationLogTable();
        initDictTables();
        initSuperRole();
        initSuperAdmin();
        initContractPermissions();
        initRepairPermissions();
        initStatisticsPermissions();
        initVisitPermissions();
        initFinancePermissions();
        initOperationLogPermissions();
        initFollowUpPermissions();
        syncSuperRolePermissions();
        initDictData();
//        initDefaultRoles();

        log.info("========== 默认数据初始化完成 ==========");
    }

    private void initOperationLogTable() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sys_operation_log (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "user_id BIGINT DEFAULT NULL COMMENT '操作人ID'," +
                    "username VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名'," +
                    "real_name VARCHAR(50) DEFAULT NULL COMMENT '操作人真实姓名'," +
                    "operation VARCHAR(50) NOT NULL COMMENT '操作类型'," +
                    "module VARCHAR(50) DEFAULT NULL COMMENT '操作模块'," +
                    "description VARCHAR(500) DEFAULT NULL COMMENT '操作描述'," +
                    "method VARCHAR(200) DEFAULT NULL COMMENT '请求方法'," +
                    "request_url VARCHAR(500) DEFAULT NULL COMMENT '请求URL'," +
                    "request_params TEXT DEFAULT NULL COMMENT '请求参数'," +
                    "ip VARCHAR(50) DEFAULT NULL COMMENT '操作IP'," +
                    "tenant_id BIGINT DEFAULT NULL COMMENT '租户ID'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'," +
                    "PRIMARY KEY (id)," +
                    "INDEX idx_user_id (user_id)," +
                    "INDEX idx_tenant_id (tenant_id)," +
                    "INDEX idx_operation (operation)," +
                    "INDEX idx_module (module)," +
                    "INDEX idx_create_time (create_time)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表'");
            log.info("操作日志表检查/创建完成");
        } catch (Exception e) {
            log.warn("操作日志表创建失败（可能已存在）: {}", e.getMessage());
        }
    }

    private void initSuperRole() {
        SysRole superRole = sysRoleMapper.selectById(1L);
        if (superRole != null) {
            log.info("超级角色已存在，跳过初始化");
            return;
        }

        superRole = new SysRole();
        superRole.setId(1L);
        superRole.setName("超级管理员");
        superRole.setTenantId(0L);
        sysRoleMapper.insert(superRole);

        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        for (SysPermission perm : allPermissions) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(1L);
            rp.setPermissionId(perm.getId());
            sysRolePermissionMapper.insert(rp);
        }

        log.info("超级角色创建成功，已分配 {} 个权限", allPermissions.size());
    }

    private void initSuperAdmin() {
        SysUser admin = sysUserMapper.selectByUsername("admin");
        if (admin != null) {
            log.info("超级账户 admin 已存在，跳过初始化");
            return;
        }

        admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRoleId(1L);
        admin.setTenantId(0L);
        admin.setRealName("超级管理员");
        admin.setStatus(1);
        sysUserMapper.insert(admin);

        log.info("超级账户创建成功！用户名: admin, 密码: 123456");
    }

    private void initContractPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long contractMenuId = getOrCreatePermission("合同管理", "contract", "menu", menuParentId, 3);

        getOrCreatePermission("合同列表", "contract:list", "button", contractMenuId, 1);
        getOrCreatePermission("新增合同", "contract:add", "button", contractMenuId, 2);
        getOrCreatePermission("编辑合同", "contract:edit", "button", contractMenuId, 3);
        getOrCreatePermission("删除合同", "contract:delete", "button", contractMenuId, 4);
        getOrCreatePermission("合同状态变更", "contract:status", "button", contractMenuId, 5);
        getOrCreatePermission("合同到期提醒", "contract:remind", "button", contractMenuId, 6);

        log.info("合同管理权限初始化完成");
    }

    private void initRepairPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long repairMenuId = getOrCreatePermission("报修管理", "repair", "menu", menuParentId, 4);

        getOrCreatePermission("报修列表", "repair:list", "button", repairMenuId, 1);
        getOrCreatePermission("新增报修", "repair:add", "button", repairMenuId, 2);
        getOrCreatePermission("编辑报修", "repair:edit", "button", repairMenuId, 3);
        getOrCreatePermission("删除报修", "repair:delete", "button", repairMenuId, 4);
        getOrCreatePermission("分配报修", "repair:assign", "button", repairMenuId, 5);
        getOrCreatePermission("处理报修", "repair:process", "button", repairMenuId, 6);
        getOrCreatePermission("确认报修", "repair:confirm", "button", repairMenuId, 7);
        getOrCreatePermission("异常处理", "repair:exception", "button", repairMenuId, 8);
        getOrCreatePermission("报修统计", "repair:stats", "button", repairMenuId, 9);
        getOrCreatePermission("导出报修", "repair:export", "button", repairMenuId, 10);

        log.info("报修管理权限初始化完成");
    }

    private void initStatisticsPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long statsMenuId = getOrCreatePermission("统计分析", "statistics", "menu", menuParentId, 5);

        getOrCreatePermission("客户统计", "statistics:customer", "button", statsMenuId, 1);
        getOrCreatePermission("报修统计", "statistics:repair", "button", statsMenuId, 2);
        getOrCreatePermission("拜访统计", "statistics:visit", "button", statsMenuId, 3);
        getOrCreatePermission("合同统计", "statistics:contract", "button", statsMenuId, 4);
        getOrCreatePermission("统计导出", "statistics:export", "button", statsMenuId, 5);

        log.info("统计分析权限初始化完成");
    }

    private void initVisitPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long visitMenuId = getOrCreatePermission("拜访管理", "visit", "menu", menuParentId, 6);

        getOrCreatePermission("拜访列表", "visit:list", "button", visitMenuId, 1);
        getOrCreatePermission("新增拜访", "visit:add", "button", visitMenuId, 2);
        getOrCreatePermission("编辑拜访", "visit:edit", "button", visitMenuId, 3);
        getOrCreatePermission("删除拜访", "visit:delete", "button", visitMenuId, 4);
        getOrCreatePermission("导出拜访", "visit:export", "button", visitMenuId, 5);

        log.info("拜访管理权限初始化完成");
    }

    private void initFinancePermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long financeMenuId = getOrCreatePermission("财务管理", "finance", "menu", menuParentId, 7);

        getOrCreatePermission("合同金额查看", "finance:contract", "button", financeMenuId, 1);
        getOrCreatePermission("费用查看", "finance:expense", "button", financeMenuId, 2);

        log.info("财务管理权限初始化完成");
    }

    private void initOperationLogPermissions() {
        Long systemMenuId = getOrCreatePermission("系统管理", "system", "menu", 0L, 1);
        Long logMenuId = getOrCreatePermission("操作日志", "log", "menu", systemMenuId, 4);

        getOrCreatePermission("日志列表", "log:list", "button", logMenuId, 1);
        getOrCreatePermission("导出日志", "log:export", "button", logMenuId, 2);

        log.info("操作日志权限初始化完成");
    }

    private void initFollowUpPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long customerMenuId = getOrCreatePermission("客户管理", "customer", "menu", menuParentId, 2);

        getOrCreatePermission("跟进列表", "followup:list", "button", customerMenuId, 8);
        getOrCreatePermission("新增跟进", "followup:add", "button", customerMenuId, 9);
        getOrCreatePermission("编辑跟进", "followup:edit", "button", customerMenuId, 10);
        getOrCreatePermission("删除跟进", "followup:delete", "button", customerMenuId, 11);
        getOrCreatePermission("导出跟进", "followup:export", "button", customerMenuId, 12);
        getOrCreatePermission("状态变更", "followup:status", "button", customerMenuId, 13);

        log.info("跟进管理权限初始化完成");
    }

    private void syncSuperRolePermissions() {
        SysRole superRole = sysRoleMapper.selectById(1L);
        if (superRole == null) {
            return;
        }

        List<SysPermission> allPermissions = sysPermissionMapper.selectList(null);
        for (SysPermission perm : allPermissions) {
            SysRolePermission existing = new SysRolePermission();
            existing.setRoleId(1L);
            existing.setPermissionId(perm.getId());
            try {
                sysRolePermissionMapper.insert(existing);
            } catch (Exception ignored) {
            }
        }

        log.info("超级角色权限同步完成");
    }

    private void initDefaultRoles() {
        createRoleIfNotExists("管理员", 0L, Arrays.asList(
                "system", "tenant", "role", "user", "business", "customer", "contract",
                "repair", "statistics", "visit", "finance", "log",
                "tenant:list", "tenant:add", "tenant:edit", "tenant:delete",
                "role:list", "role:add", "role:edit", "role:delete",
                "user:list", "user:add", "user:edit", "user:delete",
                "customer:list", "customer:add", "customer:edit", "customer:delete",
                "customer:invalid", "customer:import", "customer:export",
                "followup:list", "followup:add", "followup:edit", "followup:delete",
                "followup:export", "followup:status",
                "contract:list", "contract:add", "contract:edit", "contract:delete",
                "contract:status", "contract:remind",
                "repair:list", "repair:add", "repair:edit", "repair:delete",
                "repair:assign", "repair:process", "repair:confirm", "repair:exception",
                "repair:stats", "repair:export",
                "statistics:customer", "statistics:repair", "statistics:visit",
                "statistics:contract", "statistics:export",
                "visit:list", "visit:add", "visit:edit", "visit:delete", "visit:export",
                "finance:contract", "finance:expense",
                "log:list", "log:export"
        ));

        createRoleIfNotExists("客户专员", 0L, Arrays.asList(
                "business", "customer", "contract",
                "customer:list", "customer:add", "customer:edit", "customer:export",
                "followup:list", "followup:add", "followup:edit",
                "contract:list", "contract:add"
        ));

        createRoleIfNotExists("运维人员", 0L, Arrays.asList(
                "business", "repair",
                "repair:list", "repair:add", "repair:edit",
                "repair:assign", "repair:process", "repair:confirm",
                "repair:exception", "repair:stats"
        ));

        createRoleIfNotExists("拜访人员", 0L, Arrays.asList(
                "business", "visit", "customer",
                "visit:list", "visit:add", "visit:edit", "visit:delete", "visit:export",
                "customer:list"
        ));

        createRoleIfNotExists("财务", 0L, Arrays.asList(
                "business", "finance", "contract",
                "finance:contract", "finance:expense",
                "contract:list"
        ));

        log.info("默认角色模板初始化完成");
    }

    private void createRoleIfNotExists(String roleName, Long tenantId, List<String> permissionCodes) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getName, roleName);
        wrapper.eq(SysRole::getTenantId, tenantId);
        SysRole existing = sysRoleMapper.selectOne(wrapper);
        if (existing != null) {
            return;
        }

        SysRole role = new SysRole();
        role.setName(roleName);
        role.setTenantId(tenantId);
        sysRoleMapper.insert(role);

        for (String code : permissionCodes) {
            LambdaQueryWrapper<SysPermission> permWrapper = new LambdaQueryWrapper<>();
            permWrapper.eq(SysPermission::getCode, code);
            SysPermission perm = sysPermissionMapper.selectOne(permWrapper);
            if (perm != null) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(perm.getId());
                try {
                    sysRolePermissionMapper.insert(rp);
                } catch (Exception ignored) {
                }
            }
        }

        log.info("默认角色「{}」创建成功", roleName);
    }

    private Long getOrCreatePermission(String name, String code, String type, Long parentId, int sort) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getCode, code);
        SysPermission existing = sysPermissionMapper.selectOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }

        SysPermission permission = new SysPermission();
        permission.setName(name);
        permission.setCode(code);
        permission.setType(type);
        permission.setParentId(parentId);
        permission.setSort(sort);
        sysPermissionMapper.insert(permission);

        return permission.getId();
    }

    private void initDictTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS sys_dict (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "code VARCHAR(100) NOT NULL COMMENT '字典编码'," +
                    "name VARCHAR(100) NOT NULL COMMENT '字典名称'," +
                    "description VARCHAR(500) DEFAULT NULL COMMENT '字典描述'," +
                    "status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用'," +
                    "sort INT DEFAULT 0 COMMENT '排序号'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除'," +
                    "PRIMARY KEY (id)," +
                    "UNIQUE KEY uk_code (code)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典类型表'");

            stmt.execute("CREATE TABLE IF NOT EXISTS sys_dict_item (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID'," +
                    "dict_id BIGINT NOT NULL COMMENT '字典类型ID'," +
                    "value VARCHAR(200) NOT NULL COMMENT '字典项值'," +
                    "label VARCHAR(200) NOT NULL COMMENT '字典项标签'," +
                    "parent_value VARCHAR(200) DEFAULT NULL COMMENT '父项值'," +
                    "sort INT DEFAULT 0 COMMENT '排序号'," +
                    "status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用'," +
                    "remark VARCHAR(500) DEFAULT NULL COMMENT '备注说明'," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                    "deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除'," +
                    "PRIMARY KEY (id)," +
                    "INDEX idx_dict_id (dict_id)," +
                    "INDEX idx_parent_value (parent_value)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典项表'");

            log.info("字典表检查/创建完成");
        } catch (Exception e) {
            log.warn("字典表创建失败（可能已存在）: {}", e.getMessage());
        }
    }

    private void initDictData() {
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDict::getCode, "business_category");
        SysDict existing = sysDictMapper.selectOne(wrapper);
        if (existing != null) {
            log.info("字典数据已存在，跳过初始化");
            return;
        }

        Long businessCategoryId = createDict("business_category", "业务类型一级分类", "客户业务类型一级分类", 1);
        Long businessTypeId = createDict("business_type", "业务类型二级分类", "客户业务类型二级分类，关联一级分类", 2);
        Long cooperationCategoryId = createDict("cooperation_category", "合作状态一级分类", "客户合作状态一级分类", 3);
        Long cooperationStatusId = createDict("cooperation_status", "合作状态二级分类", "客户合作状态二级分类", 4);
        Long maintenanceCategoryId = createDict("maintenance_category", "运维需求分类", "客户运维需求分类", 5);
        Long gasScaleId = createDict("gas_scale", "用气规模分类", "工业客户用气规模分类", 6);

        createDictItem(businessCategoryId, "加气站类", "加气站类", null, 1, "加气站类客户");
        createDictItem(businessCategoryId, "商业用气", "商业用气", null, 2, "商业用气客户");
        createDictItem(businessCategoryId, "工业用气", "工业用气", null, 3, "工业用气客户");

        createDictItem(businessTypeId, "CNG加气站", "CNG加气站", "加气站类", 1, null);
        createDictItem(businessTypeId, "LPG加气站", "LPG加气站", "加气站类", 2, null);
        createDictItem(businessTypeId, "餐饮类", "餐饮类（饭店、餐馆）", "商业用气", 3, null);
        createDictItem(businessTypeId, "团餐类", "团餐类（大企业食堂、高校食堂）", "商业用气", 4, null);
        createDictItem(businessTypeId, "其他商业类", "其他商业类（酒店、商超后厨等）", "商业用气", 5, null);
        createDictItem(businessTypeId, "大型", "大型", "工业用气", 6, null);
        createDictItem(businessTypeId, "中型", "中型", "工业用气", 7, null);
        createDictItem(businessTypeId, "小型", "小型", "工业用气", 8, null);

        createDictItem(cooperationCategoryId, "已合作", "已合作", null, 1, "已合作客户");
        createDictItem(cooperationCategoryId, "潜在", "潜在", null, 2, "潜在客户");
        createDictItem(cooperationCategoryId, "无效", "无效", null, 3, "无效客户");

        createDictItem(cooperationStatusId, "正常履约", "正常履约（已签合同，正在使用系统，无逾期）", "已合作", 1, null);
        createDictItem(cooperationStatusId, "终止合作", "终止合作（不再合作，留存历史数据）", "已合作", 2, null);
        createDictItem(cooperationStatusId, "高潜力", "高潜力（明确需求，短期内可签约）", "潜在", 3, null);
        createDictItem(cooperationStatusId, "中潜力", "中潜力（有需求但时间不明确）", "潜在", 4, null);
        createDictItem(cooperationStatusId, "低潜力", "低潜力（需求不明确，需长期跟进）", "潜在", 5, null);
        createDictItem(cooperationStatusId, "无效客户", "无效客户（多次无回应或不符合服务范围）", "无效", 6, null);

        createDictItem(maintenanceCategoryId, "高频报修", "高频报修客户（需重点关注）", null, 1, null);
        createDictItem(maintenanceCategoryId, "常规运维", "常规运维客户（按计划巡检）", null, 2, null);
        createDictItem(maintenanceCategoryId, "无报修", "无报修客户（系统稳定）", null, 3, null);

        createDictItem(gasScaleId, "大型", "大型", null, 1, null);
        createDictItem(gasScaleId, "中型", "中型", null, 2, null);
        createDictItem(gasScaleId, "小型", "小型", null, 3, null);

        log.info("字典数据初始化完成");
    }

    private Long createDict(String code, String name, String description, int sort) {
        SysDict dict = new SysDict();
        dict.setCode(code);
        dict.setName(name);
        dict.setDescription(description);
        dict.setSort(sort);
        dict.setStatus(1);
        sysDictMapper.insert(dict);
        return dict.getId();
    }

    private void createDictItem(Long dictId, String value, String label, String parentValue, int sort, String remark) {
        SysDictItem item = new SysDictItem();
        item.setDictId(dictId);
        item.setValue(value);
        item.setLabel(label);
        item.setParentValue(parentValue);
        item.setSort(sort);
        item.setStatus(1);
        item.setRemark(remark);
        sysDictItemMapper.insert(item);
    }
}
