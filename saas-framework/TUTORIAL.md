# 开发教程 —— 以学生管理为例

本教程以「学生管理」模块为例，逐步演示如何在 SaaS 多租户框架中开发一个新的业务模块。

## 目录

1. [创建数据库表](#1-创建数据库表)
2. [后端开发](#2-后端开发)
3. [前端开发](#3-前端开发)
4. [分配权限](#4-分配权限)
5. [测试验证](#5-测试验证)

---

## 1. 创建数据库表

在 `docs/init.sql` 中添加建表语句（已有 `biz_student` 表，此处以它为例讲解）：

```sql
CREATE TABLE biz_student (
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name        VARCHAR(50) NOT NULL                 COMMENT '学生姓名',
    student_no  VARCHAR(50) NOT NULL                 COMMENT '学号',
    grade       VARCHAR(20) NOT NULL                 COMMENT '年级',
    phone       VARCHAR(20) DEFAULT NULL             COMMENT '联系电话',
    tenant_id   BIGINT      NOT NULL                 COMMENT '租户ID，数据隔离',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT(1)  NOT NULL DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';
```

**关键点：**
- 业务表前缀使用 `biz_`
- 必须包含 `tenant_id` 列并建立索引
- 必须包含 `deleted` 逻辑删除字段
- 每个字段都有中文注释

---

## 2. 后端开发

### 2.1 创建实体类 Entity

```java
// backend/src/main/java/com/saas/framework/entity/BizStudent.java
package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_student")
public class BizStudent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String studentNo;
    private String grade;
    private String phone;
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

**关键点：**
- `@TableName` 指定表名
- `@TableLogic` 标记逻辑删除字段
- `@TableField(fill = ...)` 配置自动填充

### 2.2 创建 Mapper 接口

```java
// backend/src/main/java/com/saas/framework/mapper/BizStudentMapper.java
package com.saas.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.framework.entity.BizStudent;

public interface BizStudentMapper extends BaseMapper<BizStudent> {
}
```

多租户插件会自动在查询时追加 `tenant_id = ?` 条件，无需手动编写。

### 2.3 创建 DTO

```java
// backend/src/main/java/com/saas/framework/common/dto/StudentRequest.java
package com.saas.framework.common.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class StudentRequest {
    @NotBlank(message = "学生姓名不能为空")
    private String name;
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotBlank(message = "年级不能为空")
    private String grade;
    private String phone;
}
```

### 2.4 创建 Service 接口和实现

```java
// Service 接口
public interface StudentService {
    IPage<BizStudent> page(int page, int size, String name, String studentNo);
    void create(StudentRequest request);
    void update(Long id, StudentRequest request);
    void delete(Long id);
}

// Service 实现（关键代码）
@Service
@Slf4j
public class StudentServiceImpl implements StudentService {

    @Resource
    private BizStudentMapper bizStudentMapper;

    @Override
    public void create(StudentRequest request) {
        BizStudent student = new BizStudent();
        // ... 属性复制
        // 自动填充租户ID
        student.setTenantId(TenantContext.getTenantId());
        bizStudentMapper.insert(student);
        log.info("新增学生: id={}, tenantId={}", student.getId(), student.getTenantId());
    }
}
```

**关键点：**
- 新增数据时手动设置 `tenant_id`（多租户插件也会自动处理）
- 查询时多租户插件自动过滤，无需手动添加条件
- 使用 `@Slf4j` 打印关键操作日志

### 2.5 创建 Controller

```java
@Slf4j
@RestController
@RequestMapping("/api/student")
@Tag(name = "学生管理")
public class StudentController {

    @Resource
    private StudentService studentService;

    @GetMapping("/page")
    @RequirePermission("student:list")  // 权限注解
    public Result<PageResult<BizStudent>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String studentNo) {
        log.info("查询学生列表: page={}", page);
        IPage<BizStudent> iPage = studentService.page(page, size, name, studentNo);
        return Result.ok(PageResult.of(iPage));
    }

    @PostMapping
    @RequirePermission("student:add")
    public Result<?> create(@Valid @RequestBody StudentRequest request) {
        studentService.create(request);
        return Result.ok("学生添加成功");
    }
    // ... update, delete 类似
}
```

**关键点：**
- 使用 `@RequirePermission` 注解控制访问权限
- 使用 `@Valid` + JSR303 进行参数校验
- 统一返回 `Result<T>` 格式
- 分页使用 `PageResult.of()` 转换

---

## 3. 前端开发

### 3.1 创建 API 封装

```js
// frontend/src/api/student.js
import request from '@/utils/request'

export const studentApi = {
  page(params) { return request.get('/student/page', { params }) },
  create(data) { return request.post('/student', data) },
  update(id, data) { return request.put(`/student/${id}`, data) },
  delete(id) { return request.delete(`/student/${id}`) }
}
```

### 3.2 创建页面组件

页面组件需要包含以下元素（完整代码见 `frontend/src/views/Student.vue`）：

1. **搜索栏**：支持按姓名、学号筛选
2. **数据表格**：展示学生列表，使用 `el-table`
3. **分页组件**：使用 `el-pagination`
4. **新增/编辑弹窗**：使用 `el-dialog` + `el-form`，带表单验证
5. **权限按钮**：使用 `v-permission="'student:add'"` 控制按钮显示

### 3.3 添加路由

在 `frontend/src/router/index.js` 中添加：

```js
{
  path: 'student',
  name: 'Student',
  component: () => import('@/views/Student.vue'),
  meta: { title: '学生管理', permission: 'student:list' }
}
```

---

## 4. 分配权限

1. 使用超级账户 admin 登录
2. 进入「角色管理」
3. 编辑角色，在权限树中勾选「学生管理」相关的权限：
   - student:list（学生列表）
   - student:add（添加学生）
   - student:edit（编辑学生）
   - student:delete（删除学生）
4. 保存后，拥有该角色的用户即可访问学生管理模块

---

## 5. 测试验证

### 5.1 运行单元测试

```bash
cd backend
mvn test -Dtest=StudentServiceTest
mvn test -Dtest=StudentControllerTest
```

### 5.2 功能验证步骤

1. 创建两个租户（如「学校A」和「学校B」）
2. 分别给两个租户管理员分配包含 student 权限的角色
3. 用租户A管理员登录，添加一些学生
4. 用租户B管理员登录，添加另一些学生
5. 验证：每个租户管理员只能看到自己租户的学生数据
6. 用超级账户 admin 登录，可以看到所有租户的学生数据

---

## 总结

添加一个新业务模块的步骤：

1. 创建数据库表（含 `tenant_id`、`deleted`）
2. 创建 Entity → Mapper → DTO → Service → Controller
3. 在 Controller 方法上添加 `@RequirePermission` 注解
4. 创建前端 API 封装和页面组件
5. 添加路由配置
6. 在数据库中插入对应权限数据
7. 为角色分配权限

多租户隔离和权限控制由框架自动处理，业务代码只需关注核心逻辑。
