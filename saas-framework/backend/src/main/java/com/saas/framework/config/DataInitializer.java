package com.saas.framework.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.framework.entity.SysDict;
import com.saas.framework.entity.SysDictItem;
import com.saas.framework.entity.SysPermission;
import com.saas.framework.entity.SysRole;
import com.saas.framework.entity.SysRolePermission;
import com.saas.framework.entity.SysUser;
import com.saas.framework.entity.report.RpTemplate;
import com.saas.framework.mapper.RpTemplateMapper;
import com.saas.framework.mapper.RpConfigMapper;
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

import javax.annotation.PostConstruct;
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

    @Resource
    private RpTemplateMapper rpTemplateMapper;

    @Resource
    private RpConfigMapper rpConfigMapper;

    @PostConstruct
    public void init() {
        addMissingColumns();
    }

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
        initCustomerPermissions();
        syncSuperRolePermissions();
        initDictData();
        initReportTables();
        initReportPermissions();
        initReportTemplates();
        initCheckInPermissions();
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
        admin.setPostType("DEV");
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

    private void initCustomerPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long customerMenuId = getOrCreatePermission("客户管理", "customer", "menu", menuParentId, 1);

        // 客户管理基础权限
        getOrCreatePermission("客户列表", "customer:list", "button", customerMenuId, 1);
        getOrCreatePermission("新增客户", "customer:add", "button", customerMenuId, 2);
        getOrCreatePermission("编辑客户", "customer:edit", "button", customerMenuId, 3);
        getOrCreatePermission("删除客户", "customer:delete", "button", customerMenuId, 4);
        getOrCreatePermission("标记无效", "customer:invalid", "button", customerMenuId, 5);
        getOrCreatePermission("导入客户", "customer:import", "button", customerMenuId, 6);
        getOrCreatePermission("导出客户", "customer:export", "button", customerMenuId, 7);
        // 客户分配/转移/回收权限（租户管理员可用）
        getOrCreatePermission("查看全部客户", "customer:all", "button", customerMenuId, 8);
        getOrCreatePermission("分配/转移/回收客户", "customer:assign", "button", customerMenuId, 9);

        log.info("客户管理权限初始化完成");
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
                "customer:invalid", "customer:import", "customer:export", "customer:all", "customer:assign",
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

        // 租户管理员：拥有客户分配/转移/回收权限
        createRoleIfNotExists("客户专员", 0L, Arrays.asList(
                "business", "customer", "contract",
                "customer:list", "customer:add", "customer:edit", "customer:export", "customer:assign",
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
        Long cooperationStatusId = createDict("cooperation_status", "合作状态", "客户合作状态", 3);
        Long maintenanceCategoryId = createDict("maintenance_category", "运维需求分类", "客户运维需求分类", 4);
        Long gasScaleId = createDict("gas_scale", "用气规模分类", "工业客户用气规模分类", 5);

        createDictItem(businessCategoryId, "加气站类", "加气站类", null, 1, "加气站类客户");
        createDictItem(businessCategoryId, "商业用气", "商业用气", null, 2, "商业用气客户");
        createDictItem(businessCategoryId, "民业用气", "民业用气", null, 3, "民业用气客户");

        createDictItem(businessTypeId, "CNG加气站", "CNG加气站", "加气站类", 1, null);
        createDictItem(businessTypeId, "LPG加气站", "LPG加气站", "加气站类", 2, null);
        createDictItem(businessTypeId, "餐饮类", "餐饮类（饭店、餐馆）", "商业用气", 3, null);
        createDictItem(businessTypeId, "团餐类", "团餐类（大企业食堂、高校食堂）", "商业用气", 4, null);
        createDictItem(businessTypeId, "其他商业类", "其他商业类（酒店、商超后厨等）", "商业用气", 5, null);
        createDictItem(businessTypeId, "大型", "大型", "民业用气", 6, null);
        createDictItem(businessTypeId, "中型", "中型", "民业用气", 7, null);
        createDictItem(businessTypeId, "小型", "小型", "民业用气", 8, null);

        createDictItem(cooperationStatusId, "正常履约", "正常履约（已签合同，正在使用系统，无逾期）", null, 1, null);
        createDictItem(cooperationStatusId, "终止合作", "终止合作（不再合作，留存历史数据）", null, 2, null);
        createDictItem(cooperationStatusId, "高潜力", "高潜力（明确需求，短期内可签约）", null, 3, null);
        createDictItem(cooperationStatusId, "中潜力", "中潜力（有需求但时间不明确）", null, 4, null);
        createDictItem(cooperationStatusId, "低潜力", "低潜力（需求不明确，需长期跟进）", null, 5, null);
        createDictItem(cooperationStatusId, "无效客户", "无效客户", null, 6, null);

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

    // ==================== 报表模块初始化 ====================

    private void initReportTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_template ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',"
                + "template_code VARCHAR(32) NOT NULL COMMENT '模板编码',"
                + "template_name VARCHAR(64) NOT NULL COMMENT '模板名称',"
                + "post_type VARCHAR(16) NOT NULL COMMENT '岗位类型',"
                + "report_type VARCHAR(16) NOT NULL COMMENT '报表类型',"
                + "template_desc TEXT DEFAULT NULL COMMENT '模板说明/填写指引',"
                + "is_enabled TINYINT(1) NOT NULL DEFAULT 1,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id), UNIQUE KEY uk_template_code (template_code)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表模板表'");
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_report ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "dept_id BIGINT DEFAULT NULL,"
                + "template_id BIGINT DEFAULT NULL COMMENT '模板ID，允许为空支持无模板填报',"
                + "report_type VARCHAR(16) NOT NULL,"
                + "report_period VARCHAR(32) NOT NULL,"
                + "content_text TEXT COMMENT '填报内容(纯文本)',"
                + "status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',"
                + "submit_time DATETIME DEFAULT NULL,"
                + "tenant_id BIGINT NOT NULL DEFAULT 0,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "deleted TINYINT(1) NOT NULL DEFAULT 0,"
                + "PRIMARY KEY (id), INDEX idx_user_period (user_id, report_period),"
                + "INDEX idx_dept_status (dept_id, status), INDEX idx_period_type (report_period, report_type),"
                + "INDEX idx_tenant_id (tenant_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表记录表'");
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_approval ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "report_id BIGINT NOT NULL,"
                + "approver_id BIGINT NOT NULL,"
                + "approval_level INT NOT NULL DEFAULT 1,"
                + "status VARCHAR(16) NOT NULL DEFAULT 'PENDING',"
                + "comment VARCHAR(512) DEFAULT NULL,"
                + "approve_time DATETIME DEFAULT NULL,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id), INDEX idx_report (report_id),"
                + "INDEX idx_approver_status (approver_id, status)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表'");
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_report_revision ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "report_id BIGINT NOT NULL,"
                + "revision_type VARCHAR(16) NOT NULL,"
                + "content_snapshot TEXT,"
                + "operator_id BIGINT NOT NULL,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id), INDEX idx_report (report_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表修改记录表'");
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_overdue ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "report_type VARCHAR(16) NOT NULL,"
                + "report_period VARCHAR(32) NOT NULL,"
                + "deadline DATETIME NOT NULL,"
                + "is_reminded TINYINT(1) NOT NULL DEFAULT 0,"
                + "tenant_id BIGINT NOT NULL DEFAULT 0,"
                + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id), INDEX idx_user_type_period (user_id, report_type, report_period),"
                + "INDEX idx_tenant_id (tenant_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逾期记录表'");
            stmt.execute("CREATE TABLE IF NOT EXISTS rp_config ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "config_key VARCHAR(64) NOT NULL,"
                + "config_value VARCHAR(512) NOT NULL,"
                + "description VARCHAR(256) DEFAULT NULL,"
                + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id), UNIQUE KEY uk_config_key (config_key)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表系统配置表'");
            log.info("报表模块数据库表检查/创建完成");
        } catch (Exception e) {
            log.warn("报表表创建失败(可能已存在): {}", e.getMessage());
        }
    }

    private void addMissingColumns() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            var rs = conn.getMetaData().getColumns(null, null, "rp_template", "template_desc");
            if (!rs.next()) {
                stmt.execute("ALTER TABLE rp_template ADD COLUMN template_desc TEXT DEFAULT NULL COMMENT '模板说明/填写指引' AFTER report_type");
                log.info("rp_template表添加template_desc字段成功");
            }

            var postTypeRs = conn.getMetaData().getColumns(null, null, "sys_user", "post_type");
            if (!postTypeRs.next()) {
                stmt.execute("ALTER TABLE sys_user ADD COLUMN post_type VARCHAR(16) DEFAULT NULL COMMENT '岗位类型: DEV-研发, OPS-运维, CS-客服' AFTER status");
                log.info("sys_user表添加post_type字段成功");
            }

            var leaderIdRs = conn.getMetaData().getColumns(null, null, "sys_user", "leader_id");
            if (!leaderIdRs.next()) {
                stmt.execute("ALTER TABLE sys_user ADD COLUMN leader_id BIGINT DEFAULT NULL COMMENT '直属领导ID' AFTER post_type");
                log.info("sys_user表添加leader_id字段成功");
            }

            // 确保 rp_report.template_id 允许 NULL 且有默认值（兼容旧库）
            var templateIdRs = conn.getMetaData().getColumns(null, null, "rp_report", "template_id");
            if (templateIdRs.next()) {
                String isNullable = templateIdRs.getString("IS_NULLABLE");
                String defaultValue = templateIdRs.getString("COLUMN_DEF");
                boolean needModify = "NO".equals(isNullable) || defaultValue == null;
                if (needModify) {
                    stmt.execute("ALTER TABLE rp_report MODIFY COLUMN template_id BIGINT DEFAULT NULL COMMENT '模板ID，允许为空支持无模板填报'");
                    log.info("rp_report表template_id字段修改为允许NULL且默认NULL成功");
                }
            }

            log.info("缺失字段检查/添加完成");
        } catch (Exception e) {
            log.warn("添加缺失字段失败: {}", e.getMessage());
        }
    }

    private void initReportPermissions() {
        Long reportMenuId = getOrCreatePermission("工作报表", "report", "menu", 0L, 3);
        getOrCreatePermission("我的报表", "report:fill", "button", reportMenuId, 1);
        getOrCreatePermission("报表查看", "report:view", "button", reportMenuId, 2);
        getOrCreatePermission("审批管理", "report:approve", "button", reportMenuId, 3);
        getOrCreatePermission("报表导出", "report:export", "button", reportMenuId, 4);
        getOrCreatePermission("数据看板", "report:dashboard", "button", reportMenuId, 5);
        getOrCreatePermission("逾期管理", "report:overdue:manage", "button", reportMenuId, 6);
        log.info("报表权限初始化完成");
    }

    private void initCheckInPermissions() {
        Long menuParentId = getOrCreatePermission("业务管理", "business", "menu", 0L, 2);
        Long checkInMenuId = getOrCreatePermission("员工打卡", "checkin", "menu", menuParentId, 8);

        getOrCreatePermission("打卡列表", "checkin:list", "button", checkInMenuId, 1);
        getOrCreatePermission("打卡", "checkin:add", "button", checkInMenuId, 2);
        getOrCreatePermission("删除打卡", "checkin:delete", "button", checkInMenuId, 3);

        log.info("打卡管理权限初始化完成");
    }

    private void initReportTemplates() {
        LambdaQueryWrapper<RpTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RpTemplate::getTemplateCode, "DEV_DAILY");
        if (rpTemplateMapper.selectCount(wrapper) > 0) {
            log.info("报表模板已存在，跳过初始化");
            return;
        }
        String[][] templates = {
            {"DEV_DAILY", "研发日报", "DEV", "DAILY", "请填写今日完成工作、进行中任务、明日计划及问题风险"},
            {"DEV_WEEKLY", "研发周报", "DEV", "WEEKLY", "请填写本周版本迭代情况、BUG闭环、任务完成率及下周计划"},
            {"DEV_MONTHLY", "研发月报", "DEV", "MONTHLY", "请填写项目进度、人均工时、BUG率及技术建设情况"},
            {"OPS_DAILY", "运维日报", "OPS", "DAILY", "请填写日常巡检、故障处理、操作记录及风险提醒"},
            {"OPS_WEEKLY", "运维周报", "OPS", "WEEKLY", "请填写巡检次数、变更发布、运维优化及现存隐患"},
            {"OPS_MONTHLY", "运维月报", "OPS", "MONTHLY", "请填写系统可用率、重大事件复盘及基础设施维护情况"},
            {"CS_DAILY", "客服日报", "CS", "DAILY", "请填写接待数据、问题分类、工单处理及典型问题"},
            {"CS_WEEKLY", "客服周报", "CS", "WEEKLY", "请填写服务数据、高频问题、投诉处理及跨部门对接"},
            {"CS_MONTHLY", "客服月报", "CS", "MONTHLY", "请填写核心KPI、客户问题分析、服务复盘及下月目标"}
        };
        for (String[] t : templates) {
            RpTemplate template = new RpTemplate();
            template.setTemplateCode(t[0]);
            template.setTemplateName(t[1]);
            template.setPostType(t[2]);
            template.setReportType(t[3]);
            template.setTemplateDesc(t[4]);
            template.setIsEnabled(1);
            rpTemplateMapper.insert(template);
        }
        log.info("9套报表模板初始化完成");
    }
}
