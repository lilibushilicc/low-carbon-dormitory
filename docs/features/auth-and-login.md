# 登录与统一登录接入实现文档

## 功能目标

该功能负责完成两类登录流程：

- 学生账号登录
- 管理员账号登录

同时，前端还支持从统一登录系统携带参数回跳，把外部 SSO 信息写入本地状态后直接进入系统。

## 前端入口

### 路由入口

- `/`
- 文件：`low-carbon-dormitory-vue/src/login.vue`

### 路由守卫

- 文件：`low-carbon-dormitory-vue/src/router/router.ts`

路由守卫负责：

- 判断页面是否需要学生登录或管理员登录
- 处理登录页的“已登录自动跳转”
- 读取并消费 SSO 参数
- 未登录时跳转统一登录地址

### 本地登录态

- 学生 store：`low-carbon-dormitory-vue/src/stores/student-token.ts`
- 管理员 store：`low-carbon-dormitory-vue/src/stores/admin-token.ts`

学生 store 保存：

- `studentToken`
- `studentStuNum`
- `dormId`
- `studentInfo`

管理员 store 保存：

- `adminToken`
- `adminProfile`

### 统一登录工具

- 文件：`low-carbon-dormitory-vue/src/utils/unified-login.ts`

该工具负责：

- 解析统一登录基础地址
- 构造带 `redirect` 和 `role` 参数的登录地址
- 执行 `window.location.assign` 或 `replace`

## 前端请求链路

### 学生登录

1. `login.vue` 选择学生角色
2. 调用 `loginStudent(...)`
3. 接口定义在 `src/api/modules/student.ts`
4. 实际请求 `POST /student/login`
5. 登录成功后：
   - 清理管理员登录态
   - 清理 `loginUser`
   - 调用 `studentTokenStore.setStudentToken(...)`
   - 跳转学生首页或重定向地址

### 管理员登录

1. `login.vue` 选择管理员角色
2. 调用 `adminLogin(...)`
3. 接口定义在 `src/api/modules/admin.ts`
4. 实际请求 `POST /admin/login`
5. 登录成功后：
   - 清理学生登录态
   - 调用 `adminTokenStore.setAdminToken(...)`
   - 写入 `loginUser`
   - 跳转管理员首页或重定向地址

### SSO 登录

路由守卫会检查这些查询参数：

- `ssoMode`
- `ssoStudentInfo`
- `ssoStudentToken`
- `ssoStudentStuNum`
- `ssoAdminToken`
- `ssoAdminProfile`
- `ssoLoginUser`

当参数存在时：

- 学生模式会写入学生 store，并清掉管理员 store
- 管理员模式会写入管理员 store，并清掉学生 store
- 最后会移除 URL 中的 SSO 参数，避免刷新后重复处理

## 后端入口

### 学生登录接口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java`
- Service：`.../service/student/StudentAuthenticationService.java`

### 管理员登录接口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminAuthenticationController.java`
- Service：`.../service/admin/AdminManagementService.java`

### Token 服务

- 文件：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/auth/TokenService.java`

## 后端实现细节

### 学生登录实现

`StudentAuthenticationService.login(...)` 的主要逻辑：

1. 读取用户名和密码
2. 先查 `student_base`
3. 解析并补齐宿舍 ID
4. 校验密码
5. 调用 `TokenService.createStudentToken(stuNum)` 生成 token
6. 调用 `StudentDormService.buildStudentProfile(...)` 组装返回对象

### 管理员登录实现

`AdminManagementService.login(...)` 的主要逻辑：

1. 查询 `system_admin_account`
2. 校验账号状态是否可用
3. 校验密码
4. 调用 `TokenService.createAdminToken(...)`
5. 返回管理员基础资料

### Token 结构

`TokenService` 不是直接依赖第三方 JWT 库，而是手工实现了类似 JWT 的三段式 token：

- Header
- Payload
- Signature

关键 claims：

- `role`
- `sub`
- `iat`
- `exp`

学生 token 会额外带：

- `stuNum`

管理员 token 会额外带：

- `adminId`
- `username`

## 鉴权拦截现状

### 拦截器定义

- `config/AuthTokenInterceptor.java`

拦截器内部具备：

- Bearer token 提取
- token 校验
- 角色判断

### 实际注册情况

- `config/WebCorsConfig.java`

当前只注册到了：

- `/admin/**`

未统一注册到：

- `/student/**`

这意味着前端把很多学生页面视为“需要登录”，但后端并没有用统一拦截器强制所有学生接口必须带 token。

## 关键数据对象

前端：

- `StudentProfile`
- `AdminLoginPayload`

后端：

- `StudentLoginRequest`
- `StudentLoginResponse`
- `AdminLoginRequest`
- `AdminLoginResponse`
- `TokenPayload`

## 维护注意点

- 学生登录现在只依赖 `student_base` 当前表结构，后续改动登录字段时要同步检查资料拼装与 token 返回。
- 路由守卫和后端鉴权边界目前并不完全一致，修改权限模型时要同时看前后端。
- 统一登录参数清理是在前端完成的，如果后续增加 SSO 字段，需要同步更新 `router.ts` 中的 `ssoKeys`。
