# 快速启动指南

## 环境要求

| 工具 | 最低版本 |
|------|----------|
| JDK | 11+ |
| MySQL | 8.0+ |
| Maven | 3.6+ |
| Node.js | 16+ |
| npm | 8+ |

## 一、数据库初始化

1. 确保 MySQL 服务已启动
2. 使用 root 用户连接 MySQL，执行初始化脚本：

```bash
mysql -u root -p < docs/init.sql
# 输入密码: 123456
```

也可以在 Navicat 等工具中打开 `docs/init.sql` 直接执行。

脚本会自动：
- 创建数据库 `saaslearn`
- 创建所有表结构
- 插入默认权限数据

## 二、修改配置（可选）

如果需要修改数据库连接信息，编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/saaslearn?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root      # 修改为你的 MySQL 用户名
    password: 1234      # 修改为你的 MySQL 密码
```

## 三、启动后端

**推荐：IDEA 中直接启动**

1. 用 IDEA 打开 `backend/` 目录（或直接打开 `backend/pom.xml` 作为项目）
2. 找到 `src/main/java/com/saas/framework/SaasFrameworkApplication.java`
3. 右键 → `Run 'SaasFrameworkApplication'`

**或者命令行启动：**

```bash
cd backend
mvn spring-boot:run
```

启动成功后会打印 `SaaS 多租户教学框架启动成功！`。

应用启动时自动通过 `DataInitializer` 创建超级账户和数据。

API 文档：http://localhost:8080/swagger-ui.html

## 四、启动前端

**方式一：双击启动**

双击项目根目录的 `start-frontend.bat`（首次会自动安装依赖）。

**方式二：命令行启动**

```bash
cd frontend
npm install        # 首次运行需安装依赖
npm run dev
```

前端开发服务器：http://localhost:3000

开发模式下前端通过 Vite 代理将 `/api` 请求转发到后端 `localhost:8080`，无需额外配置跨域。

## 五、登录系统

浏览器访问 http://localhost:3000 ，使用默认超级账户：

| 用户名 | 密码 |
|--------|------|
| admin  | 123456 |

## 六、快速体验流程

1. **创建租户**：登录后进入「租户管理」→「创建租户」，输入名称和编码
2. **分配角色**：进入「角色管理」，编辑租户管理员角色，在权限树中勾选权限
3. **管理员工**：用租户管理员账号登录，进入「员工管理」创建员工并绑定角色
4. **业务操作**：进入「学生管理」体验 CRUD + 多租户隔离

## 常见问题

**Q: 启动后端报数据库连接失败？**
A: 检查 MySQL 是否启动，确认 `application.yml` 中的用户名密码正确，确认已执行 `docs/init.sql`。

**Q: 前端页面空白或接口报 500？**
A: 先确认后端已启动成功（访问 http://localhost:8080/swagger-ui.html 验证），再刷新前端。

**Q: 登录失败？**
A: 确认已执行 `docs/init.sql`，且后端启动日志中有 `超级账户创建成功！用户名: admin, 密码: 123456`。

**Q: IDEA 中 Maven 依赖下载慢？**
A: 在 IDEA 设置中检查 Maven 仓库镜像，或配置阿里云镜像加速。
