# 宿舍水电查询与刷新

## 功能说明

学生端用于查看宿舍水电余额、单价、可用量、最近扣费时间和积分信息，并支持刷新水电信息、进入充值页和历史页。

相关页面：

- `low-carbon-dormitory-vue/src/router/student-router/billing/water-electricity.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/water-electricity-antd.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/pay-up.vue`
- `low-carbon-dormitory-vue/src/router/student-router/billing/pay-up-antd.vue`

相关后端：

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentWaterElectricityController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentDormService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPaymentService.java`

## 接口

- `GET /student/water-electricity`
- `POST /student/water-electricity/refresh`
- `POST /student/payments/orders`

## 水费不计费开关

系统现在支持对 `WATER` 单独关闭计费。

实现方式：

- 配置来源为 `system_utility_rate_config.enabled`
- 学生端水电信息接口会返回：
- `waterBillingEnabled`
- `electricityBillingEnabled`

关闭水费计费后的效果：

- 学生仍可查看当前水费余额和历史余额数据。
- 学生端不能再创建水费充值订单。
- 刷新水电信息时，不再为水费写入新的 `REFRESH` 记录。
- 低碳看板和个人低碳统计不再把水费计入费用、用量和碳排计算。
- 支付页如果检测到水费已关闭，会自动切换到电费，并禁止再切回水费充值。

## 维护注意点

- 查询接口和刷新接口共用 `StudentDormService.buildWaterElectricityResponse(...)`，字段变更会同时影响桌面端和移动端。
- 刷新接口不是纯查询，它会写费用流水，并可能触发积分结算。
- 若后续需要把“停止计费”扩展为“隐藏水费模块”，需要继续同步调整前端展示层，而不仅是后端拦截。
