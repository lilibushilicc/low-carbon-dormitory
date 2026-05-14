# 奖励兑换功能说明

> 2026-05-14 更新：手机端 `reward-exchange-antd.vue` 已改为低层级兑换流，顶部只保留积分摘要与视图切换，奖励卡片直接展示库存与兑换提示，确认弹层压缩为扣分、剩余积分、库存三个关键信息。
> 2026-05-13 更新：学生端奖励页数据逻辑已回收到页面内部，当前不再依赖 `use-reward-center.ts`。
> 2026-05-13 更新：学生端 `reward-exchange.vue` 已移除对 `reward-exchange-son.vue` 的依赖，奖励列表展示逻辑已并回主页面。

## 功能目标

奖励兑换用于把学生个人低碳积分转换为可兑换奖励，核心能力包括：

- 查询奖励中心
- 浏览奖励列表
- 发起奖励兑换
- 查看个人兑换记录

## 前端入口

### 路由与页面

- 桌面端路由：`/reward-exchange`
- 桌面端页面：`low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange.vue`
- 手机端路由：`/reward-exchange-antd`
- 手机端页面：`low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange-antd.vue`

补充说明：

- 桌面端奖励页不再提供跳转到手机端页面的可视化入口。
- 手机端奖励页当前支持通过 URL 直接访问。
- 手机端支持通过 `?stuNum=学号` 方式读取对应学生的奖励中心数据。

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/reward.ts`
- 方法：
  - `fetchRewardCenter(stuNum)`
  - `exchangeReward(stuNum, rewardId)`

## 手机端交互说明

当前手机端页面围绕“少一层进入、少一次来回”做了调整：

- 头部只保留标题、说明和搜索框，不再堆叠多层说明区。
- 顶部摘要区合并为单张积分卡，直接展示当前积分和最近可兑换奖励。
- 奖励卡片内直接展示奖励描述、库存和兑换提示，减少进入确认层之前的信息缺失。
- 兑换确认弹层只保留三项核心信息：本次扣除、兑换后剩余、剩余库存。
- 分页信息压缩为简短页码提示，避免在移动端占据过多垂直空间。

## 页面流程

1. 页面挂载后调用 `loadRewardCenter()`。
2. 页面根据当前登录态或路由中的 `stuNum` 调用 `fetchRewardCenter(stuNum)`。
3. 返回的数据包含：
   - 当前积分
   - 最近可兑换奖励
   - 奖励列表
   - 个人兑换记录
4. 用户在奖励卡片中直接查看库存和兑换提示。
5. 用户点击兑换按钮后打开简化确认弹层。
6. 页面调用 `exchangeRewardById(rewardId)` 发起兑换。
7. 兑换成功后刷新奖励中心数据并更新剩余积分。

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/RewardService.java`

接口包括：

- `GET /student/rewards`
- `POST /student/rewards/supplement-points`
- `POST /student/rewards/exchange`

## 奖励中心查询实现

`RewardService.getRewardCenter(stuNum)` 负责：

1. 解析学生与宿舍上下文。
2. 读取学生当前积分。
3. 读取启用中的奖励列表。
4. 读取该学生最近的兑换记录。
5. 组装 `RewardCenterResponse` 返回前端。

返回结构主要包含：

- `currentPoints`
- `exchangeCount`
- `dormScoreSummary`
- `rewardItems`
- `exchangeRecords`

### 奖励可兑换状态

每个奖励项在后端会补充：

- `canExchange`
- `exchangeTip`

判断依据：

- 库存是否大于 0
- 当前积分是否足够

## 兑换实现

`RewardService.exchange(stuNum, rewardId)` 的主要流程：

1. 校验 `rewardId`
2. 解析学生和宿舍信息
3. 校验奖励项状态
4. 校验库存与积分是否足够
5. 扣减学生积分
6. 扣减奖励库存
7. 写入 `student_reward_exchange`
8. 返回兑换结果

### 数据一致性

兑换逻辑运行在事务中，并且更新条件包含积分和库存约束，可降低并发场景下超兑风险。

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange.vue`
- `low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange-antd.vue`
- `low-carbon-dormitory-vue/src/api/modules/reward.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/RewardService.java`

## 维护注意点

- 奖励中心里的“最近可兑换奖励”属于运行时计算结果，不是独立存储字段。
- 前端奖励中心查询目前仍以 `stuNum` 为主要参数来源；如果后续完全改为 token 鉴权，需要同步调整前端调用方式。
- 删除奖励项时，需要确认历史兑换记录是否仍要继续展示。
