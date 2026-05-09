# 管理员奖励管理与水电单价实现文档

## 功能目标

该功能是管理端的综合维护页面，负责两类后台配置：

- 水电单价维护
- 奖励项目维护

## 前端入口

### 路由与页面

- 路由：`/manager/reward-manage`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/reward/reward-manage.vue`

页面包含三块能力：

- 编辑并保存水电单价
- 新增奖励
- 修改奖励库存或删除奖励

### 前端 API

位于 `low-carbon-dormitory-vue/src/api/modules/admin.ts`：

- `fetchUtilityRates()`
- `updateUtilityRate(...)`
- `fetchRewardsByAdmin()`
- `createRewardByAdmin(...)`
- `updateRewardStockByAdmin(...)`
- `deleteRewardByAdmin(...)`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Service：`.../service/admin/AdminManagementService.java`

相关接口：

- `GET /admin/utility-rates`
- `PUT /admin/utility-rates/{feeType}`
- `GET /admin/rewards`
- `POST /admin/rewards`
- `PUT /admin/rewards/{rewardId}/stock`
- `DELETE /admin/rewards/{rewardId}`

## 水电单价实现

### 查询单价

`AdminManagementService.listUtilityRates()`：

- 直接读取 `system_utility_rate_config`
- 按 `feeType` 排序返回

### 保存单价

`AdminManagementService.updateUtilityRate(...)`：

1. 标准化 `feeType`
2. 先按主键查配置
3. 如果存在则更新
4. 如果不存在则插入
5. 返回最新配置

这使得水电单价表具备“可补建”能力，不要求数据一定先初始化完成。

## 奖励管理实现

### 奖励列表

`AdminManagementService.listRewards()`：

- 查询全部奖励
- 按 `sortOrder`、`rewardId` 排序

### 新增奖励

`AdminManagementService.createReward(...)`：

1. 组装 `RewardItem`
2. 设置默认排序、状态、创建时间、更新时间
3. 插入奖励表

### 更新库存

`AdminManagementService.updateRewardStock(...)`：

1. 按 ID 查奖励
2. 校验奖励存在
3. 更新库存
4. 更新 `updateTime`
5. 返回最新记录

### 删除奖励

`AdminManagementService.deleteReward(...)`：

1. 校验 `rewardId`
2. 校验奖励存在
3. 直接物理删除

## 与学生端奖励中心的关系

学生端奖励中心读取的是：

- `status = 1` 的奖励

因此管理端维护的这些字段会直接影响学生端：

- 是否可见
- 所需积分
- 库存数量
- 展示顺序

## 核心数据对象

前端：

- `UtilityRateItem`
- `AdminRewardItem`
- `AdminCreateRewardRequest`

后端：

- `UtilityRateConfig`
- `RewardItem`
- `AdminCreateRewardRequest`
- `AdminUpdateRewardStockRequest`

## 相关文件

- `low-carbon-dormitory-vue/src/router/manager-router/reward/reward-manage.vue`
- `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

## 维护注意点

- 该页面把“费率配置”和“奖励管理”放在一起，业务上是两个子域，后续如果页面复杂度继续增长，建议拆页。
- 奖励删除是物理删除，不是软删除；若后续需要保留历史关联，删除策略要调整。
- 费率配置会影响看板预览、宿舍可用量换算和部分展示文本，修改后要联动验证相关页面。
