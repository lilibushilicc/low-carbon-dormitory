# 奖励兑换实现文档

> 2026-05-12 补充：`reward-exchange-antd.vue` 头部已调整为标题与搜索框同一行的紧凑布局，搜索框固定在右上角，头部高度仅包裹这一行内容。
> 2026-05-12 补充：`reward-exchange-antd.vue` 头部已补充少量顶部留白，避免标题与搜索框紧贴页面顶部。
> 2026-05-12 补充：`reward-exchange-antd.vue` 手机端积分商城已新增首次加载提示与骨架屏；奖励图片采用独立加载状态，未加载完成前在图片位展示转圈占位，加载完成后淡入显示，避免外部图片响应慢时出现近似白屏。

> 2026-05-11 补充：奖励兑换手机端页面（`reward-exchange-antd.vue`）背景图资源使用 `low-carbon-dormitory-vue/public/images/reward-exchange-mobile-bg.jpg`；页面已调整为手机端全屏响应布局，背景图直接参与页面主视觉，仅保留轻度遮罩保证可读性；奖励列表与兑换记录两种视图统一使用同一套内容盒尺寸与间距规则。

## 功能目标

奖励功能用于把学生的个人低碳积分转化为可兑换奖励，主要包含：

- 奖励中心查询
- 奖励列表展示
- 奖励兑换
- 个人兑换记录展示

## 前端入口

### 路由与页面

- 路由：`/reward-exchange`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange.vue`

相关文件：

- `reward-exchange-son.vue`
- `reward-exchange-antd.vue`
- `use-reward-center.ts`

补充说明：

- 桌面版奖励中心不再提供跳转到 `reward-exchange-antd` 的页面入口
- 手机端奖励页当前仅支持通过 URL 直接访问

### 前端组合逻辑

`useRewardCenter()` 位于：

- `low-carbon-dormitory-vue/src/router/student-router/reward/use-reward-center.ts`

职责：

- 读取当前学号
- 加载奖励中心数据
- 发起奖励兑换
- 管理加载状态和提交状态

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/reward.ts`
- 方法：
  - `fetchRewardCenter(stuNum)`
  - `exchangeReward(stuNum, rewardId)`

## 页面流程

1. `reward-exchange.vue` 页面挂载时调用 `loadRewardCenter()`
2. `useRewardCenter()` 调用 `fetchRewardCenter(stuNum)`
3. 返回的数据包括：
   - 当前积分
   - 最近可兑换奖励差距
   - 奖励列表
   - 个人兑换记录
4. 用户点击某个奖励的兑换按钮
5. 页面弹确认框
6. 调用 `exchangeRewardById(rewardId)`
7. 兑换成功后重新加载奖励中心

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java`
- Service：`.../service/student/RewardService.java`

接口：

- `GET /student/rewards`
- `POST /student/rewards/supplement-points`
- `POST /student/rewards/exchange`

## 奖励中心查询实现

`RewardService.getRewardCenter(stuNum)` 实际直接调用：

- `supplementPersonalPoints(stuNum)`

该方法会：

1. 解析学生和宿舍上下文
2. 读取学生当前积分
3. 读取启用中的奖励列表
4. 读取该学生最近兑换记录
5. 组装 `RewardCenterResponse`

返回中包括：

- `currentPoints`
- `exchangeCount`
- `dormScoreSummary`
- `rewardItems`
- `exchangeRecords`

### 奖励可兑换状态

每个奖励项在后端就会被转换成：

- `canExchange`
- `exchangeTip`

判断依据：

- 库存是否大于 0
- 当前积分是否足够

## 兑换实现

`RewardService.exchange(stuNum, rewardId)` 的核心流程：

1. 校验 `rewardId`
2. 解析学生和宿舍
3. 读取奖励项并校验状态
4. 校验库存与积分是否足够
5. 通过 `studentBaseMapper.update(...)` 扣减学生积分
6. 通过 `rewardItemMapper.update(...)` 扣减奖励库存
7. 重新查询学生与奖励项，得到剩余积分和库存
8. 写入 `student_reward_exchange`
9. 返回 `RewardExchangeResult`

### 数据一致性

兑换逻辑使用了事务，并且更新条件里带有积分与库存约束，因此可以避免一部分并发下的超兑问题。

## 关键数据对象

前端：

- `RewardCenter`
- `RewardItem`
- `RewardRecord`
- `RewardExchangeResult`

后端：

- `RewardCenterResponse`
- `RewardExchangeRequest`
- `RewardExchangeResult`
- `RewardItem`
- `RewardExchangeRecord`

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange.vue`
- `low-carbon-dormitory-vue/src/router/student-router/reward/use-reward-center.ts`
- `low-carbon-dormitory-vue/src/api/modules/reward.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/RewardService.java`

## 维护注意点

- 兑换中心显示的“最近可兑换奖励差距”是运行时根据最低积分门槛奖励计算的，不是单独存表字段。
- `GET /student/rewards` 当前是按 `stuNum` 查的，如果后续要彻底改为 token 鉴权，需要同步调整前端调用方式。
- 删除奖励时要留意历史兑换记录是否仍需要展示，当前代码没有做软删除。
