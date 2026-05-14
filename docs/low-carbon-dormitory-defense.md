# 低碳宿舍管理系统项目结构与答辩文档

> 文档日期：2026-05-14  
> 适用范围：`D:\study\system\low-carbon-dormitory` 整体仓库  
> 文档目的：用于项目答辩、项目汇报、项目交接与技术说明

## 1. 项目概述

低碳宿舍管理系统是一个面向校园宿舍场景的综合管理平台，围绕“宿舍水电管理 + 低碳积分计算 + 奖励兑换激励 + 管理员运营配置”构建完整闭环。系统既服务学生，也服务管理员。

项目当前主要解决三个问题：

- 解决传统宿舍管理中水电余额、缴费、账单分散的问题。
- 解决低碳行为难量化、难展示、难激励的问题。
- 解决管理端对学生、奖励、规则、扣费等业务缺乏统一后台的问题。

从业务定位看，这不是一个单纯的“缴费系统”，而是一个将宿舍运营数据转化为低碳评价与激励机制的校园数字化应用。

## 2. 项目一句话答辩摘要

本项目采用 Vue 3 + Spring Boot + PostgreSQL 的前后端分离架构，以宿舍水电账务数据为基础，通过规则引擎式的低碳积分计算、排行榜看板和奖励兑换机制，构建了“数据采集 - 指标计算 - 可视化展示 - 激励反馈”的业务闭环。

## 3. 仓库整体结构

项目根目录按照“源码、文档、日志、发布物、规划资料”进行分层管理：

```text
low-carbon-dormitory/
├─ low-carbon-dormitory-vue/        前端源码（Vue 3 + TypeScript + Vite）
├─ low-carbon-dormitory-spring/     后端源码（Spring Boot + MyBatis-Plus）
├─ docs/                            项目说明、功能文档、部署文档、答辩文档
├─ log/                             运行日志、构建日志、排查记录、调试输出
├─ release/
│  ├─ package/                      打包产物、SQL 包、前后端发布包
│  └─ docker/                       Docker 相关产物
├─ willplan/                        后续规划、预研资料
├─ README.md                        项目总览与使用说明
└─ AGENTS.md                        项目协作规则
```

这种目录结构的优势是：

- 源码目录保持干净，便于维护和打包。
- 文档、日志、产物与规划资料分别归档，降低信息混杂。
- 项目具备较好的工程化交付特征，适合课程答辩和后续扩展。

## 4. 前后端源码结构拆解

### 4.1 前端结构

前端位于 `low-carbon-dormitory-vue/`，核心职责是页面展示、交互编排、路由守卫、接口调用与状态管理。

关键目录如下：

- `src/main.ts`：应用入口，挂载 Router 与 Pinia。
- `src/App.vue`：根组件，负责全局布局与路由容器。
- `src/router/router.ts`：统一路由表、登录态判断、SSO 参数处理、路由守卫。
- `src/api/http.ts`：Axios 实例、统一请求前缀、Token 注入。
- `src/api/modules/`：按业务模块拆分接口，如学生、管理员、奖励、看板、规则。
- `src/stores/`：学生与管理员登录状态分别存储。
- `src/components/`：学生导航、管理员导航、系统横幅等公共组件。
- `src/router/student-router/`：学生端页面。
- `src/router/manager-router/`：管理员端页面。
- `src/modules/restaurant/`：集成的餐厅管理子模块页面与接口。
- `src/utils/`：统一登录跳转、格式化、导航、公共工具函数。
- `src/types/`：前端类型定义。

### 4.2 后端结构

后端位于 `low-carbon-dormitory-spring/`，采用典型分层设计。

关键目录如下：

- `controller/`：接口入口层，接收 HTTP 请求并返回统一结果。
- `service/`：业务层，承载核心业务逻辑。
- `mapper/`：数据访问层，使用 MyBatis-Plus 操作数据库。
- `entity/`：数据库实体对象。
- `dto/request/`：请求参数模型。
- `dto/response/`：响应模型。
- `config/`：跨域、拦截器等 Web 配置。
- `common/`：统一返回体、异常定义、全局异常处理。
- `src/test/`：集成测试与接口测试。
- `src/main/resources/application.properties`：后端运行配置。

### 4.3 文档与发布结构

根目录 `docs/` 不仅保存功能文档，也承载架构文档、部署文档、答辩文档。`release/package/` 用于保存前端打包目录、后端 Jar、SQL 导入包与示例配置，体现出项目具备可交付性，而不是仅停留在开发阶段。

## 5. 技术架构说明

## 5.1 总体架构

项目采用经典的前后端分离三层架构：

```text
前端表示层（Vue 3）
        ↓ HTTP / JSON
后端业务层（Spring Boot）
        ↓ MyBatis-Plus
数据持久层（PostgreSQL）
```

架构特征包括：

- 前端负责展示与交互，不直接处理数据库逻辑。
- 后端承担认证、业务聚合、规则计算与数据写入。
- 数据库作为统一事实源，为水电、积分、奖励等业务提供一致的数据基础。

## 5.2 采用该架构的原因

- Vue 3 适合快速构建交互界面，组件化清晰。
- Spring Boot 便于构建 RESTful API，生态成熟。
- PostgreSQL 在事务一致性、SQL 能力、结构化数据管理上表现稳定。
- 前后端解耦后，界面迭代与业务逻辑迭代可以相对独立。

## 5.3 分层设计的工程价值

- `Controller` 层只负责“接收和返回”，降低控制器复杂度。
- `Service` 层沉淀业务规则，便于复用和测试。
- `Mapper` 层隔离数据库访问，减少 SQL 与业务逻辑耦合。
- `DTO` 将接口契约与数据库实体分开，避免前后端直接绑定表结构。

这类分层方式符合软件工程中的高内聚、低耦合原则。

## 6. 技术栈与专业说明

### 6.1 前端技术栈

- `Vue 3`：组件化开发，提高页面复用性。
- `TypeScript`：增强类型约束，降低接口联调错误率。
- `Vite`：本地开发启动快，适合现代前端工程化。
- `Vue Router`：负责页面路由与权限导航。
- `Pinia`：负责学生与管理员登录态管理。
- `Axios`：封装 HTTP 请求。
- `Element Plus`：桌面端表单与业务页面组件。
- `Ant Design Vue`：移动端嵌入式页面样式支撑。
- `ECharts`：低碳数据、消费趋势、统计图形展示。

### 6.2 后端技术栈

- `Spring Boot 3.4.4`：后端主框架。
- `Java 17`：LTS 版本，稳定且适合企业级开发。
- `MyBatis-Plus 3.5.11`：简化 CRUD，提高开发效率。
- `Spring Validation`：参数校验。
- `JUnit 5 + MockMvc`：接口级测试与集成测试。

### 6.3 数据库技术栈

- `PostgreSQL`：关系型数据库，适合事务型业务。

其专业优势体现在：

- 支持复杂查询与统计聚合，便于后续做排行榜、趋势分析。
- 事务语义清晰，适合充值、扣费、兑换等一致性要求较高的场景。

## 7. 核心业务模块

系统当前可拆分为六个核心模块。

### 7.1 认证与登录模块

前端路由入口位于：

- `low-carbon-dormitory-vue/src/router/router.ts`
- `low-carbon-dormitory-vue/src/stores/student-token.ts`
- `low-carbon-dormitory-vue/src/stores/admin-token.ts`

后端接口入口位于：

- `POST /student/login`
- `POST /student/register`
- `POST /admin/login`

技术说明：

- 前端通过路由元信息区分 `requiresStudentAuth` 与 `requiresAdminAuth`。
- 后端通过 `TokenService` 生成类 JWT 结构的 Token。
- 该方案属于轻量级认证实现，适合课程项目与内部系统原型。

### 7.2 宿舍水电与缴费模块

主要功能包括：

- 查询宿舍水电余额。
- 查询历史费用记录。
- 发起充值或创建支付订单。
- 管理员执行宿舍扣费。

相关页面：

- `water-electricity.vue`
- `history-fee.vue`
- `pay-up.vue`
- `water-electricity-antd.vue`
- `history-fee-antd.vue`
- `pay-up-antd.vue`

相关接口：

- `GET /student/water-electricity`
- `POST /student/water-electricity/refresh`
- `GET /student/fee-history`
- `POST /student/pay`
- `POST /student/payments/orders`
- `POST /admin/dorms/{dormId}/fees/deduct`

专业说明：

该模块本质是一个“小型账务子系统”。其中：

- `DormFee` 维护当前宿舍账户余额。
- `DormFeeHistory` 记录变动流水。
- “余额 + 流水”的组合设计符合账务系统常见建模方式。

### 7.3 低碳看板模块

相关接口：

- `GET /student/low-carbon-dashboard`
- `GET /student/low-carbon-dashboard-personal`
- `GET /public/student/low-carbon-dashboard`
- `GET /public/student/low-carbon-dashboard-personal`

相关服务：

- `LowCarbonDashboardService`
- `StudentPersonalLowCarbonDashboardService`

专业说明：

该模块的核心不是简单展示数据，而是把宿舍账务数据二次加工为低碳评价指标，形成“统计计算层”。也就是说，系统把原始水电数据转化为：

- 用量指标
- 金额指标
- 碳排估算指标
- 积分或排名指标
- 可视化看板指标

这是一种典型的数据服务化思路，即“原始业务数据 -> 统计聚合 -> 展示模型”。

### 7.4 低碳规则配置模块

相关接口：

- `GET /student/low-carbon-rules`
- `GET /admin/low-carbon-rules`
- `PUT /admin/low-carbon-rules`
- `POST /admin/low-carbon-rules/preview`

相关服务：

- `LowCarbonRuleService`

专业说明：

低碳规则模块把“积分计算逻辑”从页面和数据库表中抽离出来，形成可配置规则。这样做的价值在于：

- 规则变化时，不需要大幅修改前端页面。
- 管理员可预览规则效果，提高运营灵活性。
- 系统从“写死逻辑”升级为“参数化配置逻辑”。

### 7.5 奖励兑换模块

相关接口：

- `GET /student/rewards`
- `POST /student/rewards/supplement-points`
- `POST /student/rewards/exchange`
- `GET /admin/rewards`
- `POST /admin/rewards`
- `PUT /admin/rewards/{rewardId}/stock`
- `DELETE /admin/rewards/{rewardId}`

相关实体：

- `RewardItem`
- `RewardExchangeRecord`

专业说明：

奖励兑换模块体现了系统的业务闭环设计：

1. 宿舍行为产生水电消费数据。
2. 消费数据经过规则计算形成低碳积分。
3. 积分进入奖励系统进行兑换。
4. 兑换记录反向沉淀为运营数据。

这说明项目不仅完成了“记录”，还设计了“激励机制”，具备较好的产品逻辑完整性。

### 7.6 管理员后台模块

当前管理员端支持：

- 首页总览
- 宿舍低碳总览与宿舍详情
- 低碳规则配置
- 学生新增、列表、删除校验
- 奖励管理
- 宿舍费用扣减
- 餐厅子模块入口

从工程角度看，管理员端承担的是“配置中心 + 运营中心”的角色。

## 8. 关键业务流程

### 8.1 学生登录流程

1. 学生在前端登录页输入账号密码。
2. 前端调用 `POST /student/login`。
3. 后端校验通过后生成 Token 并返回学生资料。
4. 前端将 Token、学号、宿舍信息写入 Pinia 和本地存储。
5. 路由守卫放行学生端页面。

这个流程体现了“认证成功后写入本地会话状态，再由路由系统做访问控制”的常见单页应用模式。

### 8.2 水电查询与账单流程

1. 学生进入水电页面。
2. 前端调用 `GET /student/water-electricity`。
3. 后端从宿舍信息表、余额表、历史表中聚合数据。
4. 前端将余额、趋势、最近记录可视化展示。

### 8.3 低碳积分计算流程

1. 系统读取宿舍费用数据和用量数据。
2. 读取当前生效的低碳评分规则。
3. 由服务层计算碳排、积分、排名或对比结果。
4. 返回适用于页面展示的 DTO。

这体现出后端不是简单返回数据库原值，而是进行了“领域计算”。

### 8.4 奖励兑换流程

1. 学生查询可兑换奖励。
2. 学生提交兑换请求。
3. 后端校验库存与积分。
4. 扣减积分与库存。
5. 写入兑换记录。
6. 返回最新积分与库存结果。

该流程具备明显的事务型业务特征，核心点是“状态变化必须一致”。

## 9. 数据模型说明

从后端实体可梳理出当前核心数据对象：

- `StudentBase`：学生基础信息。
- `StudentExt`：学生扩展档案。
- `DormInfo`：宿舍信息。
- `DormFee`：宿舍账户余额。
- `DormFeeHistory`：宿舍账务流水。
- `UtilityRateConfig`：水电费单价配置。
- `LowCarbonScoreRule`：低碳积分规则。
- `LowCarbonHonorRule`：低碳荣誉规则。
- `RewardItem`：奖励物品。
- `RewardExchangeRecord`：奖励兑换记录。
- `AdminAccount`：管理员账户。

专业说明：

该建模具备“主数据 + 业务流水 + 规则配置 + 运营记录”的典型特征。尤其是 `DormFee` 与 `DormFeeHistory`、`RewardItem` 与 `RewardExchangeRecord` 的组合，符合信息系统中“状态表 + 事件表”的常见建模方式。

## 10. 接口设计说明

后端接口风格以 REST 为主，统一返回结构为：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

这种统一返回体设计有几个好处：

- 前端错误处理逻辑更统一。
- 便于后期接入网关、日志审计、接口文档工具。
- 对课程项目来说，能体现一定的接口规范意识。

此外，项目使用 `request DTO` 与 `response DTO` 分离实体对象，避免前端直接依赖数据库表结构，这是一种较规范的接口契约设计。

## 11. 前端工程化说明

前端工程化特征主要体现在以下几点：

- 使用 `Vite` 提升开发体验和构建速度。
- 使用 `TypeScript` 加强接口类型约束。
- 使用 `Pinia` 统一登录态管理。
- 使用 `Vite Proxy` 解决本地联调跨域问题。
- 使用手动拆包 `manualChunks` 优化构建输出。

`low-carbon-dormitory-vue/vite.config.ts` 中对 `vue`、`vue-router`、`pinia`、`element-plus` 做了分包策略，这说明项目已经具备一定性能优化意识，而不是纯功能堆砌。

## 12. 后端工程化说明

后端工程化特征主要体现在：

- 使用统一异常处理 `GlobalExceptionHandler`。
- 使用统一返回体 `Result`。
- 使用 `AuthTokenInterceptor` 做权限拦截。
- 使用 `TokenService` 管理认证票据。
- 使用 `Service` 聚合业务而非直接在 Controller 写逻辑。

这说明项目具备基本企业级开发规范，即“表现层、业务层、数据层职责分离”。

## 13. 餐厅子模块的扩展性说明

当前项目不仅包含宿舍系统，还在管理员端整合了餐厅管理子模块路由，例如：

- `/manager/restaurant/summary`
- `/manager/restaurant/student/list`
- `/manager/restaurant/student/meal/*`

这说明项目已经具备从“单一业务系统”向“校园多场景绿色治理平台”演进的雏形。答辩时可以将其解释为：

- 当前系统已具备模块化扩展能力。
- 低碳治理不局限于宿舍，还可延伸到餐厅、能源、行为管理等场景。

## 14. 部署与运行说明

### 14.1 本地运行

后端：

```bash
cd low-carbon-dormitory-spring
./mvnw spring-boot:run
```

前端：

```bash
cd low-carbon-dormitory-vue
npm install
npm run dev
```

### 14.2 默认运行特征

- 后端默认端口：`3000`
- 数据库：`PostgreSQL`
- 前端开发代理：
  - `/api/dorm/** -> http://localhost:3000/**`
  - `/api/** -> http://localhost:3000/**`
  - `/api/restaurant/** -> http://39.98.69.153:8081/api/**`

### 14.3 发布产物

根目录 `release/package/` 中已整理：

- 前端发布目录
- 后端 Jar
- SQL 导入包
- Nginx 示例配置
- 环境变量示例文件

这说明项目已经考虑了从开发态到发布态的交付链路。

## 15. 测试与质量保障

当前后端测试覆盖包括：

- 学生公开访问接口测试
- 低碳看板服务测试
- 学生个人看板服务测试
- 管理员学生管理测试
- 学生注册测试
- JDBC 键冲突检查测试

这类测试以 `SpringBootTest + MockMvc` 为主，特点是更贴近真实接口流程，能够验证接口、业务层、数据库访问之间的联动。

答辩时可以说明：

- 本项目测试重点放在核心业务链路正确性。
- 当前测试偏集成测试，单元测试和边界条件测试仍有继续完善空间。

## 16. 项目亮点总结

答辩中建议重点强调以下亮点：

### 16.1 业务闭环完整

系统不是单点功能，而是形成了：

- 水电数据采集
- 低碳规则计算
- 可视化看板展示
- 积分奖励兑换
- 管理端规则配置

这是一个完整的“管理 + 分析 + 激励”闭环。

### 16.2 规则可配置

低碳积分与荣誉规则不是写死在页面中，而是由后端规则配置接口统一管理。这体现出一定的可维护性和可扩展性。

### 16.3 前后端职责清晰

前端负责展示与交互，后端负责认证、聚合、计算与写库，符合软件工程分层设计思想。

### 16.4 具备工程交付意识

项目有：

- 独立文档目录
- 独立日志目录
- 独立发布目录
- 部署说明
- SQL 发布包

这使项目更接近真实工程项目，而不是纯课堂 Demo。

### 16.5 具备平台化扩展趋势

管理员端已经集成餐厅模块路由，说明系统可逐步扩展为校园绿色治理平台。

## 17. 已知风险与改进方向

为了体现答辩的客观性，建议主动说明当前仍存在的工程问题。

### 17.1 学生接口权限边界需要进一步明确

`WebCorsConfig.java` 当前只将拦截器注册到 `/admin/**`，而 `StudentPublicAccessByStuNumTest.java` 又验证了部分学生接口可在仅提供 `stuNum` 时访问。这说明：

- 当前系统对学生接口的开放边界是“有意设计”还是“暂时放开”，需要进一步规范。
- 未来可按接口敏感度细分公开接口与受保护接口。

### 17.2 支付流程目前仍以模拟成功为主

`/student/payments/orders/{orderNo}/simulate-success` 表明当前支付链路更偏演示与联调阶段，后续若接入真实支付，还需增加：

- 支付回调
- 签名校验
- 幂等控制
- 异常订单恢复

### 17.3 配置安全性可继续提升

当前 `application.properties` 中保留了默认数据库连接信息和默认 Token 密钥兜底配置。课程项目中可以接受，但真实生产环境应改为外部密钥管理或环境变量集中治理。

### 17.4 前端组件库存在混用

项目同时使用 `Element Plus` 与 `Ant Design Vue`。优点是开发灵活，但后续可能带来：

- 风格不统一
- 包体积增大
- 维护成本上升

### 17.5 测试体系可继续细化

当前集成测试较多，后续可继续补充：

- Service 层单元测试
- DTO 校验边界测试
- 权限回归测试
- 前端组件级测试

## 18. 答辩时可直接使用的技术表述

下面这些表述可以直接在答辩中使用：

- 本项目采用前后端分离架构，前端负责交互展示，后端负责业务聚合与规则计算，数据库负责结构化持久化存储。
- 系统核心创新点不在于单一的缴费功能，而在于将宿舍运行数据转化为低碳评价指标，并通过奖励机制形成行为激励闭环。
- 在工程设计上，我将业务分为认证、账务、规则、看板、奖励、管理六个核心模块，降低了功能之间的耦合度。
- 在后端实现上，我使用 Controller、Service、Mapper 三层结构，保证接口层、业务层和数据访问层职责分离。
- 在前端实现上，我通过路由守卫、状态管理和接口封装，保证学生端与管理员端在同一套工程内完成角色隔离。
- 在系统扩展性上，当前管理员端已经集成餐厅模块，这意味着系统具备从宿舍场景扩展到校园绿色治理平台的能力。

## 19. 老师可能追问的问题与参考回答

### 19.1 为什么要做低碳积分，而不是只做水电缴费？

参考回答：  
如果只做缴费，系统价值更多停留在事务处理层；加入低碳积分和奖励兑换后，系统就从“被动管理”升级为“数据驱动 + 激励引导”的治理工具，更符合绿色校园的主题。

### 19.2 为什么选择 Vue 3 和 Spring Boot？

参考回答：  
Vue 3 在组件化开发和页面响应式方面效率高，Spring Boot 在 REST API、参数校验、配置管理和工程化方面成熟，两者组合适合快速构建稳定的中小型业务系统。

### 19.3 低碳积分是如何落地的？

参考回答：  
系统并不是直接记录一个积分值，而是先采集宿舍水电相关数据，再结合低碳规则做计算，最后将计算结果展示为看板、排行和奖励可兑换能力，属于“规则驱动的数据计算”。

### 19.4 这个项目还有哪些可以继续优化？

参考回答：  
后续可以重点优化三个方向：第一是权限边界的进一步精细化；第二是真实支付链路接入；第三是补齐更完整的自动化测试和运维配置。

## 20. 总结

低碳宿舍管理系统已经具备完整的项目形态，包括前后端源码、接口体系、数据库模型、部署文档、测试代码与发布产物。它的核心价值不只是“能用”，而是完成了从宿舍账务数据到低碳积分治理的业务抽象，并通过可视化和奖励机制形成了较完整的产品闭环。

如果用于课程答辩，这个项目的讲述重点建议放在三件事上：

- 结构上，体现了规范的前后端分离与分层设计。
- 业务上，体现了从基础管理到低碳激励的闭环设计。
- 工程上，体现了文档、打包、部署、测试等较完整的软件工程意识。
