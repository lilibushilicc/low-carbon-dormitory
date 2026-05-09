# 管理员宿舍扣费实现文档

## 功能目标

该功能允许管理员按宿舍 ID 直接扣减宿舍水费或电费，并自动写入费用流水。

这是一个后台运营动作，不经过学生支付单流程。

## 前端入口

### 路由与页面

- 路由：`/manager/dorm-fee-deduct`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/fee/dorm-fee-deduct.vue`

页面可填写的主要字段：

- 宿舍 ID
- 费用类型
- 扣减金额
- 支付类型
- 付款人姓名
- 付款账号
- 备注

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/admin.ts`
- 方法：`deductDormFeeByAdmin(dormId, payload)`
- 请求：`POST /admin/dorms/{dormId}/fees/deduct`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Service：`.../service/admin/AdminManagementService.java`

## 核心调用链

1. `AdminManagementController.deductDormFee(...)`
2. `AdminManagementService.deductDormFee(dormId, request)`

## 扣费实现

`AdminManagementService.deductDormFee(...)` 的主要步骤：

1. 校验宿舍 ID 不为空
2. 调用 `DormFeeAccountService.getForUpdate(dormId)` 锁定宿舍费用账户
3. 校验宿舍账户存在
4. 规范化费用类型
5. 规范化扣减金额
6. 调用 `dormFeeAccountService.deductBalance(...)` 扣减余额
7. 更新宿舍账户的 `lastDeductTime`
8. 向 `student_fee_history` 插入一条扣费流水
9. 返回最新的宿舍费用账户信息

## 流水写入内容

插入 `student_fee_history` 时写入的关键信息包括：

- `dorm_id`
- `fee_type`
- `operation_type`
- `pay_type`
- `amount`
- `balance_after`
- `create_time`
- `payer_name`
- `payer_account`

其中金额以负数写入：

- `amount.negate()`

这使得学生端历史页能把它识别为扣费事件。

## 与学生端历史页的关系

学生端 `history-fee.vue` 会把非 `RECHARGE`、非 `REFRESH` 的记录识别为“周期扣费”。  
因此管理员手工扣费在学生视角会显示成扣费类型事件。

## 核心数据对象

前端：

- `AdminDeductDormFeeRequest`
- `DormFeeInfo`

后端：

- `AdminDeductDormFeeRequest`
- `DormFee`

## 相关文件

- `low-carbon-dormitory-vue/src/router/manager-router/fee/dorm-fee-deduct.vue`
- `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/DormFeeAccountService.java`

## 维护注意点

- 该接口会直接修改宿舍余额，不经过学生端任何确认流程，权限控制一定要依赖管理员鉴权。
- 扣费会写入学生共用历史表，历史页展示文案如果要更细化，建议引入更明确的 `operationType`。
- 当前返回值只包含余额结果，不返回这次流水 ID；如果后续需要做运营审计回跳，可以考虑补充。
