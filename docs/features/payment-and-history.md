# 缴费、支付订单与账单历史实现文档

## 功能目标

这组功能负责完成宿舍费用相关的三条用户链路：

- 学生创建支付订单并支付
- 系统在支付成功后更新宿舍余额和低碳积分
- 学生查看最近费用历史

## 前端入口

### 桌面充值页

- 路由：`/pay-up`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/billing/pay-up.vue`

当前实现补充：

- 页面为桌面版 `Vue + Element Plus` 充值页
- 需要学生登录态后访问，默认从桌面水电费页跳转进入
- 支持创建支付单、扫码支付和模拟支付成功
- 页面视觉已调整为居中大白卡布局，参考现网桌面缴费页的标题、宿舍条、单价卡、费用切换、金额输入与支付方式排布

主要能力：

- 选择水费或电费
- 输入支付金额
- 选择支付方式
- 创建支付单
- 弹出二维码对话框
- 查询支付状态
- 模拟支付成功

### 手机端充值页

- 路由：`/pay-up-antd`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/billing/pay-up-antd.vue`

当前实现补充：

- 页面采用手机端 `Ant Design Vue` 风格，使用移动卡片布局、底部支付弹层和扫码支付信息展示
- 页头、单价卡、金额区和支付方式区已做紧凑化处理，适配手机端首屏信息密度
- 当从 `/water-electricity-antd?stuNum=...` 进入时，会透传 `stuNum`、`dormId` 和 `source=utility-mobile`
- 手机端支付成功后固定返回 `/water-electricity-antd`，不会因缺少 `stuNum` 或 `source` 参数退回桌面水电页
- 未登录的公开移动链路可以直接进入该页面创建订单、查询订单状态和模拟支付成功

### 桌面历史页

- 路由：`/history-fee`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/billing/history-fee.vue`

当前实现补充：

- 页面为桌面版 `Vue + Element Plus` 历史订单页
- 需要学生登录态后访问，默认从桌面水电费页跳转进入
- 展示最近 30 条充值、刷新结算和周期扣费记录

### 手机端历史页

- 路由：`/history-fee-antd`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/billing/history-fee-antd.vue`

当前实现补充：

- 页面采用手机端 `Ant Design Vue` 风格，使用移动筛选条、订单卡片和摘要区块展示最近账单
- 历史订单卡片默认仅展示事件类型、费用类型、金额增减和时间，其余宿舍、支付方式、付款人等字段折叠到“查看详情”
- 页面会优先通过地址中的 `stuNum` 加载宿舍信息，再用宿舍 `dormId` 查询最近 30 条历史记录
- 从移动版水电费页跳转时，页面会保留公开访问链路所需的查询参数

主要能力：

- 加载最近 30 条费用事件
- 按充值、刷新、周期扣费筛选
- 展示付款人、支付类型、余额、事件描述

## 前端 API

位于 `low-carbon-dormitory-vue/src/api/modules/student.ts`：

- `createStudentPaymentOrder(...)`
- `fetchStudentPaymentOrder(...)`
- `simulateStudentPaymentSuccess(...)`
- `submitStudentPayment(...)`
- `fetchFeeHistory(...)`

## 支付单流程

### 前端流程

`pay-up.vue` 与 `pay-up-antd.vue` 的核心支付顺序一致：

1. 进入页面后调用 `fetchWaterElectricity(...)`，用于获取当前宿舍与单价信息
2. 用户选择：
   - `feeType`
   - `amount`
   - `payType`
3. 点击缴费后调用 `createStudentPaymentOrder(...)`
4. 后端返回：
   - `orderNo`
   - `status`
   - `qrCodeContent`
   - `qrCodeImageUrl`
5. 前端弹出二维码对话框
6. 用户可手动点“刷新状态”或点“模拟支付成功”
7. 如果订单状态变成 `SUCCESS`，页面提示本次积分结算结果

### 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentPaymentController.java`
- Service：`.../service/student/StudentPaymentService.java`

### 创建支付单

接口：

- `POST /student/payments/orders`

`StudentPaymentService.createPaymentOrder(...)` 的主要步骤：

1. 校验费用类型
2. 校验支付方式
3. 解析学生与宿舍
4. 保证宿舍账户存在
5. 生成订单号
6. 生成二维码内容
7. 写入 `student_payment_order`
8. 返回支付单响应

### 查询支付单

接口：

- `GET /student/payments/orders/{orderNo}`

该接口会：

- 按订单号和学生 ID 查询订单
- 返回订单状态、金额、二维码、创建时间、支付时间

### 模拟支付成功

接口：

- `POST /student/payments/orders/{orderNo}/simulate-success`

`StudentPaymentService.simulatePaymentSuccess(...)` 的主要步骤：

1. 校验订单是否存在
2. 校验当前状态是否为 `PENDING`
3. 读取订单中的支付信息
4. 调用真实充值逻辑 `applyRecharge(...)`
5. 更新订单状态为 `SUCCESS`
6. 记录模拟三方交易号
7. 返回支付成功后的订单状态与积分变化

## 直接充值流程

除了支付单模式，系统还保留了直接充值接口：

- `POST /student/pay`

它会直接走 `StudentPaymentService.pay(...)`，内部调用 `applyRecharge(...)`。

`applyRecharge(...)` 的职责包括：

- 校验参数
- 锁定宿舍账户
- 仅更新当前充值费用项的余额
- 写入费用流水
- 触发积分结算
- 返回最新宿舍账务信息

当前约束：

- 学生侧充值不会顺带对水、电两项执行系统估算的自动周期扣费
- 周期扣费事件应来自真实扣费流程，例如管理员扣费或后续接入的真实账务同步

## 账单历史流程

### 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentFeeHistoryController.java`
- Service：`.../service/student/StudentFeeHistoryService.java`

### 接口

- `GET /student/fee-history`

### 实现细节

`StudentFeeHistoryService.getFeeHistory(...)` 的核心逻辑：

1. 校验 `stuNum` 和 `dormId` 至少有一个
2. 规范化分页参数，单页最多 10 条
3. 如提供学号，则先解析学生
4. 如提供宿舍 ID，则先执行 `trimDormHistory(dormId)`
5. 查询 `student_fee_history`
6. 按 `studentId` 或 `dormId` 过滤
7. 通过 `fillPayerInfo(...)` 补齐付款人姓名、学号、账号
8. 返回分页结果

### 特别行为：历史裁剪

`trimDormHistory(dormId)` 会删除该宿舍超过最近 30 条以外的历史记录。  
这说明“历史页展示最近 30 条”不是单纯前端展示限制，而是后端会主动清理旧数据。

## 历史事件类型

前端历史页会根据 `operationType` 把事件映射成：

- `RECHARGE` -> 充值结算
- `REFRESH` -> 刷新结算
- 其他 -> 周期扣费

这层映射发生在：

- `low-carbon-dormitory-vue/src/router/student-router/billing/history-fee.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/history-fee-antd.vue`

## 核心数据对象

前端：

- `PayRequest`
- `PaymentOrder`
- `FeeHistoryRecord`

后端：

- `StudentPayRequest`
- `StudentPaymentOrderResponse`
- `FeeHistoryPageResponse`
- `DormFeeHistory`

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/billing/pay-up.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/pay-up-antd.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/history-fee.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/history-fee-antd.vue`
- `low-carbon-dormitory-vue/src/api/modules/student.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentPaymentController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentFeeHistoryController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPaymentService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentFeeHistoryService.java`

## 维护注意点

- 支付订单目前是“预支付 + 模拟成功”模型，如果接真实支付要补回调与幂等。
- 历史查询会执行数据裁剪，这对审计型需求不友好，调整前要先确认业务是否允许物理删除。
- 刷新、充值、扣费共用同一张历史表，新增 `operationType` 时要同步更新前端历史映射规则。
