# 宿舍水电查询与刷新实现文档

## 功能目标

该功能用于展示当前学生所在宿舍的：

- 电费余额
- 水费余额
- 可用量估算
- 单价信息
- 最后扣费时间
- 宿舍周期积分与个人积分

并支持执行“刷新”动作，把当前余额同步成一条新的结算事件。

## 前端入口

### 路由与页面

- 路由：`/water-electricity`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/billing/water-electricity.vue`

页面主要能力：

- 加载当前宿舍水电信息
- 展示余额状态
- 调用刷新接口
- 跳转充值页与历史页

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/student.ts`
- 方法：
  - `fetchWaterElectricity(...)`
  - `refreshWaterElectricity(...)`

## 前端状态与行为

页面从 `studentTokenStore` 读取：

- `stuNum`
- `dormId`
- `dormLabel`

核心流程：

1. 页面加载时调用 `fetchWaterElectricity`
2. 后端返回余额、单价、可用量和积分信息
3. 页面根据阈值计算“余额正常 / 余额提醒 / 余额偏低”
4. 点击“刷新数据”时调用 `refreshWaterElectricity`
5. 刷新成功后显示最近一次积分结算摘要
6. 查询与刷新返回的 `personalCarbonScore` 会同步回学生端本地状态，保证侧栏个人积分与页面主内容一致

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentWaterElectricityController.java`
- Service：
  - `StudentContextService`
  - `StudentDormService`
  - `DormFeeAccountService`
  - `StudentPaymentService`

## 查询流程

### 接口

- `GET /student/water-electricity`

### 调用链

1. `StudentWaterElectricityController.waterElectricity(stuNum, dormId)`
2. `StudentContextService.findStudent(stuNum)`
3. `StudentContextService.resolveRequiredDormId(student, dormId)`
4. `DormFeeAccountService.getOrCreate(resolvedDormId)`
5. `StudentDormService.getDormInfo(resolvedDormId)`
6. `StudentDormService.buildWaterElectricityResponse(...)`

### 查询特征

该接口允许两种定位方式：

- 通过 `stuNum`
- 通过 `dormId`

如果提供 `stuNum`，系统会先解析出学生和宿舍。  
如果只提供 `dormId`，系统也可以直接查宿舍账户。

## 刷新流程

### 接口

- `POST /student/water-electricity/refresh`

### 调用链

1. `StudentWaterElectricityController.refreshWaterElectricity(...)`
2. `StudentPaymentService.refreshWaterElectricity(stuNum, dormId)`

### 核心逻辑

`StudentPaymentService.refreshWaterElectricity(...)` 不只是“重新读取”，它会执行一次带副作用的刷新结算：

1. 解析当前学生与宿舍
2. 读取并锁定宿舍账户
3. 找到电费、水费最近一次锚点记录
4. 写入两条 `REFRESH` 类型流水
5. 依据当前事件补加宿舍积分与个人积分
6. 返回最新余额与积分变化

注意：

- 刷新不会再按系统估算结果自动扣减水电余额
- 学生侧充值也只更新目标费用项余额，不会顺带对水、电两项做自动周期扣费
- `lastDeductTime` 仅表示真实扣费动作时间，不再由学生刷新或充值推进

### 为什么刷新会影响积分

从实现上看，系统把“刷新”视为一次显式结算事件。  
因此刷新后响应里可能出现：

- `carbonPointsAdded`
- `personalPointsAdded`

前端页面会把这两个字段转成“最近结算摘要”。

这里的字段口径为：

- `dormCarbonScore`：宿舍周期积分
- `personalCarbonScore`：个人积分
- `carbonPointsAdded` / `personalPointsAdded`：本次刷新新增的积分，不是新的第三种积分类型

## 核心数据对象

前端：

- `StudentWaterElectricity`

后端：

- `StudentWaterElectricityResponse`
- `DormFee`
- `DormInfo`
- `UtilityRateConfig`

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/billing/water-electricity.vue`
- `low-carbon-dormitory-vue/src/api/modules/student.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentWaterElectricityController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPaymentService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentDormService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/DormFeeAccountService.java`

## 维护注意点

- 查询接口和刷新接口共享 `StudentDormService.buildWaterElectricityResponse(...)`，字段调整会同时影响两个行为。
- 刷新会写流水、更新积分，不能把它当成纯只读接口。
- 学生侧余额变化只应来自真实充值或真实扣费，不应再由刷新流程用估算值改写。
- 当前功能与账单历史、缴费功能共用同一套 `student_fee_history` 数据，改流水类型时要同步检查历史页展示逻辑。
