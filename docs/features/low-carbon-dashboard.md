# 低碳看板实现文档

## 功能目标

低碳看板功能分成两类：

- 个人低碳看板
- 宿舍整体低碳看板

它们共同依赖宿舍费用历史、水电单价和低碳规则，把账务数据转换为：

- 用量
- 碳排
- 得分
- 排名
- 趋势
- 推荐信息

## 前端入口

### 个人看板

- 路由：`/low-carbon-dashboard`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/dashboard/low-carbon-dashboard.vue`

页面展示内容包括：

- 当前宿舍信息
- 当前周期得分
- 楼栋 / 学院 / 学校排名
- 费用构成与碳排构成
- 历史趋势
- 楼栋与学院榜样宿舍

### 总览看板

- 页面文件：`low-carbon-dormitory-vue/src/router/student-router/dashboard/low-carbon-dashboard-overview.vue`
- 管理端路由也复用这一页面：
  - `/manager/low-carbon-overview`

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/dashboard.ts`
- 方法：
  - `fetchPersonalDashboard(...)`
  - `fetchGlobalDashboard(...)`

## 后端入口

### 学生端接口

- Controller：`.../controller/student/StudentLowCarbonDashboardController.java`

接口：

- `GET /student/low-carbon-dashboard`
- `GET /student/low-carbon-dashboard-personal`

### 公开接口

- Controller：`.../controller/publicapi/PublicStudentDashboardController.java`

接口：

- `GET /public/student/low-carbon-dashboard`
- `GET /public/student/low-carbon-dashboard-personal`

## 核心服务分工

### `LowCarbonDashboardService`

职责：

- 构建宿舍整体看板
- 计算宿舍列表、楼栋列表、总览信息
- 支持周期化排行视图

### `StudentPersonalLowCarbonDashboardService`

职责：

- 构建个人看板
- 组合宿舍锚点、比较结果、建议、趋势
- 生成更贴近前端页面的数据结构

### `LowCarbonDashboardSupport`

职责：

- 构建周期窗口
- 聚合费用历史
- 换算电费、水费用量
- 计算碳排与得分
- 格式化公式文本
- 辅助排名

### `LowCarbonRuleService`

职责：

- 提供当前生效的低碳评分规则
- 提供荣誉规则
- 为看板服务提供规则快照

## 宿舍总览实现流程

入口：

- `LowCarbonDashboardService.buildDashboard(...)`

主要流程：

1. 解析当前学生或目标宿舍
2. 加载水电单价
3. 加载当前评分规则
4. 读取所有宿舍
5. 读取费用历史
6. 构建：
   - 最近充值周期聚合
   - 指定排行周期聚合
7. 为每个宿舍计算：
   - 电费
   - 水费
   - 电量
   - 水量
   - 电碳排
   - 水碳排
   - 总碳排
   - 碳分
8. 生成宿舍列表
9. 生成楼栋统计
10. 填充排名与总览

## 个人看板实现流程

入口：

- `StudentPersonalLowCarbonDashboardService.buildDashboard(...)`

主要流程：

1. 定位当前学生与宿舍
2. 读取全量学生和宿舍
3. 按宿舍分组住户
4. 读取水电单价与评分规则
5. 读取费用历史
6. 计算：
   - 最近周期聚合
   - 当前周期聚合
   - 每个宿舍的指标对象
7. 提取当前宿舍、同楼栋、同学院、全校的对比集合
8. 生成：
   - `scoreSummary`
   - `ruleSummary`
   - `dormAnchor`
   - `overview`
   - `comparisons`
   - `recommendations`
   - `trends`

## 数据来源

该功能依赖的主要表与实体：

- `student_dorm_info` / `DormInfo`
- `student_fee_history` / `DormFeeHistory`
- `student_base` / `StudentBase`
- `system_utility_rate_config` / `UtilityRateConfig`
- `student_low_carbon_score_rule` / `LowCarbonScoreRule`
- `student_low_carbon_honor_rule` / `LowCarbonHonorRule`

## 前端数据结构

在 `src/api/modules/dashboard.ts` 中定义了两大返回类型：

- `PersonalDashboardData`
- `GlobalDashboardData`

这两个类型已经非常接近页面最终展示结构，说明后端承担了较多展示聚合职责。

## 公开查询实现

公开接口与学生接口共享同一套服务：

- `PublicStudentDashboardController`
- `LowCarbonDashboardService`
- `StudentPersonalLowCarbonDashboardService`

这意味着：

- 学生端和公开端返回的数据形状基本一致
- 如果改动看板返回字段，通常会同时影响公开查询和登录后页面

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/dashboard/low-carbon-dashboard.vue`
- `low-carbon-dormitory-vue/src/api/modules/dashboard.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentLowCarbonDashboardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/publicapi/PublicStudentDashboardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPersonalLowCarbonDashboardService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardSupport.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/rule/LowCarbonRuleService.java`

## 维护注意点

- `StudentPersonalLowCarbonDashboardService` 体量较大，修改时要特别注意不要把展示逻辑和底层计算逻辑继续耦合得更深。
- 公开接口与登录后接口共用服务，改动字段时要同时评估公开访问场景。
- 看板强依赖 `student_fee_history`，如果账单流水策略变化，排名结果也会变化。
