# 宿舍水电查询与刷新实现文档

## 功能目标

该功能用于展示当前学生所在宿舍的：

- 电费余额
- 水费余额
- 可用量估算
- 单价信息
- 最后扣费时间
- 宿舍与个人低碳积分

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

`StudentPaymentService.refreshWaterElectricity(...)` 不只是“重新读取”，它会执行完整的业务结算：

1. 解析当前学生与宿舍
2. 读取并锁定宿舍账户
3. 执行周期扣费结算
4. 找到电费、水费最近一次锚点记录
5. 写入两条 `REFRESH` 类型流水
6. 依据当前事件补加宿舍积分与个人积分
7. 返回最新余额与积分变化

### 为什么刷新会影响积分

从实现上看，系统把“刷新”视为一次显式结算事件。  
因此刷新后响应里可能出现：

- `carbonPointsAdded`
- `personalPointsAdded`

前端页面会把这两个字段转成“最近结算摘要”。

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
- 当前功能与账单历史、缴费功能共用同一套 `student_fee_history` 数据，改流水类型时要同步检查历史页展示逻辑。
