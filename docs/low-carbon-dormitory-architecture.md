# 低碳宿舍管理系统详细文档

> 文档范围：基于当前仓库代码生成，覆盖 `low-carbon-dormitory-vue` 与 `low-carbon-dormitory-spring` 两个子项目。  
> 文档日期：2026-05-08。  
> 说明：本文尽量只陈述代码中可以直接验证的事实；涉及部署方式、统一登录接入方式等内容时，会明确标注为“推断”。

---

## 1. 项目概览

### 1.1 项目目标

这是一个面向校园宿舍场景的“低碳宿舍管理系统”，把宿舍水电账务、低碳积分、宿舍排行、奖励兑换和管理员配置整合到同一套系统中。

从代码结构看，系统主要服务两类用户：

- 学生：登录后查看个人信息、宿舍水电余额、缴费历史、低碳看板、积分奖励、低碳规则。
- 管理员：登录后查看宿舍低碳总览、配置低碳规则、创建学生、管理奖励、对宿舍执行费用扣减。

### 1.2 仓库结构

仓库根目录下有两个主要子项目：

- `low-carbon-dormitory-vue`：Vue 3 前端。
- `low-carbon-dormitory-spring`：Spring Boot 后端。

此外还有一些辅助目录：

- `.github/java-upgrade/hooks/scripts`：与当前业务无直接关系的脚本文件。
- `low-carbon-dormitory-spring/tools`：后端未使用代码审计相关脚本与输出。

### 1.3 当前实现特征

从代码现状看，这个项目不是单纯的“宿舍缴费系统”，而是一个把以下几类能力叠加在一起的综合系统：

- 宿舍账户余额管理。
- 学生充值与账单历史。
- 低碳积分与排行计算。
- 宿舍/个人低碳看板。
- 奖励兑换。
- 管理端规则与库存维护。
- 当前代码以现有新表体系为唯一运行模型，应用直接连接既有 PostgreSQL 数据库运行，不再处理旧表兼容，也不再在启动时自动建表或灌初始化数据。

---

## 2. 技术栈

### 2.1 前端

- 运行时框架：Vue 3
- 语言：TypeScript
- 构建工具：Vite
- 路由：Vue Router
- 状态管理：Pinia
- 请求库：Axios
- UI 组件库：Element Plus、Ant Design Vue
- 图表：ECharts

相关入口文件：

- `low-carbon-dormitory-vue/package.json`
- `low-carbon-dormitory-vue/src/main.ts`
- `low-carbon-dormitory-vue/vite.config.ts`

### 2.2 后端

- 框架：Spring Boot 3.4.4
- 语言：Java 17
- ORM / 数据访问：MyBatis-Plus
- 数据库：PostgreSQL
- 参数校验：Spring Validation
- 测试：Spring Boot Test、MockMvc、JUnit 5

相关入口文件：

- `low-carbon-dormitory-spring/pom.xml`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/LowCarbonDormitoryApplication.java`
- `low-carbon-dormitory-spring/src/main/resources/application.properties`

### 2.3 接入与部署痕迹

- 前端构建基路径为 `/`，见 `low-carbon-dormitory-vue/vite.config.ts`。
- 前端同时支持统一登录跳转参数，见 `low-carbon-dormitory-vue/src/utils/unified-login.ts`。
- 前端开发时通过 Vite 代理把 `/api/dorm` 和 `/api` 转发到 `http://localhost:3000`。
- 后端默认运行在 `3000` 端口。

推断：

- 系统大概率需要挂在统一登录系统之后，支持携带 `redirect`、`role`、SSO 相关查询参数进入应用。

---

## 3. 顶层目录与职责划分

### 3.1 前端目录

`low-carbon-dormitory-vue` 的关键目录职责如下：

| 路径 | 职责 |
| --- | --- |
| `src/main.ts` | 创建 Vue 应用，挂载 Pinia 和 Router |
| `src/App.vue` | 根布局，按学生/管理员路由切换页面壳 |
| `src/router/router.ts` | 前端总路由、路由守卫、SSO 参数处理 |
| `src/api/http.ts` | Axios 实例、token 注入、基础路径处理 |
| `src/api/modules/*` | 按业务拆分的接口封装 |
| `src/stores/*` | 学生与管理员登录态 |
| `src/components/*` | 全局导航、系统横幅等共享组件 |
| `src/router/student-router/*` | 学生端页面 |
| `src/router/manager-router/*` | 管理员端页面 |
| `src/utils/*` | 登录跳转、路径解析、格式化、响应转换等工具函数 |
| `src/types/*` | TS 类型定义 |

### 3.2 后端目录

`low-carbon-dormitory-spring` 的关键目录职责如下：

| 路径 | 职责 |
| --- | --- |
| `controller/student` | 学生端接口入口 |
| `controller/admin` | 管理端接口入口 |
| `controller/publicapi` | 公开接口入口 |
| `service/student` | 学生业务服务 |
| `service/admin` | 管理员业务服务 |
| `service/dashboard` | 低碳看板聚合与计算 |
| `service/rule` | 低碳规则管理与预览 |
| `service/auth` | token 签发与校验 |
| `entity` | 数据库实体 |
| `mapper` | MyBatis-Plus 映射层 |
| `dto/request` | 请求对象 |
| `dto/response` | 响应对象 |
| `config` | CORS、拦截器等 Web 与运行时配置 |
| `common` | 统一返回体、异常、全局异常处理 |
| `src/test` | 集成测试与控制器测试 |

---

## 4. 前端架构

### 4.1 应用入口与布局

前端入口非常简单：

1. 在 `src/main.ts` 创建应用。
2. 注册 Pinia。
3. 注册 Router。
4. 挂载到 `#app`。

根组件 `src/App.vue` 根据当前路由决定是否使用学生端布局：

- 当路径不是 `/` 且不以 `/manager` 开头时，认为是学生端路由。
- 学生端使用 `StudentGlobalNav` 作为侧边导航。
- 管理端页面直接由各页面或壳组件负责布局。

### 4.2 路由结构

总路由定义在 `src/router/router.ts`，可分为三类：

- 登录页：`/`
- 学生端页面：平铺在根路径下
- 管理端页面：统一挂在 `/manager` 下

#### 4.2.1 学生端路由

| 路径 | 名称 | 页面文件 | 用途 |
| --- | --- | --- | --- |
| `/` | `login` | `src/login.vue` | 登录页 |
| `/index-student` | `index-student` | `student-router/home/index-student.vue` | 学生首页 |
| `/personal-info` | `personal-info` | `student-router/profile/personal-info.vue` | 个人信息 |
| `/pay-up` | `pay-up` | `student-router/billing/pay-up.vue` | 学生充值相关页面 |
| `/water-electricity` | `water-electricity` | `student-router/billing/water-electricity.vue` | 宿舍水电信息 |
| `/history-fee` | `history-fee` | `student-router/billing/history-fee.vue` | 费用历史 |
| `/low-carbon-dashboard` | `low-carbon-dashboard` | `student-router/dashboard/low-carbon-dashboard.vue` | 个人低碳看板 |
| `/reward-exchange` | `reward-exchange` | `student-router/reward/reward-exchange.vue` | 奖励兑换主页面 |
| `/reward-exchange-antd` | `reward-exchange-antd` | `student-router/reward/reward-exchange-antd.vue` | 奖励兑换的另一实现 |
| `/low-carbon-rule-readonly` | `low-carbon-rule-readonly` | `student-router/rules/low-carbon-rule-readonly.vue` | 学生只读规则页 |

说明：

- `reward-exchange-son.vue` 和 `use-reward-center.ts` 表明奖励页面内部有拆分逻辑。
- `low-carbon-dashboard-overview.vue` 同时被学生和管理员复用。

#### 4.2.2 管理端路由

| 路径 | 名称 | 页面文件 | 用途 |
| --- | --- | --- | --- |
| `/manager/home` | `manager-home` | `manager-router/home/manager-home.vue` | 管理首页 |
| `/manager/low-carbon-overview` | `manager-low-carbon-overview` | `student-router/dashboard/low-carbon-dashboard-overview.vue` | 宿舍低碳总览 |
| `/manager/low-carbon-overview/dorm/:dormId` | `manager-low-carbon-dorm-detail` | `manager-router/dashboard/manager-low-carbon-dorm-detail.vue` | 宿舍详情 |
| `/manager/low-carbon-rule-config` | `manager-low-carbon-rule-config` | `manager-router/config/low-carbon-rule-config.vue` | 低碳规则配置 |
| `/manager/student-create` | `manager-student-create` | `manager-router/student/student-create.vue` | 新增学生 |
| `/manager/reward-manage` | `manager-reward-manage` | `manager-router/reward/reward-manage.vue` | 奖励管理 |
| `/manager/dorm-fee-deduct` | `manager-dorm-fee-deduct` | `manager-router/fee/dorm-fee-deduct.vue` | 宿舍水电扣费 |

管理端的外层壳组件是：

- `src/router/manager-router/dorm/manager-dorm-shell.vue`

### 4.3 路由守卫与登录态

路由守卫逻辑集中在 `src/router/router.ts`。

它做了几件关键事情：

1. 为学生端和管理员端路由分别标记 `requiresStudentAuth` 与 `requiresAdminAuth`。
2. 从 URL 查询参数中读取 SSO 回跳信息：
   - `ssoMode`
   - `ssoStudentInfo`
   - `ssoStudentToken`
   - `ssoStudentStuNum`
   - `ssoAdminToken`
   - `ssoAdminProfile`
   - `ssoLoginUser`
3. 如果识别到 SSO 参数，会把登录态写入本地存储，然后清理 URL 查询参数。
4. 如果访问的是登录页且当前已经登录，会根据角色跳回默认首页或指定重定向路径。
5. 未登录访问受保护页面时，会跳转到统一登录地址。

### 4.4 登录态存储

学生与管理员各自有独立的 Pinia store。

#### 学生 store

文件：`src/stores/student-token.ts`

本地存储键：

- `studentToken`
- `studentStuNum`
- `dormId`
- `studentInfo`

核心职责：

- 维护学生 token。
- 维护当前学号与宿舍 ID。
- 缓存学生资料。
- 提供 `isLoggedIn` 与 `dormLabel` 等派生状态。

#### 管理员 store

文件：`src/stores/admin-token.ts`

本地存储键：

- `adminProfile`
- `adminToken`

核心职责：

- 维护管理员 token。
- 维护管理员基本资料。
- 提供 `isAdminLoggedIn` 派生状态。

### 4.5 请求层

Axios 实例定义在 `src/api/http.ts`。

关键行为：

- `baseURL` 在开发环境默认是 `/api/dorm`，生产环境默认是空字符串，也可以通过 `VITE_API_BASE_URL` 覆盖。
- 请求超时为 10 秒。
- 非登录请求会自动附加 `Authorization: Bearer <token>`。
- 如果访问的是 `/admin/*`，优先使用管理员 token。
- 如果访问的是学生接口，会优先用学生 token；没有学生 token 时会退回管理员 token。

这意味着：

- 管理员可能被允许访问部分学生接口。
- 前端本身支持“同一浏览器内两类 token 切换”，但实际页面逻辑仍以单角色登录为主。

### 4.6 页面导航

#### 学生端导航

文件：`src/components/student-global-nav.vue`

学生端导航大致分为三组：

- 顶部页面：
  - 学生首页
  - 个人信息
- 账务页面：
  - 宿舍水电费
- 低碳服务：
  - 个人低碳看板
  - 奖励兑换
  - 低碳规则

#### 管理端导航

文件：`src/components/admin-global-nav.vue`

管理端导航大致分为三组：

- 管理导航：
  - 返回管理首页
  - 宿舍总览
- 基础配置：
  - 规则配置
  - 新增学生
- 运营管理：
  - 奖励管理
  - 宿舍水电扣费

### 4.7 前端 API 模块

前端接口封装按业务拆分：

| 文件 | 主要内容 |
| --- | --- |
| `src/api/modules/student.ts` | 学生登录、个人信息、水电信息、缴费历史、充值与支付单 |
| `src/api/modules/admin.ts` | 管理员登录、水电单价、学生创建、奖励管理、扣费 |
| `src/api/modules/dashboard.ts` | 个人低碳看板与宿舍总览看板 |
| `src/api/modules/reward.ts` | 奖励中心与兑换 |
| `src/api/modules/low-carbon-rule.ts` | 管理员规则配置与学生只读规则 |

---

## 5. 后端架构

### 5.1 统一返回与异常处理

统一返回体在 `common/Result.java` 中定义：

- `code`
- `msg`
- `data`

返回风格：

- 成功固定为 `code = 200`
- 业务失败默认 `code = 500`
- 鉴权失败使用 `code = 401`

全局异常处理在 `common/GlobalExceptionHandler.java` 中定义，负责处理：

- `IllegalArgumentException`
- `IllegalStateException`
- `AuthException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`
- 兜底 `Exception`

### 5.2 配置层

#### CORS 与拦截器注册

文件：`config/WebCorsConfig.java`

当前实现：

- 允许所有来源、所有常见方法、所有请求头。
- 注册了 `AuthTokenInterceptor`。
- 实际只拦截 `/admin/**`。
- 显式放行 `/admin/login`。

这是一个很重要的事实：虽然 `AuthTokenInterceptor` 内部写了 `/student/*` 与 `/admin/*` 的角色判断逻辑，但它在 WebMvc 中只被注册到管理员路径上。

#### Token 校验

文件：`config/AuthTokenInterceptor.java`

拦截器能力：

- 读取 `Authorization` 请求头。
- 验证 Bearer token。
- 调用 `TokenService.verify(...)` 解析 token。
- 把 `authRole`、`authSubject`、`authClaims` 放入 `request` 属性。

#### 数据库模型约定

当前后端代码默认数据库已经具备现行的 `student_*` 与 `system_*` 表结构。  
应用启动阶段不再负责补表、补字段、灌种子数据或清理旧表，数据库结构与基础数据需要由既有数据库本身保证。

### 5.3 认证层

文件：`service/auth/TokenService.java`

该服务实现了一个自定义 token 机制，结构与 JWT 类似：

- Header：包含 `alg=HS256` 与 `typ=JWT`
- Payload：包含 `role`、`sub`、`iat`、`exp` 与附加 claims
- Signature：HMAC-SHA256

已实现的 token 类型：

- 学生 token：`createStudentToken(stuNum)`
- 管理员 token：`createAdminToken(adminId, username)`

默认配置来源：

- `app.auth.token-secret`
- `app.auth.token-ttl-seconds`

### 5.4 控制器层

#### 学生端接口

| 控制器 | 路径 | 主要接口 |
| --- | --- | --- |
| `StudentAuthenticationController` | `/student` | `POST /login` |
| `StudentProfileController` | `/student` | `GET /profile` |
| `StudentWaterElectricityController` | `/student` | `GET /water-electricity`、`POST /water-electricity/refresh` |
| `StudentFeeHistoryController` | `/student` | `GET /fee-history` |
| `StudentPaymentController` | `/student` | `POST /pay`、支付单创建/查询/模拟支付成功 |
| `StudentLowCarbonDashboardController` | `/student` | `GET /low-carbon-dashboard`、`GET /low-carbon-dashboard-personal` |
| `StudentLowCarbonRuleController` | `/student/low-carbon-rules` | `GET /` |
| `StudentRewardController` | `/student/rewards` | 奖励中心、补积分、兑换 |

#### 管理端接口

| 控制器 | 路径 | 主要接口 |
| --- | --- | --- |
| `AdminAuthenticationController` | `/admin` | `POST /login` |
| `AdminManagementController` | `/admin` | 水电单价、学生创建、奖励管理、宿舍扣费 |
| `LowCarbonRuleAdminController` | `/admin/low-carbon-rules` | 规则查询、更新、预览 |

#### 公开接口

| 控制器 | 路径 | 主要接口 |
| --- | --- | --- |
| `PublicStudentDashboardController` | `/public/student` | 公开宿舍看板、公开个人看板 |

### 5.5 服务层职责

#### 学生域服务

| 服务 | 职责 |
| --- | --- |
| `StudentAuthenticationService` | 学生登录与学生资料返回 |
| `StudentContextService` | 根据学号解析学生、宿舍与上下文 |
| `StudentDormService` | 宿舍解析、宿舍信息拼装、学生资料拼装、水电响应拼装 |
| `DormFeeAccountService` | 宿舍余额账户读写与加减 |
| `StudentPaymentService` | 充值、刷新水电、支付单、模拟支付成功、费用结算 |
| `StudentFeeHistoryService` | 分页查询缴费历史 |
| `RewardService` | 奖励中心、补积分、兑换 |
| `StudentPersonalLowCarbonDashboardService` | 构建个人低碳看板 |

#### 管理域服务

| 服务 | 职责 |
| --- | --- |
| `AdminManagementService` | 管理员登录、水电单价维护、宿舍扣费、学生创建、奖励 CRUD |

#### 低碳看板与规则服务

| 服务 | 职责 |
| --- | --- |
| `LowCarbonDashboardService` | 构建宿舍全局/分周期低碳看板 |
| `LowCarbonDashboardSupport` | 周期窗口、费用聚合、碳排换算、评分与排序辅助 |
| `LowCarbonRuleService` | 规则读取、只读规则、规则更新、公式预览、荣誉规则查询 |

---

## 6. 核心页面与接口映射

### 6.1 学生端页面到接口的关系

| 页面 | 主要接口 |
| --- | --- |
| 登录页 | `POST /student/login`、`POST /admin/login` |
| 个人信息 | `GET /student/profile` |
| 宿舍水电 | `GET /student/water-electricity`、`POST /student/water-electricity/refresh` |
| 费用历史 | `GET /student/fee-history` |
| 充值/支付 | `POST /student/pay`、`POST /student/payments/orders`、`GET /student/payments/orders/{orderNo}`、`POST /student/payments/orders/{orderNo}/simulate-success` |
| 个人低碳看板 | `GET /student/low-carbon-dashboard-personal`、`GET /student/low-carbon-dashboard` |
| 奖励兑换 | `GET /student/rewards`、`POST /student/rewards/exchange` |
| 低碳规则只读页 | `GET /student/low-carbon-rules` |

### 6.2 管理端页面到接口的关系

| 页面 | 主要接口 |
| --- | --- |
| 管理登录 | `POST /admin/login` |
| 宿舍总览 | `GET /student/low-carbon-dashboard` 或公开看板接口的复用逻辑 |
| 宿舍详情 | 看板数据组合查询 |
| 规则配置 | `GET /admin/low-carbon-rules`、`PUT /admin/low-carbon-rules`、`POST /admin/low-carbon-rules/preview` |
| 新增学生 | `POST /admin/students` |
| 奖励管理 | `GET /admin/rewards`、`POST /admin/rewards`、`PUT /admin/rewards/{rewardId}/stock`、`DELETE /admin/rewards/{rewardId}` |
| 宿舍扣费 | `GET /admin/utility-rates`、`PUT /admin/utility-rates/{feeType}`、`POST /admin/dorms/{dormId}/fees/deduct` |

---

## 7. 核心业务流

### 7.1 登录流程

#### 学生登录

学生登录流程由 StudentAuthenticationService.login(...) 驱动：

1. 从请求中读取 username 和 password。
2. 在 student_base 中按学号查学生。
3. 附加并解析宿舍 ID。
4. 校验密码。
5. 生成学生 token。
6. 使用 StudentDormService.buildStudentProfile(...) 组装学生资料。

这个流程说明：

- 登录已完全建立在当前新表体系上。
- 登录返回的学生资料依赖 student_base、student_profile 与宿舍信息表的现行结构。

#### 管理员登录

管理员登录流程由 `AdminManagementService.login(...)` 驱动：

1. 按用户名查 `system_admin_account`。
2. 校验状态是否启用。
3. 校验密码。
4. 签发管理员 token。
5. 返回 `adminId`、`username`、`displayName`、`token`。

### 7.2 水电查询与刷新流程

#### 查询水电

入口：`GET /student/water-electricity`

流程：

1. 接口允许通过 `stuNum` 或 `dormId` 查询。
2. 如果两个都没有，则报错。
3. 如果给了 `stuNum`，会尝试定位学生。
4. 通过 `StudentContextService.resolveRequiredDormId(...)` 解析最终宿舍 ID。
5. 通过 `DormFeeAccountService.getOrCreate(...)` 读取或创建宿舍余额账户。
6. 通过 `StudentDormService.buildWaterElectricityResponse(...)` 组装响应。

#### 刷新水电

入口：`POST /student/water-electricity/refresh`

核心逻辑在 `StudentPaymentService.refreshWaterElectricity(...)`：

1. 锁定宿舍费用账户。
2. 执行周期性扣费结算。
3. 找到水电最近一次锚点记录。
4. 写入两条 `REFRESH` 历史记录。
5. 根据最新结算结果给宿舍与个人补积分。
6. 返回刷新后的余额、水电和积分变化。

这说明“刷新”并不只是重新查一遍，而是一个带结算、副作用和积分更新的业务动作。

### 7.3 充值与支付流程

入口：

- `POST /student/pay`
- `POST /student/payments/orders`
- `GET /student/payments/orders/{orderNo}`
- `POST /student/payments/orders/{orderNo}/simulate-success`

流程可分两条：

#### 直接充值

`POST /student/pay` 直接调用 `StudentPaymentService.pay(...)`，本质上走 `applyRecharge(...)`：

1. 校验 `feeType` 和 `payType`。
2. 定位学生与宿舍。
3. 读取并锁定宿舍账户。
4. 更新余额。
5. 写入 `student_fee_history`。
6. 结算并补充低碳积分。
7. 返回最新宿舍账务结果。

#### 支付单模式

`POST /student/payments/orders` 会：

1. 创建一条状态为 `PENDING` 的订单。
2. 生成 `orderNo`。
3. 生成二维码内容与图片地址。
4. 把订单写入 `student_payment_order`。

`POST /student/payments/orders/{orderNo}/simulate-success` 会：

1. 校验订单当前状态必须是 `PENDING`。
2. 读取订单内容。
3. 调用实际充值逻辑。
4. 把订单更新为 `SUCCESS`。
5. 写入模拟三方流水号。

从代码看，这是一套“支付接入预留 + 本地模拟成功”的实现，而不是完整第三方支付接入。

### 7.4 低碳看板流程

#### 全局宿舍看板

入口：`GET /student/low-carbon-dashboard` 与 `GET /public/student/low-carbon-dashboard`

核心逻辑在 `LowCarbonDashboardService.buildDashboard(...)`：

1. 解析当前周期窗口。
2. 加载水电单价。
3. 读取当前生效的低碳评分规则。
4. 读取所有宿舍。
5. 读取费用历史。
6. 聚合最近充值周期或指定周期内的水电数据。
7. 计算电费、水费、用量、碳排、碳分。
8. 组装宿舍列表、楼栋列表、总览统计。
9. 计算排序与更新时间。

#### 个人低碳看板

入口：`GET /student/low-carbon-dashboard-personal` 与 `GET /public/student/low-carbon-dashboard-personal`

核心逻辑在 `StudentPersonalLowCarbonDashboardService.buildDashboard(...)`：

1. 定位当前学生与宿舍。
2. 读取全量学生、全量宿舍和费用历史。
3. 计算当前宿舍、同楼栋、同学院、全校的比较样本。
4. 计算宿舍锚点信息、总览、对比、建议与趋势。
5. 生成用于页面展示的比较文本和推荐信息。

这个服务承担了较多“展示层聚合”职责，返回结果已非常贴近前端页面结构。

### 7.5 低碳规则流程

#### 学生只读规则

入口：`GET /student/low-carbon-rules`

行为：

- 直接读取当前规则配置并返回只读视图。

#### 管理员规则配置

入口：

- `GET /admin/low-carbon-rules`
- `PUT /admin/low-carbon-rules`
- `POST /admin/low-carbon-rules/preview`

核心逻辑在 `LowCarbonRuleService`：

- 读取当前有效评分规则与荣誉规则。
- 更新规则配置。
- 根据传入的电费、水费和规则参数，预览碳排与得分。

### 7.6 奖励兑换流程

入口：

- `GET /student/rewards`
- `POST /student/rewards/exchange`
- `POST /student/rewards/supplement-points`

核心逻辑在 `RewardService`：

1. 查询学生与宿舍上下文。
2. 读取当前可用奖励列表。
3. 读取该学生最近兑换记录。
4. 返回奖励中心视图。

兑换时：

1. 校验奖励存在、启用且库存充足。
2. 校验学生积分足够。
3. 扣减学生积分。
4. 扣减奖励库存。
5. 写入兑换记录。
6. 返回剩余积分与剩余库存。

---

## 8. 数据模型与状态流

### 8.1 前端主要状态对象

前端最核心的业务状态对象包括：

- `StudentProfile`
- `StudentWaterElectricity`
- `FeeHistoryPage`
- `PaymentOrder`
- `PersonalDashboardData`
- `GlobalDashboardData`
- `RewardCenter`
- `LowCarbonRuleConfig`

这些类型主要定义在：

- `src/types/student.ts`
- `src/api/modules/student.ts`
- `src/api/modules/dashboard.ts`
- `src/api/modules/reward.ts`
- `src/api/modules/low-carbon-rule.ts`

### 8.2 后端核心实体

根据 `entity` 目录与服务使用情况，当前核心实体包括：

| 实体 | 含义 |
| --- | --- |
| `StudentBase` | 当前学生主表 |
| `StudentExt` | 学生扩展信息表映射 |
| `DormInfo` | 宿舍基础信息 |
| `DormFee` | 宿舍水电余额账户 |
| `DormFeeHistory` | 宿舍费用变动历史 |
| `UtilityRateConfig` | 水电单价配置 |
| `LowCarbonScoreRule` | 低碳评分规则 |
| `LowCarbonHonorRule` | 荣誉规则 |
| `RewardItem` | 奖励项 |
| `RewardExchangeRecord` | 奖励兑换记录 |
| `AdminAccount` | 管理员账户 |

### 8.3 关键数据表关系

从代码可以看出以下主要关系：

- `student_base.dorm_id -> student_dorm_info.dorm_id`
- `student_dorm_fee.dorm_id -> student_dorm_info.dorm_id`
- `student_reward_exchange.reward_id -> student_reward_item.reward_id`

业务上的隐含关系还包括：

- `student_fee_history` 用于支撑账单、余额刷新和低碳统计。
- `student_payment_order` 用于支撑支付单和模拟支付成功。
- `student_low_carbon_score_rule` 与 `student_low_carbon_honor_rule` 用于低碳得分和荣誉规则。

### 8.4 状态流说明

#### 登录态流

- 登录接口返回 token 与资料。
- 前端把 token 与资料写入 Pinia 和 `localStorage`。
- 路由守卫决定是否允许进入目标页面。
- 接口请求自动附带 token。

#### 宿舍账务流

- 查询余额读取 `student_dorm_fee`。
- 充值/扣费/刷新会写入 `student_fee_history`。
- 最新账务变化又会被低碳看板聚合逻辑消费。

#### 低碳积分流

- 宿舍账务变化触发积分补算。
- 个人积分存放在 `student_base.carbon_score`。
- 奖励兑换会消耗个人积分。

---

## 9. API 详细清单

### 9.1 学生端 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/student/login` | 学生登录 |
| `GET` | `/student/profile` | 查询学生资料 |
| `GET` | `/student/water-electricity` | 查询宿舍水电余额与用量 |
| `POST` | `/student/water-electricity/refresh` | 刷新宿舍水电与积分 |
| `GET` | `/student/fee-history` | 查询账单历史 |
| `POST` | `/student/pay` | 直接充值 |
| `POST` | `/student/payments/orders` | 创建支付单 |
| `GET` | `/student/payments/orders/{orderNo}` | 查询支付单 |
| `POST` | `/student/payments/orders/{orderNo}/simulate-success` | 模拟支付成功 |
| `GET` | `/student/low-carbon-dashboard` | 宿舍低碳看板 |
| `GET` | `/student/low-carbon-dashboard-personal` | 个人低碳看板 |
| `GET` | `/student/low-carbon-rules` | 学生只读低碳规则 |
| `GET` | `/student/rewards` | 奖励中心 |
| `POST` | `/student/rewards/supplement-points` | 补积分视图刷新 |
| `POST` | `/student/rewards/exchange` | 兑换奖励 |

### 9.2 管理端 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/admin/login` | 管理员登录 |
| `GET` | `/admin/utility-rates` | 查询水电单价 |
| `PUT` | `/admin/utility-rates/{feeType}` | 更新水电单价 |
| `POST` | `/admin/dorms/{dormId}/fees/deduct` | 扣减宿舍费用 |
| `POST` | `/admin/students` | 创建学生 |
| `GET` | `/admin/rewards` | 查询奖励列表 |
| `POST` | `/admin/rewards` | 新增奖励 |
| `PUT` | `/admin/rewards/{rewardId}/stock` | 更新奖励库存 |
| `DELETE` | `/admin/rewards/{rewardId}` | 删除奖励 |
| `GET` | `/admin/low-carbon-rules` | 查询规则 |
| `PUT` | `/admin/low-carbon-rules` | 更新规则 |
| `POST` | `/admin/low-carbon-rules/preview` | 预览规则计算结果 |

### 9.3 公开 API

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/public/student/low-carbon-dashboard` | 公开宿舍低碳看板 |
| `GET` | `/public/student/low-carbon-dashboard-personal` | 公开个人低碳看板 |

---

## 10. 鉴权与访问控制现状

### 10.1 前端视角

前端认为：

- 学生页面需要学生登录。
- 管理页面需要管理员登录。
- 未登录会跳统一登录地址。

### 10.2 后端视角

后端当前实际注册的拦截器只保护：

- `/admin/**`

这意味着：

- 管理员接口确实要求 token。
- 学生接口是否要求 token，要看各接口自身是否依赖 `stuNum`、`dormId` 等参数完成定位，而不是统一由拦截器强制。

### 10.3 测试体现出的开放能力

`src/test/java/com/example/lowcarbondormitory/StudentPublicAccessByStuNumTest.java` 明确验证了以下行为：

- 仅提供 `stuNum` 时可以访问 `/student/water-electricity`
- 仅提供 `stuNum` 时可以访问 `/student/rewards`
- 仅提供 `stuNum` 时可以访问 `/student/profile`
- 不带 token 的情况下访问奖励兑换接口，至少能进入业务逻辑而不是直接 401

这说明当前系统对“学生接口是否公开”采取的是较宽松策略。

如果这是设计选择，建议在文档和接口命名上明确为“公开学生查询接口”。  
如果这不是设计选择，则这是一个重要的安全风险。

---

## 11. 启动、构建与运行

### 11.1 前端

在 `low-carbon-dormitory-vue` 目录下：

```bash
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

可用脚本定义在 `low-carbon-dormitory-vue/package.json`：

- `dev`
- `build`
- `preview`
- `build-only`
- `type-check`

### 11.2 后端

在 `low-carbon-dormitory-spring` 目录下：

```bash
./mvnw spring-boot:run
```

测试：

```bash
./mvnw test
```

### 11.3 配置项

当前 `application.properties` 中包含：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `server.port`
- `app.auth.token-secret`
- `app.auth.token-ttl-seconds`

### 11.4 本地联调方式

本地开发时的常见方式应为：

1. 启动本机 PostgreSQL，并确保数据库 `lowcarbon` 中已经存在项目所需正式表结构和基础数据。
2. 启动后端服务，监听 `3000`。
3. 启动前端开发服务器。
4. 通过 Vite 代理访问后端 API。

后端默认直接连接 `127.0.0.1:5432/lowcarbon`，用户名 `postgres`、密码 `postgres`。应用不会在启动阶段自动准备表结构或基础数据，因此本地联调前需要先确认数据库内容已经齐全。

---

## 12. 测试现状

当前仓库包含以下测试方向：

- `LowCarbonDashboardServiceTest`
- `StudentPersonalLowCarbonDashboardServiceTest`
- `PublicStudentDashboardControllerTest`
- `StudentPublicAccessByStuNumTest`
- 其他应用启动与 JDBC 类测试

从测试代码看，当前测试更接近“集成测试 + 冒烟测试”：

- 依赖真实数据库或至少依赖某个可访问的数据源。
- 当数据库不可达时，部分测试会通过 `Assumptions` 直接跳过。
- Mock 层和纯单元测试相对较少。

这意味着：

- 测试可以验证真实流程是否通。
- 但本地和 CI 的稳定性会受数据库状态影响较大。

---

## 13. 已知风险、债务与维护注意事项

### 13.1 明文数据库配置

`application.properties` 直接提交了数据库 URL、用户名和密码。  
这是当前代码中最明确的高风险项之一。

### 13.2 学生接口开放边界不清晰

前端路由守卫表现为“学生页面需要登录”，但后端并没有统一拦截学生接口。  
再结合已有测试，系统当前实际上允许多个学生接口在只提供 `stuNum` 时直接访问。

这会带来两个问题：

- 业务语义不清楚：到底是“公开接口”还是“漏了鉴权”。
- 安全边界不清楚：查询个人资料、宿舍账务是否应该对任意知道学号的人开放。

### 13.3 数据库初始化外置后的运维要求

应用本身不再承担数据库自举职责，因此数据库准备流程需要由既有数据库内容保证。  
如果环境缺少现行表结构、索引或基础数据，应用会直接在运行时报错，而不是像之前那样尝试自修复。

### 13.5 中文字符串存在编码异常

多个 `.vue`、`.java` 与测试文件中的中文字符串出现乱码现象。  
这会影响：

- 页面展示文案
- 接口错误消息
- 测试断言可读性
- 维护者理解代码语义

### 13.6 前端 UI 组件库混用

当前前端同时依赖：

- `Element Plus`
- `Ant Design Vue`

这会增加：

- 视觉风格不一致风险
- 组件交互不一致风险
- 打包体积
- 维护成本

### 13.7 支付链路仍是模拟实现

从 `StudentPaymentService` 看，支付单流程已具备订单模型、二维码内容和成功回写，但“支付成功”仍通过 `simulate-success` 完成。  
如果后续要接真实支付，需要补充：

- 支付回调
- 签名校验
- 幂等控制
- 异常订单处理

---

## 14. 建议的后续整理方向

按优先级建议：

1. 明确学生接口哪些应公开，哪些必须鉴权，并让前后端行为一致。
2. 立即移除仓库中的明文数据库密码，改为环境变量或外部配置。
3. 保持“应用只连现有正式数据库”的边界，不再把建表、初始化或旧迁移逻辑带回应用启动流程。
4. 清理乱码文案，统一源码编码为 UTF-8。
5. 统一前端组件体系，减少混用。
6. 为核心服务补充可离线运行的单元测试。
7. 补充一份独立的建库 / 初始化脚本，替代原先由应用启动阶段隐式承担的数据库准备工作。

---

## 15. 关键源码入口清单

如果新维护者只想快速建立全局认知，建议优先阅读以下文件：

### 15.1 前端优先阅读

- `low-carbon-dormitory-vue/src/router/router.ts`
- `low-carbon-dormitory-vue/src/api/http.ts`
- `low-carbon-dormitory-vue/src/stores/student-token.ts`
- `low-carbon-dormitory-vue/src/stores/admin-token.ts`
- `low-carbon-dormitory-vue/src/utils/unified-login.ts`
- `low-carbon-dormitory-vue/src/api/modules/student.ts`
- `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- `low-carbon-dormitory-vue/src/api/modules/dashboard.ts`

### 15.2 后端优先阅读

- `low-carbon-dormitory-spring/src/main/resources/application.properties`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/WebCorsConfig.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/AuthTokenInterceptor.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/auth/TokenService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPaymentService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPersonalLowCarbonDashboardService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/rule/LowCarbonRuleService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

---

## 16. 总结

这套系统的核心价值不只是“缴费”，而是围绕“宿舍低碳表现”组织出来的一整套宿舍运营平台：

- 账务系统提供原始水电消费数据。
- 低碳规则把账务数据转换成碳排与得分。
- 看板系统把结果组织为宿舍、楼栋、学院、全校几个层级的比较视图。
- 奖励系统把积分结果再转化为激励机制。
- 管理端负责维护规则、学生、库存和扣费动作。

从架构上看，这个仓库已经具备完整业务闭环，但同时也累积了较明显的工程债务，尤其集中在：

- 鉴权边界不清晰
- 数据库初始化流程需要显式外置维护
- 配置安全性不足
- 编码与文案一致性问题

如果后续要继续演进，这几个点会比继续堆页面更值得优先处理。
