# 登录与统一登录接入实现文档

## 功能目标

该功能负责完成两类登录流程：

- 学生账号登录
- 管理员账号登录

同时，前端支持从统一登录系统携带参数回跳，把外部 SSO 信息写入本地状态后直接进入系统。

## 前端入口

### 路由入口

- 路径：`/login`
- 文件：`low-carbon-dormitory-vue/src/login.vue`

### 当前界面形态

当前登录页采用双栏布局：

- 左侧突出“低碳宿舍管理平台”的系统定位，围绕学生端与管理端两个角色展示主要使用场景。
- 右侧为登录表单区域，包含学生/管理员角色切换、账号密码输入与快捷入口按钮。
- 整体沿用绿色主色与浅金色点缀，没有改变现有品牌色方向，只优化了层次、留白和信息聚焦方式。

### 路由守卫

- 文件：`low-carbon-dormitory-vue/src/router/router.ts`

路由守卫负责：

- 判断页面是否需要学生登录或管理员登录
- 处理登录页的“已登录自动跳转”
- 读取并消费 SSO 参数
- 未登录时跳转统一登录地址

### 本地登录状态

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
- 构造带 `role` 参数的登录地址
- 执行 `window.location.assign` 或 `window.location.replace`

## 前端请求链路

前端请求实例定义在 `low-carbon-dormitory-vue/src/api/http.ts`。

当前 API 基址规则为：

- 开发环境默认使用 `/api/dorm`
- 生产环境默认使用空字符串
- 也可以通过 `VITE_API_BASE_URL` 显式覆盖

这套规则的目的，是让本地开发继续复用 Vite 代理，而生产环境在没有代理时也能直接请求后端根路径，避免把 `/api/dorm` 误带到线上接口地址里。

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

- 学生模式会写入学生 store，并清理管理员 store
- 管理员模式会写入管理员 store，并清理学生 store
- 最后会移除 URL 中的 SSO 参数，避免刷新后重复处理

## 部署注意事项

后端登录接口实际暴露的是：

- `POST /student/login`
- `POST /admin/login`

如果生产环境前端仍请求 `/api/dorm/student/login` 或 `/api/dorm/admin/login`，则必须在网关或 Nginx 中提供 `/api/dorm/**` 到 Spring Boot 根路径的反向代理；否则浏览器会直接收到 `404`。

## 后端入口

### 学生登录接口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentAuthenticationService.java`

### 管理员登录接口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminAuthenticationController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

### Token 服务

- 文件：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/auth/TokenService.java`

## 后端实现细节

### 学生登录实现

`StudentAuthenticationService.login(...)` 的主要逻辑：

1. 读取用户名和密码
2. 查询 `student_base`
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

`TokenService` 手工实现了类 JWT 的三段式 token：

- Header
- Payload
- Signature

关键 claims 包括：

- `role`
- `sub`
- `iat`
- `exp`

学生 token 额外包含：

- `stuNum`

管理员 token 额外包含：

- `adminId`
- `username`

## 鉴权拦截现状

### 拦截器定义

- `config/AuthTokenInterceptor.java`

拦截器具备：

- Bearer token 提取
- token 校验
- 角色判断

### 实际注册情况

- `config/WebCorsConfig.java`

当前只注册到了：

- `/admin/**`

尚未统一注册到：

- `/student/**`

这意味着前端把很多学生页面视为“需要登录”，但后端并没有通过统一拦截器强制所有学生接口必须带 token。

## 维护注意点

- 如果后续继续调整登录页布局，保留左右双栏和角色切换逻辑一致即可，不要改动登录成功后的状态写入链路。
- 如果修改 `StudentProfile` 结构，需要同步检查 `student-token.ts` 持久化逻辑以及 SSO 回填逻辑。
- 如果新增 SSO 字段，需要同步更新 `router.ts` 中的 `ssoKeys` 清理列表。
- 如果权限模型收紧，需要同时修改前端路由守卫与后端拦截注册范围，避免继续出现“页面需要登录、接口未必需要登录”的差异。
