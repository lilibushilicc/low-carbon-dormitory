# 公开查询接口与权限边界文档

## 文档目的

这份文档只讨论一个问题：当前系统里，哪些接口是显式公开的，哪些接口虽然看起来像“学生接口”但实际上没有被后端统一鉴权保护，以及前端页面权限和后端真实权限之间有哪些差异。

本文档尽量只基于代码中的可验证事实，不假设产品设计意图。

---

## 1. 权限边界结论

当前系统的权限边界可以概括为三层：

1. 前端页面层：学生页和管理页都有路由守卫，页面访问看起来需要登录。
2. 后端拦截器层：当前只对 `/admin/**` 注册了 token 拦截。
3. 业务参数层：许多 `/student/**` 接口不依赖已登录上下文，而是依赖 `stuNum` 或 `dormId` 直接定位数据。

因此，当前代码中的真实边界不是“学生接口都需要登录”，而更接近：

- `/admin/**`：后端统一要求管理员 token，`/admin/login` 例外。
- `/public/student/**`：显式公开。
- `/student/**`：命名上属于学生接口，但当前没有在 WebMvc 层统一拦截，多数接口可在不带 token 时进入业务逻辑。

---

## 2. 证据来源

### 2.1 后端拦截器注册

文件：

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/WebCorsConfig.java`

关键事实：

- 只注册了 `authTokenInterceptor`
- 只拦截 `/admin/**`
- 只显式排除了 `/admin/login`

这意味着 `/student/**` 和 `/public/student/**` 当前都不在统一拦截器保护范围内。

### 2.2 拦截器内部角色规则

文件：

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/AuthTokenInterceptor.java`

关键事实：

- 拦截器内部其实写了：
  - `/student/*` 允许 `STUDENT` 和 `ADMIN`
  - `/admin/*` 只允许 `ADMIN`

但因为该拦截器没有注册到 `/student/**`，所以这部分 `/student/*` 角色限制当前并不会统一生效。

### 2.3 前端路由守卫

文件：

- `low-carbon-dormitory-vue/src/router/router.ts`

关键事实：

- 学生端页面通过 `meta.requiresStudentAuth` 做前端守卫
- 管理端页面通过 `meta.requiresAdminAuth` 做前端守卫
- 未登录时，前端会跳统一登录页

这说明“前端体验”是需要登录的，但它不能替代后端鉴权。

### 2.4 前端请求头策略

文件：

- `low-carbon-dormitory-vue/src/api/http.ts`

关键事实：

- 登录接口不带 token
- `/admin/*` 请求优先附加管理员 token
- 非 `/admin/*` 请求会优先附加学生 token，没有学生 token 时会退回管理员 token

这说明前端默认会尽量带 token，但并不说明后端真的要求 token。

---

## 3. 权限矩阵总览

### 3.1 显式公开接口

这些接口位于 `/public/student/**` 命名空间下，按控制器设计就是公开接口：

| 方法 | 路径 | 控制器 | 当前后端 token 要求 |
| --- | --- | --- | --- |
| `GET` | `/public/student/low-carbon-dashboard` | `PublicStudentDashboardController` | 不要求 |
| `GET` | `/public/student/low-carbon-dashboard-personal` | `PublicStudentDashboardController` | 不要求 |

### 3.2 显式登录接口

这些接口本来就用于获取登录态，不需要先带 token：

| 方法 | 路径 | 控制器 | 当前后端 token 要求 |
| --- | --- | --- | --- |
| `POST` | `/student/login` | `StudentAuthenticationController` | 不要求 |
| `POST` | `/admin/login` | `AdminAuthenticationController` | 不要求 |

### 3.3 管理员受保护接口

这些接口位于 `/admin/**`，并且由于拦截器注册生效，后端当前会统一要求管理员 token：

| 方法 | 路径模式 | 控制器 | 当前后端 token 要求 |
| --- | --- | --- | --- |
| `GET` | `/admin/utility-rates` | `AdminManagementController` | 要求管理员 token |
| `PUT` | `/admin/utility-rates/{feeType}` | `AdminManagementController` | 要求管理员 token |
| `POST` | `/admin/dorms/{dormId}/fees/deduct` | `AdminManagementController` | 要求管理员 token |
| `GET` | `/admin/students` | `AdminManagementController` | 要求管理员 token |
| `POST` | `/admin/students` | `AdminManagementController` | 要求管理员 token |
| `GET` | `/admin/students/{studentId}/delete-check` | `AdminManagementController` | 要求管理员 token |
| `DELETE` | `/admin/students/{studentId}` | `AdminManagementController` | 要求管理员 token |
| `GET` | `/admin/rewards` | `AdminManagementController` | 要求管理员 token |
| `POST` | `/admin/rewards` | `AdminManagementController` | 要求管理员 token |
| `PUT` | `/admin/rewards/{rewardId}/stock` | `AdminManagementController` | 要求管理员 token |
| `DELETE` | `/admin/rewards/{rewardId}` | `AdminManagementController` | 要求管理员 token |
| `GET` | `/admin/low-carbon-rules` | `LowCarbonRuleAdminController` | 要求管理员 token |
| `PUT` | `/admin/low-carbon-rules` | `LowCarbonRuleAdminController` | 要求管理员 token |
| `POST` | `/admin/low-carbon-rules/preview` | `LowCarbonRuleAdminController` | 要求管理员 token |

### 3.4 学生命名空间下当前未统一受保护的接口

这些接口都位于 `/student/**`，命名上属于学生功能，但当前后端没有统一拦截保护：

| 方法 | 路径 | 控制器 | 当前后端 token 要求 |
| --- | --- | --- | --- |
| `GET` | `/student/profile` | `StudentProfileController` | 当前不统一要求 |
| `GET` | `/student/water-electricity` | `StudentWaterElectricityController` | 当前不统一要求 |
| `POST` | `/student/water-electricity/refresh` | `StudentWaterElectricityController` | 当前不统一要求 |
| `GET` | `/student/fee-history` | `StudentFeeHistoryController` | 当前不统一要求 |
| `POST` | `/student/pay` | `StudentPaymentController` | 当前不统一要求 |
| `POST` | `/student/payments/orders` | `StudentPaymentController` | 当前不统一要求 |
| `GET` | `/student/payments/orders/{orderNo}` | `StudentPaymentController` | 当前不统一要求 |
| `POST` | `/student/payments/orders/{orderNo}/simulate-success` | `StudentPaymentController` | 当前不统一要求 |
| `GET` | `/student/low-carbon-dashboard` | `StudentLowCarbonDashboardController` | 当前不统一要求 |
| `GET` | `/student/low-carbon-dashboard-personal` | `StudentLowCarbonDashboardController` | 当前不统一要求 |
| `GET` | `/student/low-carbon-rules` | `StudentLowCarbonRuleController` | 当前不统一要求 |
| `GET` | `/student/rewards` | `StudentRewardController` | 当前不统一要求 |
| `POST` | `/student/rewards/supplement-points` | `StudentRewardController` | 当前不统一要求 |
| `POST` | `/student/rewards/exchange` | `StudentRewardController` | 当前不统一要求 |

这里的“当前不统一要求”是指：

- 没有在 WebMvc 拦截器层统一强制 Bearer token
- 是否能成功执行，取决于具体接口内部是否能根据参数完成业务定位

---

## 4. 已有测试证明的开放行为

### 4.1 显式公开控制器测试

文件：

- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/PublicStudentDashboardControllerTest.java`

该测试明确验证：

- 不带 token 调用 `/public/student/low-carbon-dashboard`
- 只传 `stuNum`
- 返回 `200`

这证明公开看板接口确实按公开方式工作。

### 4.2 学生命名空间下公开访问测试

文件：

- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/StudentPublicAccessByStuNumTest.java`

该测试明确验证了以下行为：

#### `GET /student/water-electricity`

- 不带 token
- 只传 `stuNum`
- 返回 `200`

#### `GET /student/rewards`

- 不带 token
- 只传 `stuNum`
- 返回 `200`

#### `GET /student/profile`

- 不带 token
- 只传 `stuNum`
- 返回 `200`

#### `POST /student/rewards/exchange`

- 不带 token
- 传 body 中的 `stuNum` 和 `rewardId`
- 断言结果至少不是 `401`

最后这一条尤其说明：

- 当前奖励兑换接口即便不一定业务成功，也能在不带 token 的情况下进入业务层，而不是被统一鉴权拦住。

---

## 5. 按功能说明当前边界

### 5.1 低碳看板

低碳看板当前存在两套入口：

#### 显式公开入口

- `/public/student/low-carbon-dashboard`
- `/public/student/low-carbon-dashboard-personal`

特点：

- 这是清晰命名的公开接口
- 调用时靠 `stuNum` 和可选的 `dormId` 定位

#### 学生命名空间入口

- `/student/low-carbon-dashboard`
- `/student/low-carbon-dashboard-personal`

特点：

- 前端页面要求学生登录后才会进入
- 但后端当前没有统一 token 拦截
- 因为控制器参数是 `stuNum`、`dormId`，所以理论上可通过参数直接查询

### 5.2 个人资料

接口：

- `/student/profile`

特点：

- 页面侧通过路由守卫要求学生登录
- 后端控制器只要求 `stuNum`
- 已有测试证明不带 token 也可成功查询

这意味着个人资料当前更接近“知道学号即可查询”的接口行为。

### 5.3 宿舍水电

接口：

- `/student/water-electricity`
- `/student/water-electricity/refresh`

特点：

- 查询接口支持 `stuNum` 或 `dormId`
- 已有测试证明查询接口可不带 token
- 刷新接口虽然没有现成测试，但由于当前也不在统一拦截范围内，是否可执行取决于业务参数是否满足

风险点在于：

- “刷新”不是纯查询，而是会写流水并影响积分
- 如果它也能无 token 执行，风险比只读查询更高

### 5.4 缴费与支付订单

接口：

- `/student/pay`
- `/student/payments/orders`
- `/student/payments/orders/{orderNo}`
- `/student/payments/orders/{orderNo}/simulate-success`

特点：

- 都在 `/student/**` 下
- 当前不受统一后端拦截保护
- 参数里包含 `stuNum`、`dormId`、`orderNo` 等业务定位信息

风险点在于：

- 这些接口都不是只读接口
- 它们会创建订单、修改余额、更新订单状态、写流水、影响积分

### 5.5 奖励相关

接口：

- `/student/rewards`
- `/student/rewards/supplement-points`
- `/student/rewards/exchange`

特点：

- `GET /student/rewards` 已被测试证明可公开访问
- `POST /student/rewards/exchange` 已被测试证明至少能在无 token 情况下进入业务逻辑

风险点在于：

- 兑换是会修改积分和库存的写操作
- 当前后端边界并不体现“必须已登录本人才能兑换”

### 5.6 规则查看

接口：

- `/student/low-carbon-rules`

特点：

- 当前不需要参数
- 也不在统一拦截范围内

这意味着它在代码现状上是一个可匿名访问的只读规则接口。

---

## 6. 前端权限与后端权限的差异

### 6.1 前端做了什么

前端通过 `router.ts` 把这些页面都标记为需要学生或管理员登录：

- 学生页：`requiresStudentAuth`
- 管理页：`requiresAdminAuth`

这能阻止用户在正常页面导航中直接进入受限页面。

### 6.2 前端做不了什么

前端路由守卫不能防止以下行为：

- 直接调用后端 URL
- 构造请求并传入 `stuNum`
- 绕过前端页面发请求

所以只要后端没有统一拦截，这些接口在真实安全边界上仍然是开放的。

### 6.3 为什么会出现这种差异

从代码上看，原因主要有两个：

1. 设计过公开查询接口：
   - `/public/student/**`
2. 学生接口大量依赖业务参数定位，而不是依赖“当前登录用户上下文”

再加上：

3. 拦截器没有注册到 `/student/**`

就形成了当前“页面上需要登录，接口上未必需要”的状态。

---

## 7. 可以如何理解当前状态

从纯代码事实出发，当前系统更像是把接口分成了三类：

### A. 明确管理员私有接口

特点：

- 路径在 `/admin/**`
- 后端统一鉴权
- 权限边界相对清晰

### B. 明确公开接口

特点：

- 路径在 `/public/student/**`
- 公开命名清晰
- 面向公开查询场景

### C. 学生命名空间下的“半公开接口”

特点：

- 路径在 `/student/**`
- 页面语义上像“学生登录后功能”
- 但后端当前没有统一强制 token
- 是否执行成功更多依赖 `stuNum` / `dormId` / `orderNo`

这第三类是当前最容易引起误解的边界。

---

## 8. 风险与维护建议

### 8.1 风险分级

#### 低风险：公开只读说明类接口

例如：

- `/student/low-carbon-rules`

如果业务上允许匿名读取规则，这类风险相对低。

#### 中风险：公开只读查询类接口

例如：

- `/student/profile`
- `/student/water-electricity`
- `/student/rewards`
- `/student/low-carbon-dashboard*`

风险在于：

- 只要知道学号或宿舍 ID，就可能读到个人或宿舍信息

#### 高风险：公开写操作类接口

例如：

- `/student/water-electricity/refresh`
- `/student/pay`
- `/student/payments/orders`
- `/student/payments/orders/{orderNo}/simulate-success`
- `/student/rewards/exchange`

风险在于：

- 它们会写库、改余额、改积分、改库存、写订单或写流水

### 8.2 文档层建议

如果决定保留当前行为，建议至少在文档中明确把接口区分为：

- 显式公开接口
- 学生查询接口
- 学生写操作接口
- 管理员受保护接口

否则维护者容易误以为：

- `/student/**` 默认都已经被后端鉴权保护

### 8.3 代码层建议

如果希望权限模型更一致，可以考虑两种方向：

#### 方向一：把 `/student/**` 全部纳入统一拦截

适用于：

- 希望学生能力全部建立在 token 身份之上

代价：

- 公开查询需求需要迁移到 `/public/student/**` 或单独白名单

#### 方向二：明确区分“公开学生查询接口”和“登录后学生接口”

适用于：

- 确实需要通过学号/宿舍号做公开查询

做法：

- 把匿名可访问的学生查询能力搬到 `/public/student/**`
- 把会写库的学生接口统一纳入 token 拦截
- 避免现在这种“都叫 `/student/*`，但一部分其实公开”的混合状态

---

## 9. 关键源码清单

权限边界相关的关键文件如下：

### 后端

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/WebCorsConfig.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/AuthTokenInterceptor.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/publicapi/PublicStudentDashboardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentProfileController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentWaterElectricityController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentPaymentController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentLowCarbonDashboardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentLowCarbonRuleController.java`

### 前端

- `low-carbon-dormitory-vue/src/router/router.ts`
- `low-carbon-dormitory-vue/src/api/http.ts`

### 测试

- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/PublicStudentDashboardControllerTest.java`
- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/StudentPublicAccessByStuNumTest.java`

---

## 10. 一句话总结

当前系统的真实权限边界是：

- 管理员接口后端统一受保护
- 公开看板接口显式公开
- 大量学生接口虽然前端页面要求登录，但后端当前并未统一要求 token，因此它们的真实访问边界比页面语义更宽

这份差异是当前权限模型里最需要被文档化和后续处理的地方。
