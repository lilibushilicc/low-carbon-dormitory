# 登录与注册实现说明

## 功能范围

当前认证相关能力包括：

- 学生账号登录
- 管理员账号登录
- 学生公开注册
- 管理员代建学生账号

其中：

- 登录页仅提供学生登录和管理员登录
- 没有在前端登录页新增“注册学生”入口
- 学生公开注册主要供外部系统通过接口调用

## 前端入口

### 登录页

- 路径：`/login`
- 文件：`low-carbon-dormitory-vue/src/login.vue`
- 全局布局约束：未登录时不渲染学生端左侧导航栏；学生侧导航仅在“学生路由 + 已登录 + 非移动端”条件下显示

### 登录 API

- 学生登录前端调用：`low-carbon-dormitory-vue/src/api/modules/student.ts`
- 管理员登录前端调用：`low-carbon-dormitory-vue/src/api/modules/admin.ts`

实际请求接口：

- `POST /student/login`
- `POST /admin/login`

## 后端接口

### 学生登录

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentAuthenticationService.java`
- DTO：`StudentLoginRequest`

### 学生公开注册

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentRegistrationService.java`
- DTO：`StudentRegisterRequest`

接口：

- `POST /student/register`

### 管理员登录

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminAuthenticationController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- DTO：`AdminLoginRequest`

### 管理员建学生

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- 共享建档服务：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentRegistrationService.java`

接口：

- `POST /admin/students`

## 公开注册与管理员建档的关系

两者共享同一套建档核心逻辑：

- 学号去重
- 查找或创建宿舍
- 写入学生基础信息
- 初始化宿舍费用账户
- 同步宿舍可用床位

区别：

- 管理员建档允许传入初始 `carbonScore`
- 公开注册固定将初始碳积分写为 `0`
- 前端管理页继续走原有 `/admin/students`
- 外部系统可直接调用 `/student/register`

## 权限边界

- `/admin/**` 仍然要求管理员身份
- `/student/register` 为公开接口，不要求管理员 token
- `POST /student/login` 仍然只负责登录，不负责注册

## 外部系统调用建议

外部系统如需注册学生，应直接请求：

- `POST /student/register`

注册成功后，再请求：

- `POST /student/login`

这样能保持“注册”和“登录”职责分离，避免把登录接口做成隐式建档入口。
