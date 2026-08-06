# SaaS 多租户教学框架 — Code Wiki

---

## 目录

1. [项目概述](#1-项目概述)
2. [整体架构](#2-整体架构)
3. [技术栈与依赖](#3-技术栈与依赖)
4. [目录结构](#4-目录结构)
5. [数据库设计](#5-数据库设计)
6. [后端模块详解](#6-后端模块详解)
   - 6.1 [启动类](#61-启动类)
   - 6.2 [公共模块 (common)](#62-公共模块-common)
   - 6.3 [配置模块 (config)](#63-配置模块-config)
   - 6.4 [实体层 (entity)](#64-实体层-entity)
   - 6.5 [数据访问层 (mapper)](#65-数据访问层-mapper)
   - 6.6 [服务层 (service)](#66-服务层-service)
   - 6.7 [控制器层 (controller)](#67-控制器层-controller)
7. [前端模块详解](#7-前端模块详解)
   - 7.1 [入口与全局配置](#71-入口与全局配置)
   - 7.2 [路由系统](#72-路由系统)
   - 7.3 [状态管理](#73-状态管理)
   - 7.4 [HTTP 请求封装](#74-http-请求封装)
   - 7.5 [API 模块](#75-api-模块)
   - 7.6 [自定义指令](#76-自定义指令)
   - 7.7 [页面组件](#77-页面组件)
8. [核心机制详解](#8-核心机制详解)
   - 8.1 [多租户隔离机制](#81-多租户隔离机制)
   - 8.2 [RBAC 权限控制](#82-rbac-权限控制)
   - 8.3 [JWT 认证流程](#83-jwt-认证流程)
   - 8.4 [三层账户体系](#84-三层账户体系)
9. [请求生命周期](#9-请求生命周期)
10. [项目运行方式](#10-项目运行方式)
11. [API 接口一览](#11-api-接口一览)
12. [默认账户与初始数据](#12-默认账户与初始数据)

---

## 1. 项目概述

本项目是一个 **SaaS 多租户教学框架**，用于演示和教学多租户数据隔离、RBAC 权限控制、三层账户体系等 SaaS 核心概念。项目采用前后端分离架构：

- **后端**：Spring Boot 2.7.x + MyBatis-Plus + MySQL，提供 RESTful API
- **前端**：Vue 3 + Element Plus + Pinia + Vite，提供管理后台界面

### 核心设计思想

| 设计思想 | 说明 |
|---------|------|
| **多租户隔离** | 所有业务表包含 `tenant_id` 字段，通过 MyBatis-Plus 多租户插件自动追加 `WHERE tenant_id = ?` 条件，实现数据隔离 |
| **RBAC 权限控制** | 基于 `用户 → 角色 → 权限` 的经典 RBAC 模型，通过自定义注解 `@RequirePermission` + AOP 切面实现声明式权限校验 |
| **权限继承** | 租户管理员只能分配自己权限范围内的权限给下级，防止越权 |
| **三层账户体系** | 超级账户 → 租户管理员 → 租户员工，层层递进 |

---

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + Vite)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  Login   │  │  Tenant  │  │   Role   │  │  Student │ ...   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       │              │              │              │             │
│  ┌────┴──────────────┴──────────────┴──────────────┴─────┐     │
│  │                   API 层 (axios 封装)                   │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │  /api/**                           │
│                     Vite Proxy (3000→8080)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────┴─────────────────────────────────────┐
│                     后端 (Spring Boot 2.7.x)                     │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Filter 层                              │   │
│  │   CorsFilter → JwtAuthFilter (Token 解析 + 上下文设置)     │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                  Controller 层                            │   │
│  │   AuthController / TenantController / RoleController      │   │
│  │   UserController / StudentController / PermissionController│  │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │            AOP 切面层 (PermissionAspect)                   │   │
│  │   拦截 @RequirePermission 注解 → 校验用户权限              │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                   Service 层                              │   │
│  │   AuthService / TenantService / RoleService               │   │
│  │   UserService / StudentService / PermissionService        │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │            MyBatis-Plus 拦截器层                           │   │
│  │   PaginationInnerInterceptor (分页)                        │   │
│  │   TenantLineInnerInterceptor (自动追加 tenant_id 条件)     │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│  ┌──────────────────────────┴───────────────────────────────┐   │
│  │                   Mapper 层                               │   │
│  │   SysUserMapper / SysRoleMapper / SysTenantMapper ...     │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
└─────────────────────────────┼────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   MySQL 8.0       │
                    │   数据库: saaslearn│
                    └───────────────────┘
```

---

## 3. 技术栈与依赖

### 后端依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| MyBatis-Plus | 3.5.5 | ORM 框架 + 多租户插件 + 分页插件 |
| MySQL Connector | 8.0.33 (runtime) | 数据库驱动 |
| jjwt | 0.9.1 | JWT Token 生成与解析 |
| spring-security-crypto | 5.7.x | BCrypt 密码加密 |
| Lombok | 1.18.30 | 代码简化（getter/setter/日志等） |
| springdoc-openapi-ui | 1.7.0 | Swagger API 文档 |
| spring-boot-starter-validation | 2.7.x | JSR303 参数校验 |
| spring-boot-starter-aop | 2.7.x | AOP 切面支持 |
| HikariCP | 4.0.3 | 数据库连接池 |

### 前端依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Vue | ^3.4.0 | 前端框架 |
| Vue Router | ^4.3.0 | 路由管理 |
| Pinia | ^2.1.0 | 状态管理 |
| Element Plus | ^2.6.0 | UI 组件库 |
| @element-plus/icons-vue | ^2.3.0 | Element Plus 图标 |
| Axios | ^1.6.0 | HTTP 请求 |
| Vite | ^5.4.0 | 构建工具 |
| @vitejs/plugin-vue | ^5.0.0 | Vue 编译插件 |

---

## 4. 目录结构

```
saas-framework/
├── backend/                              # Spring Boot 后端
│   ├── pom.xml                           # Maven 配置
│   └── src/
│       ├── main/
│       │   ├── java/com/saas/framework/
│       │   │   ├── SaasFrameworkApplication.java    # 启动类
│       │   │   ├── common/              # 公共模块
│       │   │   │   ├── Result.java                  # 统一响应封装
│       │   │   │   ├── annotation/                  # 自定义注解
│       │   │   │   │   └── RequirePermission.java   # 权限校验注解
│       │   │   │   ├── aspect/                      # AOP 切面
│       │   │   │   │   └── PermissionAspect.java    # 权限校验切面
│       │   │   │   ├── context/                     # 上下文（ThreadLocal）
│       │   │   │   │   ├── TenantContext.java        # 租户上下文
│       │   │   │   │   └── UserContext.java          # 用户上下文
│       │   │   │   ├── dto/                         # 数据传输对象
│       │   │   │   │   ├── LoginRequest.java         # 登录请求
│       │   │   │   │   ├── PageResult.java           # 分页响应
│       │   │   │   │   ├── RoleCreateRequest.java    # 角色创建请求
│       │   │   │   │   ├── RoleResponse.java         # 角色响应
│       │   │   │   │   ├── StudentRequest.java       # 学生请求
│       │   │   │   │   ├── TenantCreateRequest.java  # 租户创建请求
│       │   │   │   │   ├── UserCreateRequest.java    # 用户创建请求
│       │   │   │   │   └── UserUpdateRequest.java    # 用户修改请求
│       │   │   │   ├── exception/                   # 异常处理
│       │   │   │   │   ├── BusinessException.java    # 业务异常
│       │   │   │   │   └── GlobalExceptionHandler.java # 全局异常处理器
│       │   │   │   └── util/                        # 工具类
│       │   │   │       └── JwtUtil.java              # JWT 工具
│       │   │   ├── config/              # 配置类
│       │   │   │   ├── CorsConfig.java              # 跨域配置
│       │   │   │   ├── DataInitializer.java         # 数据初始化
│       │   │   │   ├── FilterConfig.java            # 过滤器注册
│       │   │   │   ├── JwtAuthFilter.java           # JWT 认证过滤器
│       │   │   │   ├── MyBatisPlusConfig.java       # MyBatis-Plus 配置
│       │   │   │   └── SecurityConfig.java          # 安全配置
│       │   │   ├── controller/          # 控制器层
│       │   │   │   ├── AuthController.java          # 认证控制器
│       │   │   │   ├── PermissionController.java    # 权限控制器
│       │   │   │   ├── RoleController.java          # 角色控制器
│       │   │   │   ├── StudentController.java       # 学生控制器
│       │   │   │   ├── TenantController.java        # 租户控制器
│       │   │   │   └── UserController.java          # 用户控制器
│       │   │   ├── entity/              # 实体类
│       │   │   │   ├── BizStudent.java              # 学生实体
│       │   │   │   ├── SysPermission.java           # 权限实体
│       │   │   │   ├── SysRole.java                 # 角色实体
│       │   │   │   ├── SysRolePermission.java       # 角色权限关联实体
│       │   │   │   ├── SysTenant.java               # 租户实体
│       │   │   │   └── SysUser.java                 # 用户实体
│       │   │   ├── mapper/              # 数据访问层
│       │   │   │   ├── BizStudentMapper.java        # 学生 Mapper
│       │   │   │   ├── SysPermissionMapper.java     # 权限 Mapper
│       │   │   │   ├── SysRoleMapper.java           # 角色 Mapper
│       │   │   │   ├── SysRolePermissionMapper.java # 角色权限 Mapper
│       │   │   │   ├── SysTenantMapper.java         # 租户 Mapper
│       │   │   │   └── SysUserMapper.java           # 用户 Mapper
│       │   │   └── service/             # 服务层
│       │   │       ├── AuthService.java             # 认证服务接口
│       │   │       ├── PermissionService.java       # 权限服务接口
│       │   │       ├── RoleService.java             # 角色服务接口
│       │   │       ├── StudentService.java          # 学生服务接口
│       │   │       ├── TenantService.java           # 租户服务接口
│       │   │       ├── UserService.java             # 用户服务接口
│       │   │       └── impl/                        # 服务实现
│       │   │           ├── AuthServiceImpl.java
│       │   │           ├── PermissionServiceImpl.java
│       │   │           ├── RoleServiceImpl.java
│       │   │           ├── StudentServiceImpl.java
│       │   │           ├── TenantServiceImpl.java
│       │   │           └── UserServiceImpl.java
│       │   └── resources/
│       │       └── application.yml      # 应用配置
│       └── test/                        # 测试类
├── frontend/                            # Vue 3 前端
│   ├── package.json                     # NPM 配置
│   ├── vite.config.js                   # Vite 配置
│   ├── index.html                       # HTML 入口
│   └── src/
│       ├── main.js                      # 应用入口
│       ├── App.vue                      # 根组件
│       ├── api/                         # API 封装
│       │   ├── auth.js                  # 认证 API
│       │   ├── role.js                  # 角色 API
│       │   ├── student.js               # 学生 API
│       │   ├── tenant.js                # 租户 API
│       │   └── user.js                  # 用户 API
│       ├── directives/                  # 自定义指令
│       │   └── permission.js            # v-permission 权限指令
│       ├── router/                      # 路由
│       │   └── index.js                 # 路由配置 + 守卫
│       ├── store/                       # 状态管理
│       │   └── user.js                  # 用户状态 (Pinia)
│       ├── utils/                       # 工具
│       │   └── request.js               # Axios 封装
│       └── views/                       # 页面组件
│           ├── Dashboard.vue            # 首页
│           ├── Layout.vue               # 布局
│           ├── Login.vue                # 登录
│           ├── Role.vue                 # 角色管理
│           ├── Student.vue              # 学生管理
│           ├── Tenant.vue               # 租户管理
│           └── User.vue                 # 员工管理
├── docs/
│   └── init.sql                         # 数据库初始化脚本
├── QUICKSTART.md                        # 快速启动指南
├── TUTORIAL.md                          # 开发教程
└── README.md                            # 项目说明
```

---

## 5. 数据库设计

数据库名：`saaslearn`，字符集：`utf8mb4`，引擎：InnoDB。

### ER 关系图

```
sys_tenant (1) ──────< (N) sys_user
     │                        │
     │                        │ tenant_id
     │                        ↓
     │               (N) sys_role (1) ──< sys_role_permission >── sys_permission
     │                        ↑
     └────────────────────────┘

sys_tenant (1) ──────< (N) biz_student
```

### 系统表 (sys_)

#### sys_permission — 系统权限表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | 权限名称 |
| code | VARCHAR(100) | 权限编码，如 `student:list` |
| type | VARCHAR(20) | 权限类型：`menu`-菜单 / `button`-按钮 |
| parent_id | BIGINT | 父权限ID，0 为根节点 |
| sort | INT | 排序号 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

> **注意**：权限表无租户隔离，全局共享。

#### sys_role — 系统角色表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | 角色名称 |
| tenant_id | BIGINT | 租户ID，0 为平台角色 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT(1) | 逻辑删除：0-未删除，1-已删除 |

#### sys_role_permission — 角色权限关联表

| 字段 | 类型 | 说明 |
|------|------|------|
| role_id | BIGINT | 角色ID |
| permission_id | BIGINT | 权限ID |

> **注意**：联合主键 `(role_id, permission_id)`，无租户隔离。

#### sys_user — 系统用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(200) | BCrypt 加密密码 |
| role_id | BIGINT | 关联角色ID |
| tenant_id | BIGINT | 租户ID，超级账户为 0 |
| real_name | VARCHAR(50) | 真实姓名 |
| status | TINYINT(1) | 状态：1-启用，0-禁用 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT(1) | 逻辑删除 |

#### sys_tenant — 租户信息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键（即租户ID） |
| name | VARCHAR(100) | 租户名称 |
| code | VARCHAR(50) | 租户编码（唯一） |
| status | TINYINT(1) | 状态：1-启用，0-禁用 |
| admin_user_id | BIGINT | 关联管理员用户ID |
| admin_password | VARCHAR(50) | 管理员初始密码（明文） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 业务表 (biz_)

#### biz_student — 学生信息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| name | VARCHAR(50) | 学生姓名 |
| student_no | VARCHAR(50) | 学号 |
| grade | VARCHAR(20) | 年级 |
| phone | VARCHAR(20) | 联系电话 |
| tenant_id | BIGINT | 租户ID（数据隔离） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT(1) | 逻辑删除 |

### 租户隔离策略

| 表名 | 是否租户隔离 | 说明 |
|------|:----------:|------|
| sys_permission | ❌ | 全局共享，所有租户共用 |
| sys_role_permission | ❌ | 全局共享 |
| sys_tenant | ❌ | 全局共享，仅超级账户管理 |
| sys_user | 手动控制 | Service 层通过 `wrapper.eq(tenantId)` 过滤 |
| sys_role | 手动控制 | Service 层根据用户身份过滤 |
| biz_student | 手动控制 | Service 层根据用户身份过滤 |

> **设计说明**：虽然 MyBatis-Plus 多租户插件已配置，但 `sys_user`、`sys_role`、`biz_student` 被加入忽略表列表。原因是超级账户（tenant_id=0）需要查看所有租户的数据，而多租户插件的自动追加条件会阻止此行为。因此这三个表由 Service 层通过 `LambdaQueryWrapper` 手动控制租户过滤。

---

## 6. 后端模块详解

### 6.1 启动类

**`SaasFrameworkApplication`** — 应用入口

```java
@SpringBootApplication
@MapperScan("com.saas.framework.mapper")
public class SaasFrameworkApplication { ... }
```

- `@SpringBootApplication`：Spring Boot 自动配置
- `@MapperScan`：扫描 Mapper 接口所在的包
- 启动后输出 API 文档地址：`http://localhost:8080/swagger-ui.html`

### 6.2 公共模块 (common)

#### 6.2.1 Result — 统一响应封装

| 方法 | 返回码 | 说明 |
|------|--------|------|
| `Result.ok()` | 200 | 成功（无数据） |
| `Result.ok(T data)` | 200 | 成功（带数据） |
| `Result.ok(String msg, T data)` | 200 | 成功（带消息和数据） |
| `Result.error(String msg)` | 400 | 失败 |
| `Result.error(int code, String msg)` | 自定义 | 失败（自定义状态码） |
| `Result.unauth()` | 401 | 未登录 |
| `Result.forbidden()` | 403 | 权限不足 |
| `Result.notFound()` | 404 | 资源不存在 |
| `Result.serverError()` | 500 | 服务器内部错误 |

#### 6.2.2 RequirePermission — 权限校验注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();  // 权限编码，如 "student:list"
}
```

- 标记在 Controller 方法上
- 由 `PermissionAspect` AOP 切面自动拦截并校验
- 超级账户（tenant_id=0）自动放行

#### 6.2.3 PermissionAspect — 权限校验切面

| 方法 | 说明 |
|------|------|
| `checkPermission(ProceedingJoinPoint)` | 环绕通知，拦截 `@RequirePermission` 注解的方法 |

**逻辑流程**：
1. 判断当前用户是否为超级账户 → 是则直接放行
2. 获取注解上的权限编码
3. 从 `UserContext` 获取当前用户权限列表
4. 判断权限列表是否包含所需权限 → 不包含则抛出 `BusinessException(403)`

#### 6.2.4 TenantContext — 租户上下文

基于 `ThreadLocal<Long>` 存储当前请求的租户ID。

| 方法 | 说明 |
|------|------|
| `setTenantId(Long)` | 设置当前线程的租户ID |
| `getTenantId()` | 获取当前线程的租户ID |
| `remove()` | 清除 ThreadLocal，防止内存泄漏 |

> 由 `JwtAuthFilter` 在请求进入时设置，由 MyBatis-Plus 多租户插件读取。

#### 6.2.5 UserContext — 用户上下文

基于多个 `ThreadLocal` 存储当前登录用户信息。

| 方法 | 说明 |
|------|------|
| `setUserId(Long)` / `getUserId()` | 用户ID |
| `setUsername(String)` / `getUsername()` | 用户名 |
| `setTenantId(Long)` / `getTenantId()` | 租户ID |
| `setPermissions(List<String>)` / `getPermissions()` | 权限编码列表 |
| `isSuperAdmin()` | 判断是否为超级账户（tenant_id == 0） |
| `remove()` | 清除所有 ThreadLocal |

> 由 `JwtAuthFilter` 在请求进入时设置，请求结束后在 `finally` 块中清除。

#### 6.2.6 JwtUtil — JWT 工具类

| 方法 | 说明 |
|------|------|
| `generateToken(Long userId, String username, Long tenantId)` | 生成 JWT Token |
| `getClaimsFromToken(String token)` | 解析 Token 获取 Claims |
| `validateToken(String token)` | 验证 Token 是否有效（未过期） |
| `isTokenExpired(String token)` | 判断 Token 是否过期 |
| `getUserIdFromToken(String token)` | 从 Token 中获取用户ID |
| `getTenantIdFromToken(String token)` | 从 Token 中获取租户ID |
| `getUsernameFromToken(String token)` | 从 Token 中获取用户名 |

**Token 结构**：
- Claims: `{ userId, username, tenantId }`
- Subject: username
- 签名算法: HS512
- 过期时间: 2 小时（7200000ms）

#### 6.2.7 BusinessException — 业务异常

| 构造方法 | 说明 |
|---------|------|
| `BusinessException(String message)` | 默认 code=400 |
| `BusinessException(int code, String message)` | 自定义状态码 |

#### 6.2.8 GlobalExceptionHandler — 全局异常处理器

| 异常类型 | HTTP 状态码 | 说明 |
|---------|------------|------|
| `BusinessException` | 动态（与 code 一致） | 业务异常 |
| `MethodArgumentNotValidException` | 400 | @Valid 参数校验失败 |
| `BindException` | 400 | 参数绑定失败 |
| `ConstraintViolationException` | 400 | 约束校验失败 |
| `Exception` | 500 | 兜底异常 |

#### 6.2.9 DTO 类一览

| DTO 类 | 用途 | 关键字段 |
|--------|------|---------|
| `LoginRequest` | 登录请求 | username, password |
| `PageResult<T>` | 分页响应 | records, total, size, current |
| `RoleCreateRequest` | 创建/修改角色 | name, permissionIds |
| `RoleResponse` | 角色详情响应 | id, name, tenantId, permissionIds, createTime, updateTime |
| `StudentRequest` | 新增/修改学生 | name, studentNo, grade, phone |
| `TenantCreateRequest` | 创建租户 | name, code, adminUsername(可选), adminPassword(可选) |
| `UserCreateRequest` | 创建员工 | username, realName, roleId, password(可选) |
| `UserUpdateRequest` | 修改员工 | realName, roleId |

### 6.3 配置模块 (config)

#### 6.3.1 CorsConfig — 跨域配置

- 允许所有来源 (`addAllowedOriginPattern("*")`)
- 允许携带凭证 (`setAllowCredentials(true)`)
- 允许所有请求头和请求方法
- 预检请求缓存 3600 秒
- 仅对 `/api/**` 路径生效

#### 6.3.2 JwtAuthFilter — JWT 认证过滤器

**核心职责**：拦截所有 `/api/**` 请求，验证 JWT Token 并设置用户上下文。

**排除路径**（不需要认证）：
- `/api/auth/login`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/doc.html`
- `/webjars/**`

**处理流程**：
1. 判断请求路径是否在排除列表中 → 是则放行
2. 从 `Authorization` 请求头获取 Token → 缺失则返回 401
3. 验证 Token 有效性 → 无效则返回 401
4. 从 Token 解析 userId、tenantId、username
5. 查询用户确认存在且状态正常 → 异常则返回 401
6. 设置 `TenantContext` 和 `UserContext`
7. 查询用户权限列表并设置到 `UserContext`
8. 放行请求
9. 在 `finally` 块中清除所有 ThreadLocal

#### 6.3.3 FilterConfig — 过滤器注册

- 注册 `JwtAuthFilter`，拦截 `/api/*`，优先级 Order=1

#### 6.3.4 MyBatisPlusConfig — MyBatis-Plus 配置

**配置内容**：

1. **分页插件** (`PaginationInnerInterceptor`)
   - 数据库类型：MySQL
   - 使 `selectPage` 的 count 查询正常执行

2. **多租户插件** (`TenantLineInnerInterceptor`)
   - 租户字段名：`tenant_id`
   - 从 `TenantContext.getTenantId()` 获取当前租户ID
   - 忽略的表（不自动追加 tenant_id 条件）：
     - `sys_permission` — 全局共享
     - `sys_role_permission` — 全局共享
     - `sys_tenant` — 全局共享
     - `sys_user` — 登录时上下文未设置，需手动控制
     - `sys_role` — 超级账户需查看所有角色
     - `biz_student` — 超级账户需查看所有学生数据

3. **自动填充处理器** (`MetaObjectHandler`)
   - 插入时自动填充 `createTime` 和 `updateTime`
   - 更新时自动填充 `updateTime`

#### 6.3.5 SecurityConfig — 安全配置

- 提供 `BCryptPasswordEncoder` Bean，用于密码加密和验证

#### 6.3.6 DataInitializer — 数据初始化

实现 `CommandLineRunner`，应用启动时自动执行：

1. **初始化超级角色**（ID=1）
   - 角色名：超级管理员
   - tenant_id = 0
   - 自动分配所有权限

2. **初始化超级账户**
   - 用户名：admin
   - 密码：123456（BCrypt 加密）
   - 角色：超级管理员
   - tenant_id = 0

> 幂等操作：仅在数据不存在时初始化。

### 6.4 实体层 (entity)

| 实体类 | 对应表 | 主键策略 | 逻辑删除 | 自动填充 |
|--------|--------|---------|---------|---------|
| `SysUser` | sys_user | AUTO | ✅ deleted | createTime, updateTime |
| `SysRole` | sys_role | AUTO | ✅ deleted | createTime, updateTime |
| `SysPermission` | sys_permission | AUTO | ❌ | createTime, updateTime |
| `SysRolePermission` | sys_role_permission | 联合主键 | ❌ | ❌ |
| `SysTenant` | sys_tenant | AUTO | ❌ | createTime, updateTime |
| `BizStudent` | biz_student | AUTO | ✅ deleted | createTime, updateTime |

### 6.5 数据访问层 (mapper)

| Mapper 接口 | 继承 | 自定义方法 |
|-------------|------|-----------|
| `SysUserMapper` | `BaseMapper<SysUser>` | `selectByUsername(String)` — 按用户名查询（不区分租户） |
| `SysRoleMapper` | `BaseMapper<SysRole>` | 无 |
| `SysPermissionMapper` | `BaseMapper<SysPermission>` | 无 |
| `SysRolePermissionMapper` | `BaseMapper<SysRolePermission>` | `selectPermissionCodesByRoleId(Long)` — 查询权限编码列表 |
| | | `selectPermissionIdsByRoleId(Long)` — 查询权限ID列表 |
| | | `deleteByRoleId(Long)` — 删除角色的所有权限关联 |
| | | `batchInsert(Long, List<Long>)` — 批量插入角色权限关联 |
| `SysTenantMapper` | `BaseMapper<SysTenant>` | 无 |
| `BizStudentMapper` | `BaseMapper<BizStudent>` | 无 |

### 6.6 服务层 (service)

#### 6.6.1 AuthService / AuthServiceImpl — 认证服务

| 方法 | 说明 |
|------|------|
| `login(LoginRequest)` | 用户登录：验证用户名密码 → 检查用户状态 → 检查租户状态 → 生成 Token → 返回 token + userInfo |
| `getUserInfo()` | 获取当前用户信息：从 UserContext 获取 userId → 查询用户 → 查询权限列表 |

**登录返回数据结构**：
```json
{
  "token": "eyJhbGci...",
  "userInfo": {
    "id": 1,
    "username": "admin",
    "realName": "超级管理员",
    "roleId": 1,
    "tenantId": 0,
    "permissions": ["system", "tenant", "tenant:list", ...]
  }
}
```

#### 6.6.2 PermissionService / PermissionServiceImpl — 权限服务

| 方法 | 说明 |
|------|------|
| `getPermissionTree()` | 获取权限树：查询所有权限 → 租户管理员过滤自身权限范围 → 递归构建树形结构 |
| `checkPermissionsWithin(List<Long>)` | 校验权限范围：超级账户直接通过 → 获取当前用户权限 → 检查待分配权限是否在范围内 |

#### 6.6.3 RoleService / RoleServiceImpl — 角色服务

| 方法 | 说明 |
|------|------|
| `getById(Long)` | 查询角色详情（含权限ID列表） |
| `page(int, int)` | 分页查询：超级账户看所有角色，租户管理员只看本租户角色 |
| `create(RoleCreateRequest)` | 创建角色：校验权限范围 → 设置 tenantId → 保存角色和权限关联 |
| `update(Long, RoleCreateRequest)` | 修改角色：校验租户所有权 → 校验权限范围 → 更新名称和权限（先删后增） |
| `delete(Long)` | 删除角色：校验租户所有权 → 检查是否有用户使用 → 删除权限关联 → 逻辑删除 |

#### 6.6.4 TenantService / TenantServiceImpl — 租户服务

| 方法 | 说明 |
|------|------|
| `page(int, int)` | 分页查询租户列表（仅超级账户） |
| `create(TenantCreateRequest)` | 创建租户：检查编码唯一性 → 保存租户 → 创建默认角色 → 创建管理员用户 → 返回账号密码 |
| `updateStatus(Long, Integer)` | 修改租户状态（仅超级账户） |

**创建租户的完整流程**：
1. 检查租户编码是否已存在
2. 保存租户信息（`sys_tenant`）
3. 确定管理员用户名（默认 `code + "admin"`）和密码（默认随机6位）
4. 创建租户管理员角色（`sys_role`，tenant_id = 新租户ID）
5. 创建管理员用户（`sys_user`，关联角色和租户）
6. 更新租户的管理员信息
7. 返回 `{ tenantId, adminUsername, adminPassword }`

#### 6.6.5 UserService / UserServiceImpl — 用户服务

| 方法 | 说明 |
|------|------|
| `page(int, int, String)` | 分页查询本租户员工（超级账户不可操作） |
| `create(UserCreateRequest)` | 创建员工：检查用户名唯一 → 校验角色权限范围 → 自动设置 tenantId |
| `update(Long, UserUpdateRequest)` | 修改员工：校验租户归属 → 校验角色权限范围 |
| `resetPassword(Long)` | 重置密码为 123456 |
| `delete(Long)` | 逻辑删除员工 |

#### 6.6.6 StudentService / StudentServiceImpl — 学生服务

| 方法 | 说明 |
|------|------|
| `page(int, int, String, String)` | 分页查询：支持按姓名和学号模糊搜索，非超级账户自动过滤本租户数据 |
| `create(StudentRequest)` | 新增学生：自动填充 tenantId |
| `update(Long, StudentRequest)` | 修改学生：校验租户归属 |
| `delete(Long)` | 逻辑删除学生：校验租户归属 |

### 6.7 控制器层 (controller)

| 控制器 | 路径前缀 | 说明 | 权限要求 |
|--------|---------|------|---------|
| `AuthController` | `/api/auth` | 登录、获取用户信息 | login 无需认证 |
| `PermissionController` | `/api/permission` | 权限树查询 | `role:list` |
| `RoleController` | `/api/role` | 角色增删改查 | `role:list/add/edit/delete` |
| `StudentController` | `/api/student` | 学生增删改查 | `student:list/add/edit/delete` |
| `TenantController` | `/api/tenant` | 租户管理 | `tenant:list/add/edit` |
| `UserController` | `/api/user` | 员工增删改查 | `user:list/add/edit/delete` |

---

## 7. 前端模块详解

### 7.1 入口与全局配置

**`main.js`** — 应用入口

- 创建 Vue 3 应用
- 注册 Element Plus（含中文语言包）
- 注册 Pinia 状态管理
- 注册路由
- 注册自定义权限指令 `v-permission`

**`App.vue`** — 根组件

- 使用 `<router-view />` 渲染路由内容
- 设置全局基础样式

### 7.2 路由系统

**`router/index.js`** — 路由配置

| 路径 | 组件 | 权限要求 | 说明 |
|------|------|---------|------|
| `/login` | Login.vue | 无 | 登录页 |
| `/dashboard` | Dashboard.vue | 无 | 首页 |
| `/tenant` | Tenant.vue | `tenant:list` | 租户管理 |
| `/role` | Role.vue | `role:list` | 角色管理 |
| `/user` | User.vue | `user:list` | 员工管理 |
| `/student` | Student.vue | `student:list` | 学生管理 |

**路由守卫逻辑**：
1. 访问 `/login` → 已登录则跳转 `/dashboard`
2. 未登录访问其他页面 → 跳转 `/login`
3. 检查路由 `meta.permission` → 超级管理员放行，普通用户检查权限列表

### 7.3 状态管理

**`store/user.js`** — Pinia 用户状态

| 状态/方法 | 说明 |
|----------|------|
| `token` | JWT Token（持久化到 localStorage） |
| `userInfo` | 用户信息对象（持久化到 localStorage） |
| `isSuperAdmin` | 计算属性：是否为超级管理员（tenantId === 0） |
| `permissions` | 计算属性：用户权限列表 |
| `login(username, password)` | 登录并存储 Token 和用户信息 |
| `fetchUserInfo()` | 刷新用户信息 |
| `logout()` | 登出并清除所有存储 |

### 7.4 HTTP 请求封装

**`utils/request.js`** — Axios 实例

| 配置项 | 值 |
|--------|---|
| baseURL | `/api` |
| timeout | 15000ms |

**请求拦截器**：
- 自动从 localStorage 获取 Token
- 添加 `Authorization: Bearer {token}` 请求头

**响应拦截器**：
- `code !== 200` → 弹出错误消息，reject
- `401` → 清除 Token，跳转登录页
- `403` → 弹出"权限不足"
- `404` → 弹出"资源不存在"
- `500` → 弹出"服务器内部错误"
- 网络异常 → 弹出"网络异常"

### 7.5 API 模块

| API 模块 | 方法 | 请求路径 |
|---------|------|---------|
| `auth.js` | `login(data)` | POST `/auth/login` |
| | `getUserInfo()` | GET `/auth/info` |
| `tenant.js` | `getPage(params)` | GET `/tenant/page` |
| | `create(data)` | POST `/tenant` |
| | `updateStatus(id, status)` | PUT `/tenant/{id}/status` |
| `role.js` | `getPage(params)` | GET `/role/page` |
| | `getById(id)` | GET `/role/{id}` |
| | `create(data)` | POST `/role` |
| | `update(id, data)` | PUT `/role/{id}` |
| | `delete(id)` | DELETE `/role/{id}` |
| | `getPermissionTree()` | GET `/permission/tree` |
| `user.js` | `getPage(params)` | GET `/user/page` |
| | `create(data)` | POST `/user` |
| | `update(id, data)` | PUT `/user/{id}` |
| | `resetPassword(id)` | PUT `/user/{id}/reset-password` |
| | `delete(id)` | DELETE `/user/{id}` |
| `student.js` | `getPage(params)` | GET `/student/page` |
| | `create(data)` | POST `/student` |
| | `update(id, data)` | PUT `/student/{id}` |
| | `delete(id)` | DELETE `/student/{id}` |

### 7.6 自定义指令

**`directives/permission.js`** — `v-permission`

- 用法：`v-permission="'student:add'"`
- 判断当前用户是否拥有指定权限
- 超级管理员（tenantId === 0）始终显示
- 无权限时移除 DOM 元素

### 7.7 页面组件

| 组件 | 功能 |
|------|------|
| `Login.vue` | 登录页面：用户名/密码表单，调用登录 API |
| `Layout.vue` | 主布局：侧边栏导航 + 顶部栏 + `<router-view>` |
| `Dashboard.vue` | 首页：欢迎信息和系统概览 |
| `Tenant.vue` | 租户管理：分页表格 + 新增对话框 + 状态切换 |
| `Role.vue` | 角色管理：分页表格 + 新增/编辑对话框 + 权限树分配 |
| `User.vue` | 员工管理：分页表格 + 新增/编辑对话框 + 重置密码 |
| `Student.vue` | 学生管理：分页表格 + 新增/编辑对话框 + 搜索过滤 |

---

## 8. 核心机制详解

### 8.1 多租户隔离机制

```
请求进入
   │
   ▼
JwtAuthFilter 解析 Token → 获取 tenantId
   │
   ▼
TenantContext.setTenantId(tenantId)    ← ThreadLocal 存储
   │
   ▼
MyBatis-Plus TenantLineInnerInterceptor
   │
   ├── ignoreTable() 返回 true → 不追加条件（全局表）
   │
   └── ignoreTable() 返回 false → 自动追加 WHERE tenant_id = ?
       │
       ▼
   SQL 执行时自动拼接租户条件
```

**关键设计决策**：`sys_user`、`sys_role`、`biz_student` 被加入忽略表，原因是：
- 登录时 `TenantContext` 尚未设置，默认 tenant_id=0 会阻止租户用户登录
- 超级账户需要查看所有租户的数据
- 这三个表由 Service 层通过 `LambdaQueryWrapper` 手动控制租户过滤

### 8.2 RBAC 权限控制

```
用户 (SysUser)
  │
  │ role_id
  ▼
角色 (SysRole)
  │
  │ sys_role_permission
  ▼
权限 (SysPermission)
  │
  ├── type=menu   → 菜单权限（控制页面可见性）
  └── type=button → 按钮权限（控制操作权限）
```

**权限校验流程**：

```
Controller 方法标注 @RequirePermission("student:add")
   │
   ▼
PermissionAspect 环绕通知
   │
   ├── UserContext.isSuperAdmin() → true → 直接放行
   │
   └── UserContext.isSuperAdmin() → false
       │
       ├── 获取注解权限编码: "student:add"
       ├── 获取用户权限列表: UserContext.getPermissions()
       └── 判断是否包含 → 不包含则抛出 BusinessException(403)
```

**权限继承校验**（`PermissionService.checkPermissionsWithin`）：

```
租户管理员分配权限给下级角色/员工时：
   │
   ├── 超级账户 → 直接通过
   │
   └── 租户管理员
       │
       ├── 获取当前管理员的权限编码列表
       ├── 查询所有权限，筛选出管理员拥有的权限ID集合
       └── 逐个检查待分配权限是否在管理员权限范围内
           → 超出范围则抛出 BusinessException(403)
```

### 8.3 JWT 认证流程

```
1. 登录
   POST /api/auth/login { username, password }
      │
      ▼
   AuthServiceImpl.login()
      │
      ├── 查询用户 (SysUserMapper.selectByUsername)
      ├── BCrypt 校验密码
      ├── 检查用户状态
      ├── 检查租户状态
      ├── 生成 JWT Token (JwtUtil.generateToken)
      ├── 查询用户权限列表
      └── 返回 { token, userInfo }

2. 请求认证
   任意 /api/** 请求
      │
      ▼
   JwtAuthFilter.doFilter()
      │
      ├── 排除路径 → 放行
      ├── 获取 Authorization 头
      ├── 验证 Token 有效性
      ├── 解析 userId, tenantId, username
      ├── 查询用户确认存在
      ├── 设置 TenantContext + UserContext
      ├── 查询权限列表设置到 UserContext
      ├── chain.doFilter() → 进入 Controller
      └── finally → 清除所有 ThreadLocal
```

### 8.4 三层账户体系

```
┌─────────────────────────────────────────────────────────┐
│  超级账户 (tenant_id = 0)                                │
│  用户名: admin / 密码: 123456                            │
│  ┌─────────────────────────────────────────────────────┐│
│  │ 职责:                                               ││
│  │ • 管理所有租户（创建/禁用）                            ││
│  │ • 查看所有租户的数据                                  ││
│  │ • 拥有全部权限，自动放行                               ││
│  └─────────────────────────────────────────────────────┘│
│                         │                                │
│                    创建租户                               │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────────┐│
│  │  租户管理员 (tenant_id = 租户ID)                      ││
│  │  用户名: {code}admin / 密码: 随机6位或自定义           ││
│  │  ┌─────────────────────────────────────────────────┐││
│  │  │ 职责:                                           │││
│  │  │ • 管理本租户角色（创建/分配权限，不超过自身范围）    │││
│  │  │ • 管理本租户员工（创建/分配角色）                   │││
│  │  │ • 管理本租户业务数据                               │││
│  │  └─────────────────────────────────────────────────┘││
│  │                         │                            ││
│  │                    创建员工                            ││
│  │                         ▼                            ││
│  │  ┌─────────────────────────────────────────────────┐││
│  │  │  租户员工 (tenant_id = 租户ID)                    │││
│  │  │  密码: 默认 123456                               │││
│  │  │  职责:                                           │││
│  │  │  • 根据被分配的权限操作业务数据                      │││
│  │  │  • 无法管理其他用户或角色                           │││
│  │  └─────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

---

## 9. 请求生命周期

以"查询学生列表"为例，完整请求流程：

```
1. 前端发起请求
   axios.get('/api/student/page', { params: { page: 1, size: 10 } })
      │
      ▼
2. Vite 开发代理
   localhost:3000/api/student/page → localhost:8080/api/student/page
      │
      ▼
3. CorsFilter
   处理跨域请求
      │
      ▼
4. JwtAuthFilter
   ├── 验证 Token
   ├── 设置 TenantContext(tenantId)
   ├── 设置 UserContext(userId, username, tenantId, permissions)
   └── 放行
      │
      ▼
5. DispatcherServlet → StudentController.page()
      │
      ▼
6. PermissionAspect (AOP)
   ├── 检查 @RequirePermission("student:list")
   ├── 超级账户 → 放行
   └── 普通用户 → 检查 permissions.contains("student:list")
      │
      ▼
7. StudentServiceImpl.page()
   ├── 构建 LambdaQueryWrapper
   ├── 非超级账户: wrapper.eq(tenantId, 当前租户ID)
   └── selectPage()
      │
      ▼
8. MyBatis-Plus 执行
   ├── PaginationInnerInterceptor → 处理分页
   ├── TenantLineInnerInterceptor → biz_student 在忽略表，不追加条件
   └── 执行 SQL
      │
      ▼
9. 返回 Result<PageResult<BizStudent>>
      │
      ▼
10. JwtAuthFilter finally
    ├── TenantContext.remove()
    └── UserContext.remove()
      │
      ▼
11. 响应返回前端
    axios 响应拦截器 → 检查 code === 200 → 返回数据
```

---

## 10. 项目运行方式

### 环境要求

| 环境 | 版本要求 |
|------|---------|
| JDK | 11+ |
| Maven | 3.6+ |
| Node.js | 16+ |
| MySQL | 8.0+ |

### 后端启动

```bash
# 1. 初始化数据库
mysql -u root -p < docs/init.sql

# 2. 修改数据库配置（如需要）
# 编辑 backend/src/main/resources/application.yml
# 修改 spring.datasource.url/username/password

# 3. 编译并启动
cd backend
mvn spring-boot:run

# 或打包后运行
mvn clean package -DskipTests
java -jar target/saas-framework-1.0.0.jar
```

启动成功后：
- 后端地址：`http://localhost:8080`
- API 文档：`http://localhost:8080/swagger-ui.html`

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 或构建生产版本
npm run build
```

启动成功后：
- 前端地址：`http://localhost:3000`
- API 请求通过 Vite 代理转发到后端 8080 端口

### 配置说明

**后端配置** (`application.yml`)：

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8080 | 服务端口 |
| `spring.datasource.url` | jdbc:mysql://localhost:3306/saaslearn | 数据库地址 |
| `spring.datasource.username` | root | 数据库用户名 |
| `spring.datasource.password` | 123456 | 数据库密码 |
| `jwt.secret` | saas-framework-jwt-secret-key-... | JWT 签名密钥 |
| `jwt.expiration` | 7200000 | Token 过期时间（2小时） |
| `mybatis-plus.configuration.log-impl` | StdOutImpl | SQL 日志输出 |

**前端配置** (`vite.config.js`)：

| 配置项 | 值 | 说明 |
|--------|---|------|
| `server.port` | 3000 | 开发服务器端口 |
| `server.proxy./api.target` | http://localhost:8080 | API 代理目标 |
| `resolve.alias.@` | src 目录 | 路径别名 |

---

## 11. API 接口一览

### 认证管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| POST | `/api/auth/login` | 用户登录 | ❌ | — |
| GET | `/api/auth/info` | 获取当前用户信息 | ✅ | — |

### 租户管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/api/tenant/page` | 分页查询租户 | ✅ | tenant:list |
| POST | `/api/tenant` | 创建租户 | ✅ | tenant:add |
| PUT | `/api/tenant/{id}/status` | 修改租户状态 | ✅ | tenant:edit |

### 角色管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/api/role/{id}` | 查询角色详情 | ✅ | role:list |
| GET | `/api/role/page` | 分页查询角色 | ✅ | role:list |
| POST | `/api/role` | 新增角色 | ✅ | role:add |
| PUT | `/api/role/{id}` | 修改角色 | ✅ | role:edit |
| DELETE | `/api/role/{id}` | 删除角色 | ✅ | role:delete |

### 权限管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/api/permission/tree` | 获取权限树 | ✅ | role:list |

### 员工管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/api/user/page` | 分页查询员工 | ✅ | user:list |
| POST | `/api/user` | 新增员工 | ✅ | user:add |
| PUT | `/api/user/{id}` | 修改员工 | ✅ | user:edit |
| PUT | `/api/user/{id}/reset-password` | 重置密码 | ✅ | user:edit |
| DELETE | `/api/user/{id}` | 删除员工 | ✅ | user:delete |

### 学生管理

| 方法 | 路径 | 说明 | 认证 | 权限 |
|------|------|------|:----:|------|
| GET | `/api/student/page` | 分页查询学生 | ✅ | student:list |
| POST | `/api/student` | 新增学生 | ✅ | student:add |
| PUT | `/api/student/{id}` | 修改学生 | ✅ | student:edit |
| DELETE | `/api/student/{id}` | 删除学生 | ✅ | student:delete |

---

## 12. 默认账户与初始数据

### 默认账户

| 用户名 | 密码 | 角色 | tenant_id |
|--------|------|------|-----------|
| admin | 123456 | 超级管理员 | 0 |

> 超级账户和超级角色由 `DataInitializer` 在应用启动时自动创建（幂等操作）。

### 初始权限数据

由 `init.sql` 插入，权限树结构如下：

```
系统管理 (system)
  ├── 租户管理 (tenant)
  │     ├── 租户列表 (tenant:list)
  │     ├── 创建租户 (tenant:add)
  │     ├── 编辑租户 (tenant:edit)
  │     └── 删除租户 (tenant:delete)
  ├── 角色管理 (role)
  │     ├── 角色列表 (role:list)
  │     ├── 创建角色 (role:add)
  │     ├── 编辑角色 (role:edit)
  │     └── 删除角色 (role:delete)
  └── 员工管理 (user)
        ├── 员工列表 (user:list)
        ├── 创建员工 (user:add)
        ├── 编辑员工 (user:edit)
        └── 删除员工 (user:delete)
业务管理 (business)
  └── 学生管理 (student)
        ├── 学生列表 (student:list)
        ├── 添加学生 (student:add)
        ├── 编辑学生 (student:edit)
        └── 删除学生 (student:delete)
```

---

> 文档生成时间：2026-05-09 | 基于 saas-framework v1.0.0
