# SaaS 多租户教学框架

一个完整的 SaaS 多租户教学框架，演示多租户数据隔离、RBAC 权限控制、三层账户体系等核心概念。

## 目录结构

```
saas-framework/
├── backend/                     # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/saas/framework/
│       │   ├── common/          # 公共类（注解、切面、上下文、异常、工具类、DTO）
│       │   ├── config/          # 配置类（MyBatis-Plus、安全、跨域、JWT过滤器、数据初始化）
│       │   ├── controller/      # 控制器层
│       │   ├── entity/          # 实体类
│       │   ├── mapper/          # MyBatis-Plus Mapper
│       │   └── service/         # 服务层（接口+实现）
│       └── test/java/           # 测试类
├── frontend/                    # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── api/                 # API 封装
│       ├── directives/          # 自定义指令（v-permission）
│       ├── router/              # 路由配置
│       ├── store/               # Pinia 状态管理
│       ├── utils/               # 工具（axios 封装）
│       └── views/               # 页面组件
├── docs/
│   └── init.sql                 # 数据库初始化脚本
├── QUICKSTART.md                # 快速启动指南
├── TUTORIAL.md                  # 开发教程
└── README.md                    # 本文件
```

## 核心设计思想

### 1. 多租户隔离

所有业务表包含 `tenant_id` 字段，通过 MyBatis-Plus 多租户插件自动在 SQL 中追加 `tenant_id = ?` 条件，实现数据隔离。超级账户（tenant_id=0）不受隔离限制。

### 2. 权限继承

- 超级账户拥有全部权限，可为租户管理员分配角色
- 租户管理员只能将**自己不超出范围**的权限分配给下级角色和员工
- 后端 `PermissionService.checkPermissionsWithin()` 方法统一校验

### 3. 三层账户体系

```
超级账户 (tenant_id=0)
  └── 创建租户 + 租户管理员
       └── 租户管理员创建本租户员工
            └── 员工操作业务数据（如学生管理）
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7.x |
| ORM | MyBatis-Plus 3.5.x + 多租户插件 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT + BCrypt |
| 前端框架 | Vue 3 + Vite |
| UI 组件 | Element Plus |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios |
| API 文档 | springdoc-openapi-ui |

## 默认账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 超级管理员 |
