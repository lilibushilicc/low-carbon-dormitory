# 登录与统一登录接入实现说明

## 功能目标

当前登录体系覆盖两类入口：

- 学生账号登录
- 管理员账号登录

同时支持统一登录系统携带参数回跳，在前端写入本地登录状态后直接进入系统。

## 前端入口

### 登录页

- 路径：`/login`
- 文件：`low-carbon-dormitory-vue/src/login.vue`

### 路由守卫

- 文件：`low-carbon-dormitory-vue/src/router/router.ts`

当前守卫行为保持如下：

- 访问 `/` 时重定向到 `/login`
- 未登录访问学生端受保护页面时，跳回 `/login`
- 未登录访问管理端受保护页面时，跳回 `/login`
- 已登录用户访问 `/login` 时，按角色自动跳转到默认首页
- 路由中如带有 SSO 参数，会先写入本地状态，再清理 URL 参数

这意味着“先进入登录页”仅针对未登录访问成立，不会强制已登录用户重复登录。

## 本地登录状态

学生端状态存储：

- `low-carbon-dormitory-vue/src/stores/student-token.ts`

管理员状态存储：

- `low-carbon-dormitory-vue/src/stores/admin-token.ts`

学生端持久化内容：

- `studentToken`
- `studentStuNum`
- `dormId`
- `studentInfo`

管理端持久化内容：

- `adminToken`
- `adminProfile`

## 前端请求链路

HTTP 实例定义在：

- `low-carbon-dormitory-vue/src/api/http.ts`

当前基址策略：

- 开发环境默认使用 `/api/dorm`
- 生产环境默认直接请求后端根路径
- 可通过 `VITE_API_BASE_URL` 显式覆盖

Vite 代理定义在：

- `low-carbon-dormitory-vue/vite.config.ts`

开发环境代理规则：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`

## 登录流程

### 学生登录

1. 在 `login.vue` 中选择学生角色
2. 调用 `loginStudent(...)`
3. 实际请求 `POST /student/login`
4. 登录成功后写入学生 token 与学生资料
5. 跳转到学生默认首页

### 管理员登录

1. 在 `login.vue` 中选择管理员角色
2. 调用 `adminLogin(...)`
3. 实际请求 `POST /admin/login`
4. 登录成功后写入管理员 token 与管理员资料
5. 跳转到管理端默认首页

### SSO 登录

当前路由守卫会识别以下参数：

- `ssoMode`
- `ssoStudentInfo`
- `ssoStudentToken`
- `ssoStudentStuNum`
- `ssoAdminToken`
- `ssoAdminProfile`
- `ssoLoginUser`

识别后会：

- 将 SSO 身份信息写入对应 store
- 清理另一种角色的登录态
- 移除 URL 中的 SSO 参数，避免刷新后重复处理

## 部署注意事项

后端实际暴露的登录接口为：

- `POST /student/login`
- `POST /admin/login`

如果生产环境前端仍请求 `/api/dorm/student/login` 或 `/api/dorm/admin/login`，则必须在网关或 Nginx 中配置 `/api/dorm/**` 到 Spring Boot 根路径的反向代理；否则会出现 `404`。

## 维护说明

- 如后续仅调整登录页视觉样式，不应改动登录成功后的状态写入和跳转链路
- 如调整 `StudentProfile` 结构，需要同步检查学生端本地持久化和 SSO 回填逻辑
- 如新增 SSO 字段，需要同步更新 `router.ts` 中的参数清理列表
