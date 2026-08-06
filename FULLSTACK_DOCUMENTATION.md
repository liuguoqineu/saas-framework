# SaaS 智慧燃气CRM管理系统 - 全栈技术文档

---

## 📋 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [技术栈详解](#3-技术栈详解)
4. [项目结构](#4-项目结构)
5. [数据库设计](#5-数据库设计)
6. [后端模块详解](#6-后端模块详解)
7. [前端模块详解](#7-前端模块详解)
8. [核心业务功能](#8-核心业务功能)
9. [多租户与权限系统](#9-多租户与权限系统)
10. [API接口文档](#10-api接口文档)
11. [部署指南](#11-部署指南)
12. [开发规范](#12-开发规范)

---

## 1. 项目概述

### 1.1 项目简介

**奉天数智科技有限公司 - 智慧燃气CRM管理系统**

本项目是一个基于 **SaaS多租户架构** 的智慧燃气客户关系管理系统(CRM),专为燃气行业打造,提供完整的客户管理、合同管理、报修管理、数据统计分析等核心功能。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| **🏢 多租户隔离** | 支持多家燃气公司独立使用,数据完全隔离 |
| **🔐 RBAC权限控制** | 基于角色的细粒度权限管理,支持权限继承 |
| **👥 三层账户体系** | 超级管理员 → 租户管理员 → 普通员工 |
| **📊 数据可视化** | 基于ECharts的丰富图表展示和统计分析 |
| **📄 文件管理** | 支持客户附件、合同文件上传和管理 |
| **⚠️ 提醒系统** | 合同到期、跟进提醒等智能提醒功能 |
| **🔧 工单系统** | 完整的报修工单处理流程 |
| **💾 数据备份** | 自动化数据库备份与恢复 |

### 1.3 适用场景

- 燃气公司客户信息管理
- 商业/工业用气客户关系维护
- 加气站运营管理
- 合同生命周期管理
- 设备报修与运维管理
- 业务数据分析与决策支持

---

## 2. 技术架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        前端展示层 (Presentation)                      │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐           │
│  │  Vue 3    │ │ Element   │ │  ECharts  │ │  Pinia    │           │
│  │  + Vite   │ │  Plus     │ │  图表库   │ │  状态管理  │           │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘           │
│        └─────────────┴─────────────┴─────────────┘                  │
│                              │                                       │
│                    HTTP请求 (Axios)                                  │
│                         ↓                                           │
├─────────────────────────────────────────────────────────────────────┤
│                        后端服务层 (Backend)                          │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    过滤器层 (Filter Chain)                   │   │
│  │  CorsConfig → JwtAuthFilter (Token认证 + 上下文设置)        │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                        │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                    控制器层 (Controller)                     │   │
│  │  Auth / Tenant / Role / User / Customer / Contract          │   │
│  │  Repair / Statistics / Backup / OperationLog                │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                        │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                    AOP切面层 (Aspect)                        │   │
│  │  PermissionAspect (权限校验) + OperationLogAspect (日志记录) │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                        │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                    服务层 (Service/Business Logic)           │   │
│  │  AuthService / TenantService / CustomerService              │   │
│  │  ContractService / RepairService / StatisticsService        │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                        │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │               MyBatis-Plus拦截器层                           │   │
│  │  • PaginationInnerInterceptor (分页插件)                    │   │
│  │  • TenantLineInnerInterceptor (多租户隔离)                  │   │
│  │  • MetaObjectHandler (自动填充时间字段)                     │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
│                             │                                        │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                    数据访问层 (Mapper)                       │   │
│  │  SysUserMapper / BizCustomerMapper / BizContractMapper ...  │   │
│  └──────────────────────────┬──────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────┘
                              │
┌─────────────────────────────┼───────────────────────────────────────┐
│                    数据持久层 (Data Storage)                       │
│  ┌──────────────────────────┴──────────────────────────────────┐   │
│  │                 MySQL 8.0 数据库                             │   │
│  │            数据库名: saaslearn                               │   │
│  │            字符集: utf8mb4 | 引擎: InnoDB                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 架构设计原则

| 原则 | 实现方式 |
|------|---------|
| **前后端分离** | RESTful API + Vue SPA单页应用 |
| **分层架构** | Controller → Service → Mapper 三层分离 |
| **关注点分离** | AOP切面处理横切关注点(权限、日志) |
| **数据隔离** | 多租户插件自动追加租户条件 |
| **统一响应** | Result封装类统一返回格式 |
| **全局异常** | GlobalExceptionHandler统一异常处理 |

---

## 3. 技术栈详解

### 3.1 后端技术栈

#### 核心框架

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Java** | 17 | 编程语言 | LTS版本,性能优秀 |
| **Spring Boot** | 2.7.18 | 应用框架 | 快速开发,自动配置 |
| **MyBatis-Plus** | 3.5.5 | ORM框架 | 增强MyBatis,简化CRUD操作 |
| **MySQL** | 8.0+ | 关系型数据库 | 主流开源数据库 |

#### 安全与认证

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **jjwt** | 0.9.1 | JWT令牌 | 无状态身份认证 |
| **spring-security-crypto** | 5.7.x | 密码加密 | BCrypt算法加密存储 |

#### API文档与工具

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **springdoc-openapi-ui** | 1.7.0 | API文档 | Swagger UI界面 |
| **Apache POI** | 5.2.5 | Excel处理 | 导入导出功能 |

#### 开发工具库

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Lombok** | 1.18.30 | 代码简化 | 自动生成getter/setter等 |
| **HikariCP** | 4.0.3 | 连接池 | 高性能数据库连接池 |
| **Spring Validation** | 2.7.x | 参数校验 | JSR303注解校验 |
| **Spring AOP** | 2.7.x | 面向切面编程 | 权限校验、操作日志 |

### 3.2 前端技术栈

#### 核心框架

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Vue.js** | ^3.4.0 | 前端框架 | Composition API,响应式系统 |
| **Vite** | ^5.4.0 | 构建工具 | 快速开发服务器,HMR热更新 |
| **Vue Router** | ^4.3.0 | 路由管理 | 单页应用路由,路由守卫 |
| **Pinia** | ^2.1.0 | 状态管理 | Vue官方推荐状态管理库 |

#### UI组件与可视化

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Element Plus** | ^2.6.0 | UI组件库 | 企业级Vue 3组件库 |
| **@element-plus/icons-vue** | ^2.3.0 | 图标库 | Element Plus官方图标 |
| **ECharts** | ^6.0.0 | 图表库 | 数据可视化,统计报表 |
| **element-china-area-data** | ^5.0.2 | 地区数据 | 中国省市区三级联动 |

#### 网络通信

| 技术 | 版本 | 用途 | 说明 |
|------|------|------|------|
| **Axios** | ^1.6.0 | HTTP客户端 | Promise-based HTTP请求 |

### 3.3 技术选型理由

| 选型 | 理由 |
|------|------|
| **Spring Boot 2.7.x** | 成熟稳定,生态完善,企业级首选 |
| **MyBatis-Plus** | 内置多租户插件,分页插件,极大提升开发效率 |
| **Vue 3 + Vite** | 开发体验优秀,构建速度快,生态活跃 |
| **Element Plus** | 组件丰富,文档清晰,适合后台管理系统 |
| **JWT无状态认证** | 适合分布式部署,易于扩展 |
| **MySQL + InnoDB** | 支持事务,行级锁,性能优异 |

---

## 4. 项目结构

### 4.1 完整目录树

```
saas-framework/
│
├── backend/                                    # Spring Boot后端
│   ├── pom.xml                                 # Maven依赖配置
│   └── src/
│       ├── main/
│       │   ├── java/com/saas/framework/
│       │   │   ├── SaasFrameworkApplication.java    # 启动入口
│       │   │   │
│       │   │   ├── common/                          # 公共模块
│       │   │   │   ├── Result.java                  # 统一响应封装
│       │   │   │   ├── annotation/                  # 自定义注解
│       │   │   │   │   ├── OperationLog.java        # 操作日志注解
│       │   │   │   │   └── RequirePermission.java   # 权限校验注解
│       │   │   │   ├── aspect/                      # AOP切面
│       │   │   │   │   ├── OperationLogAspect.java  # 操作日志切面
│       │   │   │   │   └── PermissionAspect.java    # 权限校验切面
│       │   │   │   ├── context/                     # 上下文(ThreadLocal)
│       │   │   │   │   ├── TenantContext.java        # 租户上下文
│       │   │   │   │   └── UserContext.java          # 用户上下文
│       │   │   │   ├── dto/                          # 数据传输对象
│       │   │   │   │   ├── LoginRequest.java         # 登录请求
│       │   │   │   │   ├── PageResult.java           # 分页结果
│       │   │   │   │   ├── ContractRequest.java      # 合同请求DTO
│       │   │   │   │   ├── CustomerRequest.java      # 客户请求DTO
│       │   │   │   │   ├── RepairOrderRequest.java   # 报修工单DTO
│       │   │   │   │   ├── UserCreateRequest.java    # 用户创建DTO
│       │   │   │   │   └── ...                       # 其他DTO
│       │   │   │   ├── exception/                    # 异常处理
│       │   │   │   │   ├── BusinessException.java    # 业务异常
│       │   │   │   │   └── GlobalExceptionHandler.java # 全局异常处理器
│       │   │   │   └── util/                         # 工具类
│       │   │   │       └── JwtUtil.java              # JWT工具类
│       │   │   │
│       │   │   ├── config/                          # 配置类
│       │   │   │   ├── CorsConfig.java              # 跨域配置
│       │   │   │   ├── DataInitializer.java         # 初始数据初始化
│       │   │   │   ├── FilterConfig.java            # 过滤器注册
│       │   │   │   ├── FilePathConfig.java          # 文件路径配置
│       │   │   │   ├── JwtAuthFilter.java           # JWT认证过滤器
│       │   │   │   ├── MyBatisPlusConfig.java       # MyBatis-Plus配置
│       │   │   │   ├── SecurityConfig.java          # 安全配置(BCrypt)
│       │   │   │   ├── StringToLocalDateConverter.java  # 日期转换器
│       │   │   │   ├── StringToLocalDateTimeConverter.java
│       │   │   │   ├── WebMvcConfig.java            # Web MVC配置
│       │   │   │   └── BackupScheduleTask.java      # 定时备份任务
│       │   │   │
│       │   │   ├── controller/                      # 控制器层
│       │   │   │   ├── AuthController.java          # 认证控制器
│       │   │   │   ├── BackupController.java        # 备份控制器
│       │   │   │   ├── ContractController.java      # 合同控制器
│       │   │   │   ├── CustomerController.java      # 客户控制器
│       │   │   │   ├── FollowUpController.java      # 跟进记录控制器
│       │   │   │   ├── OperationLogController.java  # 操作日志控制器
│       │   │   │   ├── PermissionController.java    # 权限控制器
│       │   │   │   ├── ReminderController.java      # 提醒设置控制器
│       │   │   │   ├── RepairController.java        # 报修控制器
│       │   │   │   ├── RoleController.java          # 角色控制器
│       │   │   │   ├── StatisticsController.java    # 统计分析控制器
│       │   │   │   ├── TenantController.java        # 租户控制器
│       │   │   │   └── UserController.java          # 用户控制器
│       │   │   │
│       │   │   ├── entity/                          # 实体类(数据库表映射)
│       │   │   │   ├── biz/                         # 业务实体
│       │   │   │   │   ├── BizContract.java         # 合同实体
│       │   │   │   │   ├── BizContractAttachment.java
│       │   │   │   │   ├── BizContractModifyLog.java
│       │   │   │   │   ├── BizContractReminder.java
│       │   │   │   │   ├── BizCustomer.java         # 客户实体
│       │   │   │   │   ├── BizCustomerAttachment.java
│       │   │   │   │   ├── BizCustomerModifyLog.java
│       │   │   │   │   ├── BizCustomerStatusLog.java
│       │   │   │   │   ├── BizFollowUpRecord.java   # 跟进记录实体
│       │   │   │   │   ├── BizRepairAttachment.java
│       │   │   │   │   ├── BizRepairOrder.java      # 报修工单实体
│       │   │   │   │   └── BizRepairProcessLog.java
│       │   │   │   └── sys/                         # 系统实体
│       │   │   │       ├── SysBackupRecord.java     # 备份记录
│       │   │   │       ├── SysOperationLog.java     # 操作日志
│       │   │   │       ├── SysPermission.java       # 权限实体
│       │   │   │       ├── SysRole.java             # 角色实体
│       │   │   │       ├── SysRolePermission.java
│       │   │   │       ├── SysTenant.java           # 租户实体
│       │   │   │       └── SysUser.java             # 用户实体
│       │   │   │
│       │   │   ├── mapper/                          # 数据访问层(Mapper接口)
│       │   │   │   ├── BizContractMapper.java
│       │   │   │   ├── BizContractAttachmentMapper.java
│       │   │   │   ├── BizContractMapper.java
│       │   │   │   ├── BizContractModifyLogMapper.java
│       │   │   │   ├── BizContractReminderMapper.java
│       │   │   │   ├── BizCustomerMapper.java
│       │   │   │   ├── BizCustomerAttachmentMapper.java
│       │   │   │   ├── BizCustomerModifyLogMapper.java
│       │   │   │   ├── BizCustomerStatusLogMapper.java
│       │   │   │   ├── BizFollowUpRecordMapper.java
│       │   │   │   ├── BizRepairAttachmentMapper.java
│       │   │   │   ├── BizRepairOrderMapper.java
│       │   │   │   ├── BizRepairProcessLogMapper.java
│       │   │   │   ├── SysBackupRecordMapper.java
│       │   │   │   ├── SysOperationLogMapper.java
│       │   │   │   ├── SysPermissionMapper.java
│       │   │   │   ├── SysRoleMapper.java
│       │   │   │   ├── SysRolePermissionMapper.java
│       │   │   │   ├── SysTenantMapper.java
│       │   │   │   └── SysUserMapper.java
│       │   │   │
│       │   │   └── service/                         # 服务层
│       │   │       ├── AuthService.java             # 认证服务接口
│       │   │       ├── BackupService.java           # 备份服务接口
│       │   │       ├── ContractService.java         # 合同服务接口
│       │   │       ├── CustomerService.java         # 客户服务接口
│       │   │       ├── FollowUpService.java         # 跟进服务接口
│       │   │       ├── OperationLogService.java     # 日志服务接口
│       │   │       ├── PermissionService.java       # 权限服务接口
│       │   │       ├── ReminderService.java         # 提醒服务接口
│       │   │       ├── RepairService.java           # 报修服务接口
│       │   │       ├── RoleService.java             # 角色服务接口
│       │   │       ├── StatisticsService.java       # 统计服务接口
│       │   │       ├── TenantService.java           # 租户服务接口
│       │   │       ├── UserService.java             # 用户服务接口
│       │   │       └── impl/                        # 服务实现类
│       │   │           ├── AuthServiceImpl.java
│       │   │           ├── BackupServiceImpl.java
│       │   │           ├── ContractServiceImpl.java
│       │   │           ├── CustomerServiceImpl.java
│       │   │           ├── FollowUpServiceImpl.java
│       │   │           ├── OperationLogServiceImpl.java
│       │   │           ├── PermissionServiceImpl.java
│       │   │           ├── ReminderServiceImpl.java
│       │   │           ├── RepairServiceImpl.java
│       │   │           ├── RoleServiceImpl.java
│       │   │           ├── StatisticsServiceImpl.java
│       │   │           ├── TenantServiceImpl.java
│       │   │           └── UserServiceImpl.java
│       │   │
│       │   └── resources/
│       │       └── application.yml                 # 应用配置文件
│       │
│       └── test/                                   # 测试代码
│           └── java/com/saas/framework/
│               ├── RoleServiceTest.java
│               └── UserServiceTest.java
│
├── frontend/                                   # Vue 3前端
│   ├── package.json                            # NPM依赖配置
│   ├── vite.config.js                          # Vite构建配置
│   ├── index.html                              # HTML入口
│   ├── .eslintrc.cjs                           # ESLint代码检查
│   ├── .prettierrc                             # Prettier格式化
│   └── src/
│       ├── main.js                             # 应用入口
│       ├── App.vue                             # 根组件
│       │
│       ├── api/                                # API接口封装
│       │   ├── auth.js                         # 认证API
│       │   ├── backup.js                       # 备份API
│       │   ├── contract.js                     # 合同API
│       │   ├── customer.js                     # 客户API
│       │   ├── followUp.js                     # 跟进API
│       │   ├── operationLog.js                 # 操作日志API
│       │   ├── reminder.js                     # 提醒API
│       │   ├── repair.js                       # 报修API
│       │   ├── role.js                         # 角色API
│       │   ├── statistics.js                   # 统计API
│       │   ├── tenant.js                       # 租户API
│       │   └── user.js                         # 用户API
│       │
│       ├── components/                         # 公共组件
│       │   └── LoginReminderDialog.vue         # 登录提醒弹窗
│       │
│       ├── directives/                         # 自定义指令
│       │   └── permission.js                   # v-permission权限指令
│       │
│       ├── router/                             # 路由配置
│       │   └── index.js                        # 路由定义+守卫
│       │
│       ├── store/                              # Pinia状态管理
│       │   └── user.js                         # 用户状态store
│       │
│       ├── utils/                              # 工具函数
│       │   └── request.js                      # Axios封装
│       │
│       └── views/                              # 页面视图组件
│           ├── Dashboard.vue                   # 首页仪表盘
│           ├── Login.vue                       # 登录页面
│           ├── Layout.vue                      # 主布局(侧边栏+顶栏)
│           ├── Tenant.vue                      # 租户管理
│           ├── Role.vue                        # 角色管理
│           ├── User.vue                        # 用户/员工管理
│           ├── Customer.vue                    # 客户列表
│           ├── CustomerDetail.vue              # 客户详情
│           ├── Contract.vue                    # 合同管理
│           ├── Repair.vue                      # 报修管理
│           ├── Statistics.vue                  # 统计分析
│           ├── OperationLog.vue                # 操作日志
│           └── Backup.vue                      # 数据备份
│
├── docs/                                      # 数据库脚本
│   ├── init.sql                               # 初始化脚本(建表+初始数据)
│   ├── migration_add_backup.sql               # 备份功能迁移
│   ├── migration_add_contract.sql             # 合同功能迁移
│   ├── migration_add_contract_expire_date.sql # 合同到期日迁移
│   ├── migration_add_detail_address.sql       # 详细地址迁移
│   ├── migration_add_follow_up_person.sql     # 跟进人迁移
│   ├── migration_add_maintenance_category.sql # 维修分类迁移
│   ├── migration_add_operation_log.sql        # 操作日志迁移
│   └── migration_add_repair.sql               # 报修功能迁移
│
├── CODE_WIKI.md                               # 技术Wiki文档
├── QUICKSTART.md                              # 快速启动指南
├── TUTORIAL.md                                # 开发教程
└── README.md                                  # 项目说明
```

### 4.2 分层职责说明

```
┌─────────────────────────────────────────────────────────────────┐
│  Controller层 (控制器)                                          │
│  • 接收HTTP请求,参数校验(@Valid)                                │
│  • 调用Service层处理业务逻辑                                     │
│  • 返回统一响应Result<T>                                         │
│  • 使用@RequirePermission标注权限要求                            │
├─────────────────────────────────────────────────────────────────┤
│  Service层 (服务)                                               │
│  • 实现核心业务逻辑                                              │
│  • 事务管理(@Transactional)                                     │
│  • 多租户数据过滤(wrapper.eq("tenant_id", tenantId))            │
│  • 权限范围校验                                                  │
├─────────────────────────────────────────────────────────────────┤
│  Mapper层 (数据访问)                                            │
│  • 继承BaseMapper<T>,获得基础CRUD                                │
│  • 自定义复杂查询方法                                            │
│  • MyBatis-Plus自动处理SQL                                      │
├─────────────────────────────────────────────────────────────────┤
│  Entity层 (实体)                                                │
│  • @TableName映射数据库表名                                      │
│  • @TableId主键策略,@TableField字段映射                          │
│  • @TableLogic逻辑删除标记                                       │
│  • @TableField(fill = FieldFill.INSERT/UPDATE)自动填充           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 数据库设计

### 5.1 数据库概览

- **数据库名**: `saaslearn`
- **字符集**: `utf8mb4` (支持emoji和特殊字符)
- **排序规则**: `utf8mb4_general_ci`
- **存储引擎**: `InnoDB` (支持事务和外键)

### 5.2 ER关系图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        系统表 (sys_)                                │
│                                                                     │
│  sys_tenant (租户)                                                  │
│       │                                                             │
│       │ 1:N                                                         │
│       ▼                                                             │
│  sys_user (用户) ◄── sys_role (角色) ──┬── sys_role_permission ───► │
│       │              ▲    │              │       (角色权限关联)      │
│       │              │    │ N:M           │                         │
│       │              │    ▼              │                         │
│       │         sys_permission (权限) ◄──┘                         │
│       │                                                                 │
│       ├───────────────────────────────────────────────────────────┐  │
│       │                    业务表 (biz_)                           │  │
│       │                                                           │  │
│       │  biz_customer (客户)                                       │  │
│       │       │                                                   │  │
│       │       ├── biz_customer_attachment (客户附件)               │  │
│       │       ├── biz_customer_modify_log (客户修改日志)           │  │
│       │       └── biz_customer_status_log (客户状态变更日志)       │  │
│       │                                                           │  │
│       │  biz_follow_up_record (跟进记录)                           │  │
│       │                                                           │  │
│       │  biz_contract (合同)                                       │  │
│       │       │                                                   │  │
│       │       ├── biz_contract_attachment (合同附件)               │  │
│       │       ├── biz_contract_modify_log (合同修改日志)           │  │
│       │       └── biz_contractreminder (合同提醒设置)              │  │
│       │                                                           │  │
│       │  biz_repair_order (报修工单)                               │  │
│       │       │                                                   │  │
│       │       ├── biz_repair_attachment (报修附件)                 │  │
│       │       └── biz_repair_process_log (处理流程日志)            │  │
│       │                                                           │  │
│       └───────────────────────────────────────────────────────────┘  │
│                                                                     │
│  sys_operation_log (操作日志 - 全局)                                │
│  sys_backup_record (备份记录 - 全局)                                │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.3 系统表详细设计

#### 5.3.1 sys_tenant — 租户信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID(即租户ID) |
| name | VARCHAR(100) | NOT NULL | 租户名称(公司名称) |
| code | VARCHAR(50) | NOT NULL, UNIQUE | 租户编码(唯一标识) |
| status | TINYINT(1) | DEFAULT 1 | 状态: 1-启用, 0-禁用 |
| admin_user_id | BIGINT | NULL | 关联的管理员用户ID |
| admin_password | VARCHAR(50) | NULL | 管理员初始密码(明文) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

> **用途**: 存储燃气公司(租户)基本信息,超级管理员通过此表管理所有租户

#### 5.3.2 sys_user — 系统用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名(登录账号) |
| password | VARCHAR(200) | NOT NULL | 密码(BCrypt加密) |
| role_id | BIGINT | FK | 关联角色ID |
| tenant_id | BIGINT | NOT NULL, DEFAULT 0 | 租户ID(0=超级账户) |
| real_name | VARCHAR(50) | NULL | 真实姓名 |
| status | TINYINT(1) | DEFAULT 1 | 状态: 1-启用, 0-禁用 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT(1) | DEFAULT 0 | 逻辑删除: 0-正常, 1-已删除 |

> **三层账户体系**:
> - `tenant_id = 0`: 超级管理员(平台级)
> - `tenant_id > 0, role_id = 租户管理员角色`: 租户管理员
> - `tenant_id > 0, role_id = 普通角色`: 租户普通员工

#### 5.3.3 sys_role — 系统角色表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| name | VARCHAR(50) | NOT NULL | 角色名称 |
| tenant_id | BIGINT | NOT NULL, DEFAULT 0 | 租户ID(0=平台角色) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT(1) | DEFAULT 0 | 逻辑删除标记 |

#### 5.3.4 sys_permission — 权限表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| name | VARCHAR(50) | NOT NULL | 权限名称 |
| code | VARCHAR(100) | NOT NULL, UNIQUE | 权限编码(如 `customer:list`) |
| type | VARCHAR(20) | DEFAULT 'menu' | 类型: menu-菜单, button-按钮 |
| parent_id | BIGINT | DEFAULT 0 | 父权限ID(0=根节点) |
| sort | INT | DEFAULT 0 | 排序号 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

> **权限树结构示例**:
>
> ```
> 系统管理(system)
>   ├── 租户管理(tenant)
>   │     ├── tenant:list (列表)
>   │     ├── tenant:add (新增)
>   │     └── tenant:edit (编辑)
>   ├── 角色管理(role)
>   │     └── ...
>   └── 用户管理(user)
>         └── ...
> 业务管理(business)
>   ├── 客户管理(customer)
>   │     ├── customer:list
>   │     ├── customer:add
>   │     └── customer:edit
>   ├── 合同管理(contract)
>   │     └── ...
>   └── 报修管理(repair)
>         └── ...
> ```

#### 5.3.5 sys_role_permission — 角色-权限关联表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| role_id | BIGINT | PK, FK | 角色ID |
| permission_id | BIGINT | PK, FK | 权限ID |

> **联合主键**: `(role_id, permission_id)`,实现多对多关联

### 5.4 业务表详细设计

#### 5.4.1 biz_customer — 客户信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| name | VARCHAR(100) | NOT NULL | 客户名称(企业/个人名称) |
| address | VARCHAR(255) | NULL | 客户地址 |
| detail_address | VARCHAR(500) | NULL | 详细地址 |
| region | VARCHAR(50) | NULL | 所属区域(省市区) |
| contact_person | VARCHAR(50) | NULL | 联系人姓名 |
| contact_phone | VARCHAR(20) | NULL | 联系电话 |
| business_category | VARCHAR(50) | NULL | 业务类型一级分类 |
| business_type | VARCHAR(50) | NULL | 业务类型二级分类 |
| cooperation_category | VARCHAR(50) | DEFAULT '潜在客户' | 合作状态一级分类 |
| cooperation_status | VARCHAR(50) | DEFAULT '中潜力' | 合作状态二级分类 |
| gas_scale | VARCHAR(50) | NULL | 用气规模(大型/中型/小型) |
| smart_gas_system | VARCHAR(255) | NULL | 智慧燃气系统型号 |
| contract_info | VARCHAR(500) | NULL | 合同信息摘要 |
| is_invalid | TINYINT(1) | DEFAULT 0 | 是否无效客户 |
| follow_up_person_id | BIGINT | NULL | 当前跟进人ID |
| follow_up_person | VARCHAR(50) | NULL | 当前跟进人姓名 |
| tenant_id | BIGINT | NOT NULL | 租户ID(数据隔离) |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT(1) | DEFAULT 0 | 逻辑删除标记 |

**业务分类说明**:

| 分类维度 | 可选值 | 说明 |
|---------|--------|------|
| **business_category** | 加气站类/商业用气/工业用气 | 一级业务类型 |
| **business_type** | CNG加气站/LPG加气站/餐饮类/团餐类/其他商业类/大型/中型/小型 | 二级细分类型 |
| **cooperation_category** | 已合作/潜在/意向 | 合作阶段 |
| **cooperation_status** | 正常履约/逾期客户/暂停合作/终止合作/高潜力/中潜力/低潜力/意向跟进 | 具体状态 |

#### 5.4.2 biz_contract — 合同信息表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| customer_id | BIGINT | NOT NULL, FK | 关联客户ID |
| contract_no | VARCHAR(100) | NOT NULL | 合同编号(唯一) |
| contract_name | VARCHAR(200) | NOT NULL | 合同名称 |
| sign_date | DATE | NULL | 签订日期 |
| start_date | DATE | NULL | 生效日期 |
| expire_date | DATE | NULL | 到期日期(用于提醒) |
| amount | DECIMAL(12,2) | NULL | 合同金额 |
| status | VARCHAR(20) | DEFAULT 'draft' | 状态: draft草稿/effective生效/expired过期/terminated终止 |
| content | TEXT | NULL | 合同内容/备注 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT(1) | DEFAULT 0 | 逻辑删除标记 |

#### 5.4.3 biz_repair_order — 报修工单表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| customer_id | BIGINT | NOT NULL, FK | 关联客户ID |
| title | VARCHAR(200) | NOT NULL | 工单标题 |
| description | TEXT | NULL | 问题描述 |
| maintenance_category | VARCHAR(50) | NULL | 维修分类 |
| status | VARCHAR(20) | DEFAULT 'pending' | 工单状态 |
| priority | VARCHAR(20) | DEFAULT 'medium' | 优先级: low/medium/high/urgent |
| reporter_name | VARCHAR(50) | NULL | 报修人姓名 |
| reporter_phone | VARCHAR(20) | NULL | 报修人电话 |
| assignee_id | BIGINT | NULL | 处理人ID |
| assignee_name | VARCHAR(50) | NULL | 处理人姓名 |
| resolved_at | DATETIME | NULL | 解决时间 |
| resolution | TEXT | NULL | 解决方案/备注 |
| tenant_id | BIGINT | NOT NULL | 租户ID |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT(1) | DEFAULT 0 | 逻辑删除标记 |

**工单状态流转**:
```
pending(待处理) → assigned(已派单) → in_progress(处理中) → resolved(已解决) → closed(已关闭)
                                                              ↘ exception(异常)
```

#### 5.4.4 其他重要业务表

| 表名 | 说明 | 核心字段 |
|------|------|---------|
| **biz_customer_attachment** | 客户附件 | customer_id, file_name, file_path, file_type |
| **biz_customer_modify_log** | 客户修改日志 | customer_id, field_name, old_value, new_value |
| **biz_customer_status_log** | 客户状态变更日志 | customer_id, old_status, new_status |
| **biz_follow_up_record** | 跟进记录 | customer_id, follow_up_type, content, next_time |
| **biz_contract_attachment** | 合同附件 | contract_id, file_name, file_path |
| **biz_contract_modify_log** | 合同修改日志 | contract_id, field_name, old_value, new_value |
| **biz_contract_reminder** | 合同提醒设置 | contract_id, remind_days_before, is_enabled |
| **biz_repair_attachment** | 报修附件 | repair_order_id, file_name, file_path |
| **biz_repair_process_log** | 工单处理日志 | repair_order_id, action, operator, remark |
| **sys_operation_log** | 操作日志 | user_id, module, action, detail, ip_address |
| **sys_backup_record** | 备份记录 | file_name, file_path, file_size, backup_time |

### 5.5 多租户隔离策略

| 表类型 | 表名前缀 | 是否自动隔离 | 说明 |
|--------|---------|:------------:|------|
| 全局共享表 | sys_ | ❌ 不隔离 | permission, role_permission, tenant, operation_log, backup_record |
| 手动控制表 | sys_user/sys_role | ⚠️ Service层控制 | 登录时需跨租户查询,超级账户需查看全部 |
| 业务隔离表 | biz_ | ✅ 自动隔离 | 所有业务数据按tenant_id严格隔离 |

> **技术实现**: MyBatis-Plus `TenantLineInnerInterceptor` 自动在SQL中追加 `WHERE tenant_id = ?` 条件

---

## 6. 后端模块详解

### 6.1 公共模块 (common)

#### 6.1.1 Result — 统一响应封装

```java
public class Result<T> {
    private int code;      // 状态码: 200成功, 400业务错误, 401未认证, 403无权, 404不存在, 500服务器错误
    private String message; // 提示消息
    private T data;        // 返回数据
}
```

**使用示例**:
```java
// 成功响应
return Result.ok(data);
return Result.ok("操作成功", data);

// 错误响应
return Result.error("参数错误");
return Result.error(403, "权限不足");
return Result.unauth();
return Result.forbidden();
```

#### 6.1.2 自定义注解

**@RequirePermission — 权限校验注解**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value(); // 权限编码,如 "customer:add"
}

// 使用方式
@RequirePermission("customer:add")
@PostMapping
public Result<Void> create(@Valid @RequestBody CustomerRequest request) { ... }
```

**@OperationLog — 操作日志注解**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();  // 操作模块: "客户管理"
    String action();  // 操作动作: "新增客户"
}
```

#### 6.1.3 ThreadLocal上下文

**TenantContext — 租户上下文**
```java
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) { ... }
    public static Long getTenantId() { ... }
    public static void remove() { ... } // 必须调用,防止内存泄漏
}
```

**UserContext — 用户上下文**
```java
public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> PERMISSIONS = new ThreadLocal<>();

    // getter/setter 方法...
    public static boolean isSuperAdmin() {
        return getTenantId() != null && getTenantId() == 0;
    }
    public static void remove() { ... } // 清除所有ThreadLocal
}
```

#### 6.1.4 JwtUtil — JWT工具类

| 方法 | 功能 |
|------|------|
| `generateToken(userId, username, tenantId)` | 生成JWT Token |
| `validateToken(token)` | 验证Token有效性 |
| `getUserIdFromToken(token)` | 从Token提取用户ID |
| `getUsernameFromToken(token)` | 从Token提取用户名 |
| `getTenantIdFromToken(token)` | 从Token提取租户ID |

**Token配置**:
- 签名算法: HS512
- 有效期: 2小时 (7200000毫秒)
- Payload: `{ userId, username, tenantId }`

#### 6.1.5 异常处理体系

```java
// 业务异常(主动抛出)
throw new BusinessException("客户不存在");
throw new BusinessException(403, "无权操作");

// 全局异常处理器捕获
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(...) {
        // 提取校验错误信息返回
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.serverError();
    }
}
```

### 6.2 配置模块 (config)

#### 6.2.1 JwtAuthFilter — JWT认证过滤器(核心)

**拦截路径**: `/api/**`
**排除路径**(无需认证):
- `/api/auth/login` (登录接口)
- `/swagger-ui/**`, `/v3/api-docs/**` (Swagger文档)

**执行流程**:
```
1. 判断是否排除路径 → 是则放行
2. 从请求头获取 Authorization: Bearer <token>
3. 调用 JwtUtil.validateToken() 验证Token
4. 解析Token获取 userId, tenantId, username
5. 查询SysUser确认用户存在且状态正常
6. 设置 TenantContext.setTenantId(tenantId)
7. 设置 UserContext (userId, username, tenantId, permissions)
8. chain.doFilter() 放行到Controller
9. finally块中清除所有ThreadLocal (防止内存泄漏!)
```

#### 6.2.2 MyBatisPlusConfig — MyBatis-Plus配置(核心)

**配置三大拦截器**:

1️⃣ **PaginationInnerInterceptor (分页插件)**
- 数据库类型: MySQL
- 支持 `Page<T>` 分页查询

2️⃣ **TenantLineInnerInterceptor (多租户插件)** ⭐
- 租户字段: `tenant_id`
- 数据源: `TenantContext.getTenantId()`
- **忽略表列表**(不自动追加条件):
  ```java
  ignoreTables = Arrays.asList(
      "sys_permission",        // 全局共享
      "sys_role_permission",   // 全局共享
      "sys_tenant",            // 全局共享
      "sys_user",              // 登录需跨租户查询
      "sys_role",              // 超级账户查看全部
      "biz_customer",          // Service层手动控制
      "biz_contract",
      "biz_repair_order",
      "biz_follow_up_record"
      // ... 所有biz_表
  );
  ```

3️⃣ **MetaObjectHandler (自动填充)**
- INSERT时: 自动填充 `create_time`, `update_time`
- UPDATE时: 自动填充 `update_time`

#### 6.2.3 DataInitializer — 初始数据初始化

应用启动时自动执行(CommandLineRunner):

1. **创建超级角色**(id=1, name="超级管理员", tenant_id=0)
2. **分配所有权限**给超级角色
3. **创建超级账户**(username="admin", password="123456" BCrypt加密)
4. **幂等保证**: 仅在数据不存在时插入

#### 6.2.4 BackupScheduleTask — 定时备份任务

使用 `@Scheduled` 注解定时执行数据库备份:
- 备份路径: 配置文件中的 `file.backup-path`
- 备份文件命名: `backup_yyyyMMdd_HHmmss.sql`
- 记录备份信息到 `sys_backup_record` 表

#### 6.2.5 其他配置类

| 配置类 | 功能 |
|--------|------|
| **CorsConfig** | 跨域配置(允许所有来源,携带凭证) |
| **SecurityConfig** | 提供BCryptPasswordEncoder Bean |
| **FilePathConfig** | 文件上传/备份路径配置 |
| **WebMvcConfig** | 注册日期类型转换器(String→LocalDate/DateTime) |
| **FilterConfig** | 注册JwtAuthFilter(Order=1) |

### 6.3 服务层核心逻辑

#### 6.3.1 AuthServiceImpl — 认证服务

```java
@PostMapping("/login")
public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
    // 1. 根据username查询用户(不区分租户)
    SysUser user = userMapper.selectByUsername(request.getUsername());

    // 2. 校验密码(BCrypt)
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new BusinessException("密码错误");
    }

    // 3. 检查用户状态
    if (user.getStatus() == 0) {
        throw new BusinessException("账户已被禁用");
    }

    // 4. 检查租户状态(非超级账户)
    if (user.getTenantId() != 0) {
        SysTenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null || tenant.getStatus() == 0) {
            throw new BusinessException("租户已被禁用");
        }
    }

    // 5. 生成JWT Token
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTenantId());

    // 6. 查询用户权限列表
    List<String> permissions = getPermissionCodes(user.getRoleId());

    // 7. 返回token + userInfo
    Map<String, Object> result = new HashMap<>();
    result.put("token", token);
    result.put("userInfo", buildUserInfo(user, permissions));
    return Result.ok(result);
}
```

**登录返回结构**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "超级管理员",
      "roleId": 1,
      "roleName": "超级管理员",
      "tenantId": 0,
      "permissions": ["system", "tenant", "tenant:list", "tenant:add", ...]
    }
  }
}
```

#### 6.3.2 CustomerServiceImpl — 客户服务(示例)

```java
@Override
public PageResult<BizCustomer> page(int current, int size, String keyword) {
    // 1. 构建分页对象
    Page<BizCustomer> page = new Page<>(current, size);

    // 2. 构建查询条件
    LambdaQueryWrapper<BizCustomer> wrapper = new LambdaQueryWrapper<>();

    // 3. 多租户数据隔离(手动控制,因为biz_customer在忽略表中)
    if (!UserContext.isSuperAdmin()) {
        wrapper.eq(BizCustomer::getTenantId, UserContext.getTenantId());
    }

    // 4. 关键词搜索(模糊匹配name或contact_person)
    if (StringUtils.hasText(keyword)) {
        wrapper.and(w -> w.like(BizCustomer::getName, keyword)
                         .or()
                         .like(BizCustomer::getContactPerson, keyword));
    }

    // 5. 按创建时间倒序
    wrapper.orderByDesc(BizCustomer::getCreateTime);

    // 6. 执行分页查询
    Page<BizCustomer> result = customerMapper.selectPage(page, wrapper);

    // 7. 封装返回
    return new PageResult<>(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
}
```

#### 6.3.3 TenantServiceImpl — 租户服务(创建租户流程)

```java
@Override
@Transactional
public Map<String, Object> create(TenantCreateRequest request) {
    // 1. 检查租户编码唯一性
    if (tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getCode, request.getCode())) > 0) {
        throw new BusinessException("租户编码已存在");
    }

    // 2. 保存租户信息
    SysTenant tenant = new SysTenant();
    tenant.setName(request.getName());
    tenant.setCode(request.getCode());
    tenant.setStatus(1);
    tenantMapper.insert(tenant);

    // 3. 生成管理员账号
    String adminUsername = request.getCode() + "admin";
    String adminPassword = StringUtils.hasText(request.getAdminPassword())
            ? request.getAdminPassword()
            : generateRandomPassword(6); // 默认随机6位密码

    // 4. 创建租户管理员角色
    SysRole adminRole = new SysRole();
    adminRole.setName("管理员");
    adminRole.setTenantId(tenant.getId());
    roleMapper.insert(adminRole);

    // 5. 创建管理员用户
    SysUser adminUser = new SysUser();
    adminUser.setUsername(adminUsername);
    adminUser.setPassword(passwordEncoder.encode(adminPassword));
    adminUser.setRoleId(adminRole.getId());
    adminUser.setTenantId(tenant.getId());
    adminUser.setRealName("管理员");
    userMapper.insert(adminUser);

    // 6. 更新租户的管理员信息
    tenant.setAdminUserId(adminUser.getId());
    tenant.setAdminPassword(adminPassword); // 明文保存,用于显示给超级管理员
    tenantMapper.updateById(tenant);

    // 7. 返回租户信息和管理员账号密码
    Map<String, Object> result = new HashMap<>();
    result.put("tenantId", tenant.getId());
    result.put("adminUsername", adminUsername);
    result.put("adminPassword", adminPassword);
    return result;
}
```

### 6.4 Controller层示例

```java
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 分页查询客户列表
     */
    @GetMapping("/page")
    @RequirePermission("customer:list")
    public Result<PageResult<BizCustomer>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(customerService.page(current, size, keyword));
    }

    /**
     * 新增客户
     */
    @PostMapping
    @RequirePermission("customer:add")
    @OperationLog(module = "客户管理", action = "新增客户")
    public Result<Void> create(@Valid @RequestBody CustomerRequest request) {
        customerService.create(request);
        return Result.ok();
    }

    /**
     * 修改客户
     */
    @PutMapping("/{id}")
    @RequirePermission("customer:edit")
    @OperationLog(module = "客户管理", action = "修改客户")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CustomerRequest request) {
        customerService.update(id, request);
        return Result.ok();
    }

    /**
     * 删除客户(逻辑删除)
     */
    @DeleteMapping("/{id}")
    @RequirePermission("customer:delete")
    @OperationLog(module = "客户管理", action = "删除客户")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
```

---

## 7. 前端模块详解

### 7.1 项目配置

#### vite.config.js — Vite配置

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,                    // 开发服务器端口
    proxy: {
      '/api': {                    // API代理
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')  // 路径别名
    }
  }
})
```

### 7.2 HTTP请求封装 (utils/request.js)

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',           // 基础路径(会被Vite代理)
  timeout: 15000            // 超时时间15秒
})

// 请求拦截器: 自动附加Token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器: 统一错误处理
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // Token过期或无效 → 跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data  // 直接返回data部分
  },
  error => {
    ElMessage.error('网络异常,请稍后重试')
    return Promise.reject(error)
  }
)

export default request
```

### 7.3 API模块示例 (api/customer.js)

```javascript
import request from '@/utils/request'

// 分页查询客户
export function getPage(params) {
  return request({
    url: '/customer/page',
    method: 'get',
    params
  })
}

// 新增客户
export function create(data) {
  return request({
    url: '/customer',
    method: 'post',
    data
  })
}

// 修改客户
export function update(id, data) {
  return request({
    url: `/customer/${id}`,
    method: 'put',
    data
  })
}

// 删除客户
export function deleteCustomer(id) {
  return request({
    url: `/customer/${id}`,
    method: 'delete'
  })
}

// 获取客户详情
export function getById(id) {
  return request({
    url: `/customer/${id}`,
    method: 'get'
  })
}
```

### 7.4 路由系统与守卫 (router/index.js)

```javascript
const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: Dashboard, meta: { title: '首页' } },
      { path: 'tenant', component: Tenant, meta: { title: '租户管理', permission: 'tenant:list' } },
      { path: 'role', component: Role, meta: { title: '角色管理', permission: 'role:list' } },
      { path: 'user', component: User, meta: { title: '员工管理', permission: 'user:list' } },
      { path: 'customer', component: Customer, meta: { title: '客户管理', permission: 'customer:list' } },
      { path: 'contract', component: Contract, meta: { title: '合同管理', permission: 'contract:list' } },
      { path: 'repair', component: Repair, meta: { title: '报修管理', permission: 'repair:list' } },
      { path: 'statistics', component: Statistics, meta: { title: '统计分析', permission: 'statistics:customer' } },
      { path: 'operation-log', component: OperationLog, meta: { title: '操作日志', permission: 'log:list' } },
      { path: 'backup', component: Backup, meta: { title: '数据备份' } }
    ]
  }
]

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 已登录访问登录页 → 跳转首页
  if (to.path === '/login') {
    next(token ? '/dashboard' : undefined)
    return
  }

  // 未登录 → 跳转登录页
  if (!token) {
    next('/login')
    return
  }

  // 权限检查
  const requiredPermission = to.meta.permission
  if (requiredPermission) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo'))
    // 超级管理员放行
    if (userInfo?.tenantId !== 0) {
      const permissions = userInfo?.permissions || []
      if (!permissions.includes(requiredPermission)) {
        next('/dashboard')  // 无权访问,跳转首页
        return
      }
    }
  }

  next()
})
```

### 7.5 状态管理 (store/user.js — Pinia)

```javascript
import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),

  getters: {
    isSuperAdmin: (state) => state.userInfo.tenantId === 0,
    permissions: (state) => state.userInfo.permissions || []
  },

  actions: {
    async login(username, password) {
      const res = await loginApi({ username, password })
      this.token = res.token
      this.userInfo = res.userInfo

      // 持久化到localStorage
      localStorage.setItem('token', res.token)
      localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
    },

    async fetchUserInfo() {
      const res = await getUserInfoApi()
      this.userInfo = res
      localStorage.setItem('userInfo', JSON.stringify(res))
    },

    logout() {
      this.token = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
```

### 7.6 页面组件结构

#### Layout.vue — 主布局组件

```
┌──────────────────────────────────────────────────┐
│  顶部栏 (Header)                                  │
│  [Logo] 智慧燃气CRM    [欢迎,admin] [退出登录]     │
├──────────┬───────────────────────────────────────┤
│          │                                        │
│ 侧边栏   │         主内容区 (<router-view>)       │
│ (Sidebar)│                                        │
│          │                                        │
│ ☑ 首页   │                                        │
│ ☑ 租户管理│                                        │
│ ☑ 角色管理│                                        │
│ ☑ 员工管理│                                        │
│ ☑ 客户管理│                                        │
│ ☑ 合同管理│                                        │
│ ☑ 报修管理│                                        │
│ ☑ 统计分析│                                        │
│ ☑ 操作日志│                                        │
│ ☑ 数据备份│                                        │
│          │                                        │
└──────────┴───────────────────────────────────────┘
```

#### Customer.vue — 客户管理页面(示例)

```vue
<template>
  <div class="customer-container">
    <!-- 搜索区域 -->
    <el-card>
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="客户名称/联系人" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="success" v-permission="'customer:add'" @click="handleAdd">
            新增客户
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区域 -->
    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="name" label="客户名称" />
        <el-table-column prop="contactPerson" label="联系人" />
        <el-table-column prop="contactPhone" label="联系电话" />
        <el-table-column prop="businessCategory" label="业务类型" />
        <el-table-column prop="cooperationStatus" label="合作状态" />
        <el-table-column prop="region" label="所属区域" />
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
            <el-button type="warning" link v-permission="'customer:edit'"
                       @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link v-permission="'customer:delete'"
                       @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :total="total"
        @current-change="fetchData"
        layout="total, prev, pager, next"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="formData.contactPerson" />
        </el-form-item>
        <!-- 更多表单项... -->
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPage, create, update, deleteCustomer } from '@/api/customer'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = ref({ current: 1, size: 10, keyword: '' })

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPage(queryParams.value)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 新增
const handleAdd = () => { /* 打开对话框 */ }

// 编辑
const handleEdit = (row) => { /* 回显数据并打开对话框 */ }

// 删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该客户?', '提示', { type: 'warning' })
  await deleteCustomer(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

onMounted(() => fetchData())
</script>
```

### 7.7 自定义指令 (directives/permission.js)

```javascript
// v-permission 指令: 按权限控制元素显示/隐藏
export default {
  mounted(el, binding) {
    const requiredPermission = binding.value
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

    // 超级管理员始终显示
    if (userInfo.tenantId === 0) return

    // 检查权限
    const permissions = userInfo.permissions || []
    if (!permissions.includes(requiredPermission)) {
      el.parentNode && el.parentNode.removeChild(el) // 无权则移除DOM
    }
  }
}
```

**使用方式**:
```vue
<!-- 仅拥有 customer:add 权限的用户可见此按钮 -->
<el-button v-permission="'customer:add'" @click="handleAdd">新增</el-button>
```

---

## 8. 核心业务功能

### 8.1 客户管理

#### 功能概述
管理燃气公司的所有客户信息,包括商业用气客户、工业用气客户、加气站等。

#### 核心功能点

| 功能 | 说明 | API接口 |
|------|------|---------|
| **客户列表** | 分页查询,支持关键词搜索(名称/联系人) | `GET /api/customer/page` |
| **客户详情** | 查看客户完整信息,包含附件、跟进记录、合同 | `GET /api/customer/{id}` |
| **新增客户** | 录入客户基本信息 | `POST /api/customer` |
| **编辑客户** | 修改客户信息,自动记录修改日志 | `PUT /api/customer/{id}` |
| **删除客户** | 逻辑删除,保留历史数据 | `DELETE /api/customer/{id}` |
| **上传附件** | 上传合同扫描件、资质文件、现场照片等 | `POST /api/customer/{id}/attachment` |
| **状态变更** | 变更合作状态(潜在→意向→已合作),记录变更日志 | `PUT /api/customer/{id}/status` |
| **分配跟进人** | 指定客户跟进负责人 | `PUT /api/customer/{id}/follow-up-person` |

#### 客户业务分类

```
业务类型(business_category):
├── 加气站类
│   ├── CNG加气站
│   └── LPG加气站
├── 商业用气
│   ├── 餐饮类
│   ├── 团餐类
│   └── 其他商业类
└── 工业用气
    ├── 大型
    ├── 中型
    └── 小型

合作状态(cooperation_category):
├── 已合作
│   ├── 正常履约
│   ├── 逾期客户
│   ├── 暂停合作
│   └── 终止合作
├── 潜在
│   ├── 高潜力
│   ├── 中潜力
│   └── 低潜力
└── 意向
    └── 意向跟进
```

### 8.2 合同管理

#### 功能概述
管理客户合同的完整生命周期,从签订、生效、履约到到期续签。

#### 核心功能点

| 功能 | 说明 | API接口 |
|------|------|---------|
| **合同列表** | 按客户、状态筛选,分页查询 | `GET /api/contract/page` |
| **新建合同** | 录入合同编号、金额、日期等信息 | `POST /api/contract` |
| **编辑合同** | 修改合同内容 | `PUT /api/contract/{id}` |
| **合同附件** | 上传合同扫描件、补充协议等 | `POST /api/contract/{id}/attachment` |
| **状态流转** | 草稿→生效→到期→终止 | `PUT /api/contract/{id}/status` |
| **到期提醒** | 设置合同到期前N天提醒 | `POST /api/contract/{id}/reminder` |

#### 合同状态机

```
                    ┌─────────────┐
                    │   draft     │
                    │   (草稿)    │
                    └──────┬──────┘
                           │ 签订确认
                           ▼
                    ┌─────────────┐
              ┌────▶│  effective  │◀────┐
              │     │   (生效)    │     │
              │     └──────┬──────┘     │
              │            │ 到期       │ 续签
              │            ▼            │
              │     ┌─────────────┐     │
              │     │   expired    │─────┘
              │     │   (到期)    │
              │     └──────┬──────┘
              │            │ 终止
              │            ▼
              │     ┌─────────────┐
              └─────│ terminated  │
                    │   (终止)    │
                    └─────────────┘
```

### 8.3 报修管理

#### 功能概述
管理客户的设备报修工单,跟踪处理进度,形成闭环管理。

#### 核心功能点

| 功能 | 说明 | API接口 |
|------|------|---------|
| **工单列表** | 按状态、优先级、客户筛选 | `GET /api/repair/page` |
| **创建工单** | 客户报修,录入问题描述 | `POST /api/repair` |
| **派单处理** | 分配处理人,更新状态为"已派单" | `PUT /api/repair/{id}/assign` |
| **处理中** | 处理人接单,开始处理 | `PUT /api/repair/{id}/process` |
| **解决工单** | 填写解决方案,标记已解决 | `PUT /api/repair/{id}/resolve` |
| **关闭工单** | 确认解决,关闭工单 | `PUT /api/repair/{id}/close` |
| **异常处理** | 标记异常情况,需要升级处理 | `PUT /api/repair/{id}/exception` |
| **上传附件** | 上传现场照片、维修报告等 | `POST /api/repair/{id}/attachment` |
| **处理日志** | 记录每个环节的操作人和时间 | 自动记录 |

#### 工单状态流转

```
pending(待处理)
    │
    │ [派单]
    ▼
assigned(已派单)
    │
    │ [接单处理]
    ▼
in_progress(处理中)
    │
    ├──▶ resolved(已解决) ──▶ closed(已关闭)
    │
    └──▶ exception(异常) ──▶ 重新进入处理流程
```

**优先级定义**:

| 优先级 | 响应时限 | 说明 |
|--------|---------|------|
| **urgent** (紧急) | 2小时内 | 重大安全隐患,影响供气 |
| **high** (高) | 4小时内 | 影响正常使用 |
| **medium** (中) | 24小时内 | 一般故障 |
| **low** (低) | 72小时内 | 非紧急问题 |

### 8.4 统计分析

#### 功能概述
基于ECharts提供丰富的数据可视化报表,辅助管理层决策。

#### 统计维度

| 统计项 | 图表类型 | 数据来源 | 说明 |
|--------|---------|---------|------|
| **客户统计** | 饼图/柱状图 | biz_customer | 按业务类型、合作状态、区域分布 |
| **合同统计** | 柱状图 | biz_contract | 按月签约量、合同金额分布 |
| **报修趋势** | 折线图 | biz_repair_order | 月度报修量趋势、平均处理时长 |
| **客户拜访** | 柱状图 | biz_follow_up_record | 拜访次数统计、跟进效果分析 |

#### API示例

```javascript
// 获取客户统计数据
GET /api/statistics/customer
Response: {
  totalCustomers: 150,
  byBusinessCategory: [
    { name: '商业用气', value: 80 },
    { name: '工业用气', value: 50 },
    { name: '加气站类', value: 20 }
  ],
  byCooperationStatus: [
    { name: '正常履约', value: 100 },
    { name: '潜在客户', value: 30 },
    { name: '意向跟进', value: 20 }
  ],
  byRegion: [...]
}

// 获取报修趋势数据
GET /api/statistics/repair/trend?months=6
Response: {
  months: ['2024-01', '2024-02', ..., '2024-06'],
  counts: [12, 15, 8, 20, 18, 25],
  avgResolutionHours: [4.5, 3.2, 5.1, 2.8, 3.6, 4.0]
}
```

### 8.5 其他功能模块

#### 8.5.1 跟进记录管理
- 记录客户拜访、电话沟通、邮件往来等跟进活动
- 支持设置下次跟进时间
- 关联具体客户,便于回溯历史

#### 8.5.2 操作日志审计
- 自动记录用户的增删改操作
- 通过 `@OperationLog` 注解标记需要记录的方法
- 记录内容: 操作人、操作时间、操作模块、操作类型、详细信息、IP地址

#### 8.5.3 数据备份恢复
- 手动触发数据库备份(mysqldump)
- 定时自动备份(可配置cron表达式)
- 备份文件管理(下载、删除)
- 显示备份记录列表(文件名、大小、时间)

#### 8.5.4 提醒系统
- 合同到期提前N天提醒
- 登录时弹出待办提醒对话框
- 按租户、按用户个性化提醒

---

## 9. 多租户与权限系统

### 9.1 多租户隔离机制

#### 9.1.1 实现原理

```
┌─────────────────────────────────────────────────────────────┐
│                    请求到达后端                              │
│                         │                                   │
│                         ▼                                   │
│              ┌──────────────────────┐                       │
│              │   JwtAuthFilter      │                       │
│              │  解析JWT Token       │                       │
│              │  获取tenantId        │                       │
│              └──────────┬───────────┘                       │
│                         │                                   │
│                         ▼                                   │
│              ┌──────────────────────┐                       │
│              │   TenantContext      │                       │
│              │  .setTenantId(id)    │ ← ThreadLocal存储     │
│              └──────────┬───────────┘                       │
│                         │                                   │
│                         ▼                                   │
│              ┌──────────────────────┐                       │
│              │ MyBatis-Plus拦截器    │                       │
│              │                      │                       │
│              │  SQL: SELECT * FROM  │                       │
│              │  biz_customer        │                       │
│              │         ↓            │                       │
│              │  自动追加:           │                       │
│              │  WHERE tenant_id = ? │ ← 自动注入租户条件     │
│              └──────────┬───────────┘                       │
│                         │                                   │
│                         ▼                                   │
│              只返回当前租户的数据 ✓                           │
└─────────────────────────────────────────────────────────────┘
```

#### 9.1.2 租户隔离策略矩阵

| 场景 | 超级管理员(tenant_id=0) | 租户管理员 | 普通员工 |
|------|--------------------------|-----------|---------|
| **查看租户列表** | ✅ 所有租户 | ❌ 无权 | ❌ 无权 |
| **查看本租户用户** | ✅ 所有用户 | ✅ 本租户用户 | ❌ 无权 |
| **查看业务数据** | ✅ 所有租户数据 | ✅ 本租户数据 | ✅ 本租户数据(受权限限制) |
| **创建子账户** | ✅ 创建租户 | ✅ 创建员工 | ❌ 无权 |
| **分配权限** | ✅ 全部权限 | ✅ ≤自身权限范围 | ❌ 无权 |

### 9.2 RBAC权限控制系统

#### 9.2.1 权限模型

```
┌─────────────┐     N:1     ┌─────────────┐     N:M     ┌─────────────┐
│   User      │────────────▶│    Role     │────────────▶│ Permission  │
│  (用户)     │             │   (角色)    │             │   (权限)    │
└─────────────┘             └─────────────┘             └─────────────┘
      │                                                         │
      │ tenant_id                                               │ type
      │                                                         │ ├── menu (菜单权限)
      │                                                         │ └── button (按钮权限)
      ▼
┌─────────────┐
│   Tenant    │
│   (租户)    │
└─────────────┘
```

#### 9.2.2 权限校验流程

```
Controller方法标注: @RequirePermission("customer:add")
                         │
                         ▼
              ┌─────────────────────┐
              │  PermissionAspect   │
              │  (AOP环绕通知)       │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │ 1. 获取当前用户      │
              │    UserContext       │
              └──────────┬──────────┘
                         │
              ┌──────────▼──────────┐
              │ 2. 判断是否超管?     │
              │    isSuperAdmin()?  │
              └──────────┬──────────┘
                    │         │
                   YES        NO
                    │         │
                    ▼         ▼
                 直接放行   ┌──────────────────┐
                           │ 3. 获取注解权限    │
                           │    "customer:add"  │
                           └────────┬─────────┘
                                    │
                           ┌────────▼─────────┐
                           │ 4. 获取用户权限列表 │
                           │    UserContext     │
                           │    .getPermissions │
                           └────────┬─────────┘
                                    │
                           ┌────────▼─────────┐
                           │ 5. 权限包含判断    │
                           │    contains()?     │
                           └────────┬─────────┘
                                    │
                              YES──┴──NO
                              │         │
                              ▼         ▼
                           放行 ✓   抛出异常 ✗
                                  BusinessException(403)
```

#### 9.2.3 权限继承机制

**场景**: 租户管理员给员工分配权限时,不能分配自己都没有的权限

```java
// PermissionServiceImpl.checkPermissionsWithin()
public void checkPermissionsWithin(List<Long> permissionIds) {
    // 1. 超级管理员直接通过
    if (UserContext.isSuperAdmin()) return;

    // 2. 获取当前操作者(租户管理员)的权限ID集合
    Set<Long> myPermissionIds = getMyPermissionIds();

    // 3. 检查待分配的每个权限是否在管理员权限范围内
    for (Long pid : permissionIds) {
        if (!myPermissionIds.contains(pid)) {
            throw new BusinessException(403, "无法分配超出自身权限范围的权限");
        }
    }
}
```

**权限继承示例**:
```
超级管理员权限: [system, tenant, role, user, customer, contract, repair, statistics, log]
    │
    ▼ (创建租户A,分配部分权限给租户管理员)
租户A管理员权限: [role, user, customer, contract, repair, statistics]
    │
    ▼ (租户A管理员创建员工,只能分配≤自身范围的权限)
租户A员工权限: [customer, contract]  ✓ 合法
租户A员工权限: [tenant, log]           ✗ 非法!越权!
```

### 9.3 三层账户体系详解

```
┌─────────────────────────────────────────────────────────────────┐
│  第一层: 超级管理员 (Platform Admin)                             │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  账号: admin / 密码: 123456                                     │
│  tenant_id = 0 (特殊标识)                                       │
│                                                                  │
│  ✅ 权限范围:                                                    │
│    • 管理所有租户(创建/编辑/禁用/删除)                            │
│    • 查看所有租户的全部数据                                       │
│    • 拥有系统所有权限,自动放行所有权限检查                         │
│    • 查看操作日志、备份数据                                       │
│                                                                  │
│  ❌ 限制:                                                        │
│    • 无法操作具体的业务数据(客户/合同/报修等)                      │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  第二层: 租户管理员 (Tenant Admin)                               │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  账号: {租户编码}admin (如: gas001admin)                         │
│  密码: 创建时指定或随机生成                                       │
│  tenant_id = 租户ID (如: 1, 2, 3...)                            │
│                                                                  │
│  ✅ 权限范围:                                                    │
│    • 管理本租户的角色(创建/编辑/删除)                             │
│    • 管理本租户的员工(创建/编辑/重置密码/删除)                    │
│    • 操作本租户的所有业务数据                                     │
│    • 查看本租户的统计报表                                         │
│    • 分配权限给下级(不能超过自身权限范围) ✓                        │
│                                                                  │
│  ❌ 限制:                                                        │
│    • 无法查看其他租户数据                                         │
│    • 无法管理系统级配置                                           │
│    • 受到超级管理员分配的权限范围限制                               │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│  第三层: 租户普通员工 (Regular Employee)                          │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  账号: 由租户管理员创建                                          │
│  密码: 默认 123456                                               │
│  tenant_id = 租户ID (同上)                                       │
│                                                                  │
│  ✅ 权限范围:                                                    │
│    • 根据被分配的角色权限操作业务数据                              │
│    • 例如: 只有 customer:list 权限 → 只能查看客户列表             │
│    • 例如: 有 repair:add + repair:edit → 可以创建和编辑报修工单  │
│                                                                  │
│  ❌ 限制:                                                        │
│    • 无法管理用户、角色                                           │
│    • 无法查看其他员工数据                                         │
│    • 只能操作被授权的功能和数据                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. API接口文档

### 10.1 接口规范

**Base URL**: `http://localhost:8080/api`

**统一响应格式**:
```json
{
  "code": 200,           // 状态码
  "message": "success",  // 提示消息
  "data": {}             // 业务数据(可选)
}
```

**状态码定义**:

| code | 说明 | HTTP Status |
|------|------|-------------|
| 200 | 成功 | 200 |
| 400 | 业务错误(参数错误/业务规则违反) | 200 (body中体现) |
| 401 | 未认证(Token缺失/无效/过期) | 401 |
| 403 | 权限不足 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |

**认证方式**:
```
Authorization: Bearer <jwt_token>
```

**分页请求参数**:
```
?page=1&size=10&keyword=xxx
```

**分页响应格式**:
```json
{
  "code": 200,
  "data": {
    "records": [],      // 当前页数据数组
    "total": 100,       // 总记录数
    "size": 10,         // 每页大小
    "current": 1        // 当前页码
  }
}
```

### 10.2 认证模块 (/api/auth)

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|:----:|
| POST | `/auth/login` | 用户登录 | ❌ | - |
| GET | `/auth/info` | 获取当前用户信息 | ✅ | - |

**登录请求**:
```json
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**登录响应**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI...",
    "userInfo": {
      "id": 1,
      "username": "admin",
      "realName": "超级管理员",
      "roleId": 1,
      "roleName": "超级管理员",
      "tenantId": 0,
      "permissions": ["system", "tenant", "tenant:list", ...]
    }
  }
}
```

### 10.3 租户管理 (/api/tenant)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/tenant/page` | 分页查询租户列表 | `tenant:list` |
| POST | `/tenant` | 创建租户 | `tenant:add` |
| PUT | `/tenant/{id}/status` | 修改租户状态 | `tenant:edit` |

**创建租户请求**:
```json
{
  "name": "奉天数智科技有限公司",
  "code": "gas001",
  "adminPassword": "888888"  // 可选,不填则随机生成
}
```

**创建租户响应**:
```json
{
  "code": 200,
  "data": {
    "tenantId": 1,
    "adminUsername": "gas001admin",
    "adminPassword": "888888"  // 或随机生成的6位密码
  }
}
```

### 10.4 角色管理 (/api/role)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/role/{id}` | 查询角色详情(含权限ID列表) | `role:list` |
| GET | `/role/page` | 分页查询角色列表 | `role:list` |
| POST | `/role` | 新增角色 | `role:add` |
| PUT | `/role/{id}` | 修改角色 | `role:edit` |
| DELETE | `/role/{id}` | 删除角色 | `role:delete` |

**创建角色请求**:
```json
{
  "name": "销售经理",
  "permissionIds": [10, 11, 12, 20, 21]  // 权限ID数组
}
```

### 10.5 用户/员工管理 (/api/user)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/user/page` | 分页查询员工列表 | `user:list` |
| POST | `/user` | 新增员工 | `user:add` |
| PUT | `/user/{id}` | 修改员工信息 | `user:edit` |
| PUT | `/user/{id}/reset-password` | 重置密码为123456 | `user:edit` |
| DELETE | `/user/{id}` | 删除员工(逻辑删除) | `user:delete` |

**创建员工请求**:
```json
{
  "username": "zhangsan",
  "realName": "张三",
  "roleId": 5,           // 角色ID
  "password": "123456"   // 可选,默认123456
}
```

### 10.6 客户管理 (/api/customer)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/customer/page` | 分页查询客户列表 | `customer:list` |
| GET | `/customer/{id}` | 查询客户详情 | `customer:list` |
| POST | `/customer` | 新增客户 | `customer:add` |
| PUT | `/customer/{id}` | 修改客户 | `customer:edit` |
| DELETE | `/customer/{id}` | 删除客户 | `customer:delete` |
| PUT | `/customer/{id}/status` | 变更合作状态 | `customer:edit` |
| PUT | `/customer/{id}/follow-up-person` | 设置跟进人 | `customer:edit` |
| POST | `/customer/{id}/attachment` | 上传客户附件 | `customer:edit` |

**客户请求DTO**:
```json
{
  "name": "XX餐饮有限公司",
  "address": "沈阳市和平区XX路XX号",
  "detailAddress": "3号楼205室",
  "region": "辽宁省/沈阳市/和平区",
  "contactPerson": "王经理",
  "contactPhone": "13800138000",
  "businessCategory": "商业用气",
  "businessType": "餐饮类",
  "cooperationCategory": "已合作",
  "cooperationStatus": "正常履约",
  "gasScale": null,
  "smartGasSystem": "商用燃气报警器V2.0",
  "followUpPersonId": 5
}
```

### 10.7 合同管理 (/api/contract)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/contract/page` | 分页查询合同列表 | `contract:list` |
| GET | `/contract/{id}` | 查询合同详情 | `contract:list` |
| POST | `/contract` | 新建合同 | `contract:add` |
| PUT | `/contract/{id}` | 编辑合同 | `contract:edit` |
| DELETE | `/contract/{id}` | 删除合同 | `contract:delete` |
| PUT | `/contract/{id}/status` | 合同状态流转 | `contract:edit` |
| POST | `/contract/{id}/attachment` | 上传合同附件 | `contract:edit` |
| POST | `/contract/{id}/reminder` | 设置到期提醒 | `contract:edit` |

### 10.8 报修管理 (/api/repair)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/repair/page` | 分页查询工单列表 | `repair:list` |
| GET | `/repair/{id}` | 查询工单详情 | `repair:list` |
| POST | `/repair` | 创建报修工单 | `repair:add` |
| PUT | `/repair/{id}/assign` | 派单(分配处理人) | `repair:edit` |
| PUT | `/repair/{id}/process` | 开始处理 | `repair:edit` |
| PUT | `/repair/{id}/resolve` | 解决工单 | `repair:edit` |
| PUT | `/repair/{id}/close` | 关闭工单 | `repair:edit` |
| PUT | `/repair/{id}/exception` | 标记异常 | `repair:edit` |
| POST | `/repair/{id}/attachment` | 上传报修附件 | `repair:edit` |

**创建工单请求**:
```json
{
  "customerId": 10,
  "title": "燃气泄漏报警器故障",
  "description": "设备显示屏不亮,疑似电源问题",
  "maintenanceCategory": "设备故障",
  "priority": "high",
  "reporterName": "李四",
  "reporterPhone": "13900139000"
}
```

### 10.9 跟进记录 (/api/followUp)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/followUp/page` | 分页查询跟进记录 | `customer:list` |
| POST | `/followUp` | 新增跟进记录 | `customer:edit` |
| PUT | `/followUp/{id}` | 修改跟进记录 | `customer:edit` |
| DELETE | `/followUp/{id}` | 删除跟进记录 | `customer:edit` |

### 10.10 统计分析 (/api/statistics)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/statistics/customer` | 客户统计数据 | `statistics:customer` |
| GET | `/statistics/contract` | 合同统计数据 | `statistics:contract` |
| GET | `/statistics/repair` | 报修统计数据 | `statistics:repair` |
| GET | `/statistics/repair/trend` | 报修趋势数据 | `statistics:repair` |
| GET | `/statistics/visit` | 拜访统计数据 | `statistics:visit` |

### 10.11 操作日志 (/api/operation-log)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/operation-log/page` | 分页查询操作日志 | `log:list` |

### 10.12 数据备份 (/api/backup)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/backup/list` | 获取备份记录列表 | - (仅超管) |
| POST | `/backup/create` | 手动触发备份 | - (仅超管) |
| GET | `/backup/download/{id}` | 下载备份文件 | - (仅超管) |
| DELETE | `/backup/{id}` | 删除备份记录 | - (仅超管) |

### 10.13 权限管理 (/api/permission)

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/permission/tree` | 获取权限树形结构 | `role:list` |

**权限树响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "系统管理",
      "code": "system",
      "type": "menu",
      "children": [
        {
          "id": 2,
          "name": "租户管理",
          "code": "tenant",
          "type": "menu",
          "children": [
            { "id": 3, "name": "租户列表", "code": "tenant:list", "type": "button" },
            { "id": 4, "name": "创建租户", "code": "tenant:add", "type": "button" },
            { "id": 5, "name": "编辑租户", "code": "tenant:edit", "type": "button" }
          ]
        },
        // ...更多权限节点
      ]
    }
  ]
}
```

---

## 11. 部署指南

### 11.1 环境要求

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | 17+ | 推荐LTS版本(17或21) |
| **Maven** | 3.6+ | 项目构建工具 |
| **Node.js** | 16+ | 前端运行环境 |
| **npm** | 8+ | 包管理器(或使用pnpm/yarn) |
| **MySQL** | 8.0+ | 关系型数据库 |
| **操作系统** | Windows/Linux/macOS | 跨平台支持 |

### 11.2 后端部署步骤

#### Step 1: 初始化数据库

```bash
# 方式1: 命令行导入
mysql -u root -p < saas-framework/docs/init.sql

# 方式2: MySQL客户端导入(Navicat/DBeaver/Workbench)
# 打开 init.sql 文件并执行
```

> **注意**: 还需按顺序执行 `docs/migration_*.sql` 迁移脚本(如果是从旧版本升级)

#### Step 2: 修改配置文件

编辑 `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080  # 可修改为其他端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/saaslearn?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root           # 修改为你的数据库用户名
    password: your_password  # 修改为你的数据库密码

jwt:
  secret: your-custom-secret-key-at-least-256-bits-long  # 生产环境务必修改!
  expiration: 7200000       # Token有效期(毫秒),默认2小时

file:
  upload-path: ./uploads/   # 文件上传目录(相对路径=项目根目录)
  backup-path: ./backups/   # 备份文件目录
```

#### Step 3: 编译打包

```bash
cd saas-framework/backend

# 方式1: Maven直接运行(开发模式)
mvn clean spring-boot:run

# 方式2: 打包成JAR后运行(生产模式)
mvn clean package -DskipTests
java -jar target/saas-framework-1.0.0.jar

# 方式3: 后台运行(Linux)
nohup java -jar target/saas-framework-1.0.0.jar > app.log 2>&1 &
```

#### Step 4: 验证后端启动

```bash
# 测试1: 检查端口是否监听
curl http://localhost:8080/swagger-ui.html

# 测试2: 尝试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

启动成功标志:
- ✅ 控制台输出: `Started SaasFrameworkApplication in x.xxx seconds`
- ✅ 可访问: `http://localhost:8080/swagger-ui.html` (Swagger UI)
- ✅ 登录接口返回Token

### 11.3 前端部署步骤

#### Step 1: 安装依赖

```bash
cd saas-framework/frontend

# 安装npm依赖
npm install

# 或使用pnpm(更快)
pnpm install
```

#### Step 2: 开发模式启动

```bash
# 启动开发服务器(Vite HMR热更新)
npm run dev

# 或
pnpm dev
```

启动成功:
- ✅ 前端地址: `http://localhost:3000`
- ✅ 自动打开浏览器(如果配置了open)
- ✅ API请求代理到后端8080端口

#### Step 3: 生产环境构建

```bash
# 构建(生成dist目录)
npm run build

# 预览构建结果
npm run preview
```

构建产物位置: `frontend/dist/`

#### Step 4: 部署前端(可选方案)

**方案A: Nginx反向代理(推荐生产环境)**

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/saas-framework/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;  # Vue Router history模式
    }

    # API代理到后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**方案B: Spring Boot静态资源(简单部署)**

将 `frontend/dist/` 的内容复制到 `backend/src/main/resources/static/` 目录,这样前后端共用同一个端口(8080)。

### 11.4 Docker部署(可选)

#### Dockerfile (后端)

```dockerfile
# Dockerfile.backend
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/saas-framework-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

#### Dockerfile (前端)

```dockerfile
# Dockerfile.frontend
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### docker-compose.yml

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: saaslearn
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docs/init.sql:/docker-entrypoint-initdb.d/init.sql

  backend:
    build:
      context: .
      dockerfile: Dockerfile.backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/saaslearn
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

**一键启动**:
```bash
docker-compose up -d
```

### 11.5 生产环境检查清单

- [ ] 修改数据库密码(非默认的123456)
- [ ] 修改JWT Secret Key(至少256位随机字符串)
- [ ] 适当调整JWT过期时间(建议生产环境缩短至30分钟-2小时)
- [ ] 配置HTTPS(SSL证书)
- [ ] 配置防火墙,只开放80/443端口
- [ ] 设置MySQL定期备份策略
- [ ] 配置日志轮转(logback/log4j)
- [ ] 监控服务器资源(CPU/内存/磁盘)
- [ ] 压力测试和性能调优
- [ ] 定期更新依赖版本(安全补丁)

---

## 12. 开发规范

### 12.1 后端代码规范

#### 命名约定

| 类型 | 约定 | 示例 |
|------|------|------|
| **类名** | 大驼峰(PascalCase) | `CustomerServiceImpl` |
| **方法名** | 小驼峰(camelCase) | `getPage()`, `createCustomer()` |
| **常量** | 全大写下划线 | `MAX_FILE_SIZE` |
| **包名** | 全小写 | `com.saas.framework.service.impl` |
| **数据库表** | 小写+下划线 | `biz_customer`, `sys_user` |
| **API路径** | 小写+连字符 | `/api/customer/page` |

#### 分层调用规则

```
✅ 正确: Controller → Service → Mapper
❌ 错误: Controller → Mapper (跳过Service层)
❌ 错误: Service → Controller (循环调用)
```

#### Service层事务管理

```java
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerModifyLogMapper modifyLogMapper;

    @Override
    @Transactional  // 涉及多表操作必须添加事务
    public void update(Long id, CustomerRequest request) {
        // 1. 查询原数据
        BizCustomer oldCustomer = customerMapper.selectById(id);

        // 2. 记录修改日志
        saveModifyLog(oldCustomer, request);

        // 3. 更新客户信息
        BizCustomer customer = convertToEntity(request);
        customer.setId(id);
        customerMapper.updateById(customer);
    }
}
```

#### 异常处理规范

```java
// ✅ 推荐: 抛出业务异常(会被GlobalExceptionHandler捕获)
if (customer == null) {
    throw new BusinessException("客户不存在");  // code=400
}

// ✅ 推荐: 明确指定状态码
throw new BusinessException(403, "无权操作此客户");

// ❌ 不推荐: 直接返回null或空值
return null;

// ❌ 不推荐: 在Controller中try-catch(应该交给全局异常处理器)
```

#### 多租户数据隔离规范

```java
@Service
public class SomeServiceImpl implements SomeService {

    @Override
    public PageResult<BizEntity> page(int current, int size) {
        Page<BizEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<BizEntity> wrapper = new LambdaQueryWrapper<>();

        // ✅ 必须: 非超级管理员添加租户过滤条件
        if (!UserContext.isSuperAdmin()) {
            wrapper.eq(BizEntity::getTenantId, UserContext.getTenantId());
        }

        // 其他查询条件...
        return convertToPageResult(mapper.selectPage(page, wrapper));
    }
}
```

### 12.2 前端代码规范

#### 组件命名

| 类型 | 约定 | 示例 |
|------|------|------|
| **页面组件** | 大驼峰 | `CustomerManagement.vue` |
| **公共组件** | 大驼峰前缀 | `BaseTable.vue`, `LoginReminderDialog.vue` |
| **API文件** | 小驼峰 | `customer.js`, `orderService.js` |
| **路由路径** | 小写连字符 | `/customer-management`, `/repair-orders` |

#### Vue 3 Composition API风格

```vue
<script setup>
// ✅ 推荐: 使用 <script setup> 语法糖
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getPage, create } from '@/api/customer'

// 响应式状态
const loading = ref(false)
const tableData = ref([])
const formData = reactive({
  name: '',
  contactPerson: ''
})

// 计算属性
const isEditable = computed(() => {
  return useUserStore().permissions.includes('customer:edit')
})

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPage(queryParams.value)
    tableData.value = res.records
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchData()
})
</script>
```

#### API调用规范

```javascript
// ✅ 推荐: 使用async/await,统一错误处理
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除?', '提示', { type: 'warning' })
    await deleteCustomer(row.id)
    ElMessage.success('删除成功')
    fetchData()  // 刷新列表
  } catch (error) {
    // 用户取消或网络错误(已在request.js统一处理ElMessage.error)
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// ❌ 不推荐: .then()链式调用(难以处理错误)
deleteCustomer(row.id).then(() => {
  ElMessage.success('成功')
}).catch(err => {
  console.error(err)
})
```

#### 权限控制使用

```vue
<template>
  <!-- 1. 路由级别: router meta.permission (自动拦截) -->

  <!-- 2. 按钮级别: v-permission 指令 (DOM移除) -->
  <el-button v-permission="'customer:add'" type="success">新增</el-button>
  <el-button v-permission="'customer:edit'" @click="handleEdit">编辑</el-button>

  <!-- 3. 代码级别: store.permissions (条件渲染) -->
  <div v-if="userStore.permissions.includes('customer:delete')">
    <el-button type="danger" @click="handleDelete">删除</el-button>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
const userStore = useUserStore()
</script>
```

### 12.3 Git提交规范

#### Commit Message格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type类型**:

| 类型 | 说明 |
|------|------|
| feat | 新功能(feature) |
| fix | 修复bug |
| docs | 文档(documentation) |
| style | 格式调整(不影响代码运行) |
| refactor | 重构(既不是新增功能也不是修复bug) |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具/辅助工具变动 |

**示例**:
```
feat(customer): 添加客户批量导入功能

- 支持Excel文件上传(.xlsx/.xls)
- 使用Apache POI解析文件
- 添加数据校验和错误提示

Closes #123
```

#### 分支管理

```
main (生产环境)
  │
  ├── develop (开发环境)
  │     │
  │     ├── feature/customer-export (功能分支)
  │     ├── feature/repair-statistics
  │     │
  │     ├── hotfix/login-bug-fix (紧急修复)
  │     └── release/v1.1.0 (发布分支)
  │
  └── tag/v1.0.0 (版本标签)
```

### 12.4 代码审查清单

#### 后端审查要点

- [ ] 是否遵循分层架构(Controller→Service→Mapper)
- [ ] Service层涉及多表操作是否添加 `@Transactional`
- [ ] 是否正确处理多租户数据隔离(`wrapper.eq(tenantId)`)
- [ ] 是否使用 `@RequirePermission` 标注权限要求
- [ ] 是否对输入参数进行校验(`@Valid` + DTO校验注解)
- [ ] 异常处理是否规范(抛出BusinessException而非直接返回)
- [ ] 敏感信息是否脱敏(密码、Token等不记录到日志)
- [ ] SQL查询是否有性能问题(避免N+1查询,大表添加索引)

#### 前端审查要点

- [ ] 是否使用 `<script setup>` 和 Composition API
- [ ] API调用是否使用 async/await 并正确处理错误
- [ ] 是否正确使用 `v-permission` 指令进行权限控制
- [ ] 表单是否添加校验 rules
- [ ] 是否处理好加载状态(loading)、空状态(empty)、错误状态(error)
- [ ] 组件是否合理拆分(避免单个文件过大)
- [ ] 是否有内存泄漏风险(定时器、事件监听器需在onUnmounted清理)

### 12.5 性能优化建议

#### 后端优化

| 优化项 | 说明 | 预期效果 |
|--------|------|---------|
| **数据库索引** | 为常用查询字段添加索引 | 查询速度提升10-100倍 |
| **SQL优化** | 避免SELECT *,只查询必要字段 | 减少网络传输和内存占用 |
| **连接池调优** | 调整HikariCP参数(maxSize,minIdle等) | 提升并发能力 |
| **缓存** | 热点数据使用Redis缓存 | 减少数据库压力 |
| **分页优化** | 避免深度分页(百万级数据) | 防止慢查询 |
| **异步处理** | 耗时操作使用异步(发送邮件、生成报表) | 提升响应速度 |

#### 前端优化

| 优化项 | 说明 | 预期效果 |
|--------|------|---------|
| **懒加载** | 路由懒加载(`() => import(...)`) | 减少首屏加载时间 |
| **组件按需** | Element Plus按需引入 | 减少打包体积 |
| **图片优化** | 压缩图片,使用WebP格式 | 减少带宽占用 |
| **虚拟滚动** | 大列表使用虚拟滚动(virtual-scroller) | 提升渲染性能 |
| **防抖节流** | 搜索输入、窗口resize等场景 | 减少不必要的请求 |
| **CDN加速** | 静态资源使用CDN分发 | 加快全球访问速度 |

---

## 附录

### A. 默认账户信息

| 角色 | 用户名 | 密码 | tenant_id | 说明 |
|------|--------|------|-----------|------|
| 超级管理员 | admin | 123456 | 0 | 平台最高权限账户 |

> ⚠️ **生产环境请立即修改默认密码!**

### B. 初始权限列表

由 `init.sql` 初始化,权限树结构:

```
系统管理 (system)
├── 租户管理 (tenant)
│   ├── tenant:list (租户列表)
│   ├── tenant:add (创建租户)
│   ├── tenant:edit (编辑租户)
│   └── tenant:delete (删除租户)
├── 角色管理 (role)
│   ├── role:list (角色列表)
│   ├── role:add (创建角色)
│   ├── role:edit (编辑角色)
│   └── role:delete (删除角色)
├── 用户管理 (user)
│   ├── user:list (用户列表)
│   ├── user:add (创建用户)
│   ├── user:edit (编辑用户)
│   └── user:delete (删除用户)
└── 操作日志 (log)
    └── log:list (日志查看)

业务管理 (business)
├── 客户管理 (customer)
│   ├── customer:list (客户列表)
│   ├── customer:add (新增客户)
│   ├── customer:edit (编辑客户)
│   └── customer:delete (删除客户)
├── 合同管理 (contract)
│   ├── contract:list (合同列表)
│   ├── contract:add (新建合同)
│   ├── contract:edit (编辑合同)
│   └── contract:delete (删除合同)
├── 报修管理 (repair)
│   ├── repair:list (工单列表)
│   ├── repair:add (创建工单)
│   ├── repair:edit (处理工单)
│   └── repair:delete (删除工单)
├── 跟进管理 (followUp)
│   └── followUp:add (新增跟进)
├── 统计分析 (statistics)
│   ├── statistics:customer (客户统计)
│   ├── statistics:contract (合同统计)
│   ├── statistics:repair (报修统计)
│   └── statistics:visit (拜访统计)
└── 提醒设置 (reminder)
    └── reminder:set (设置提醒)
```

### C. 常见问题排查 (FAQ)

**Q1: 启动后端报错 "Access denied for user"**
```
A: 检查application.yml中的数据库配置:
   - 确认MySQL服务已启动
   - 确认用户名密码正确
   - 确认数据库saaslearn已创建
```

**Q2: 前端请求API返回401未认证**
```
A: 可能原因:
   - Token未携带或已过期 → 重新登录
   - 后端JWT Secret不一致 → 检查配置文件
   - 浏览器localStorage被清空 → 重新登录
```

**Q3: 跨域请求失败(CORS)**
```
A: 检查以下配置:
   - 后端CorsConfig是否正确配置
   - 前端Vite代理是否配置(/api → localhost:8080)
   - 确保前端开发服务器正在运行
```

**Q4: 多租户数据互相可见**
```
A: 检查以下几点:
   - Service层是否添加了tenant_id过滤条件
   - MyBatis-Plus配置的业务表是否在ignoreTables中
   - UserContext.getTenantId()是否正确获取到值
```

**Q5: 权限校验不生效**
```
A: 排查步骤:
   - Controller方法是否标注@RequirePermission
   - 用户角色是否分配了对应权限
   - 权限编码是否完全匹配(区分大小写)
   - 检查浏览器Network面板查看响应码
```

### D. 相关文档链接

- **快速启动指南**: [QUICKSTART.md](./QUICKSTART.md)
- **开发教程**: [TUTORIAL.md](./TUTORIAL.md)
- **技术Wiki**: [CODE_WIKI.md](./CODE_WIKI.md)
- **API在线文档**: http://localhost:8080/swagger-ui.html (启动后端后访问)

---

## 版本信息

| 项目 | 信息 |
|------|------|
| **项目名称** | SaaS 智慧燃气CRM管理系统 |
| **版本号** | v1.0.0 |
| **最后更新** | 2026-05-25 |
| **文档作者** | AI Assistant |
| **适用范围** | 开发团队、运维人员、项目管理人员 |

---

> 📝 **文档维护说明**: 本文档应随项目迭代持续更新,确保与代码保持同步。如有疑问或发现文档错误,请及时反馈。
