# 管理员学生管理与删除实现文档

## 功能概述

该功能用于系统管理员查看学生列表，并在满足删除约束时删除学生档案。

当前实现包含两部分：

- 管理员学生列表：查看学号、姓名、宿舍、学院、专业、积分等基础信息
- 管理员删除学生：仅允许删除没有历史业务数据关联的学生

## 前端入口

- 管理端导航：`low-carbon-dormitory-vue/src/components/admin-global-nav.vue`
- 管理页面：`low-carbon-dormitory-vue/src/router/manager-router/student/student-manage.vue`
- 前端 API：`low-carbon-dormitory-vue/src/api/modules/admin.ts`

管理页路由：

- `GET /manager/student-manage`

## 后端接口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

接口列表：

- `GET /admin/students`
- `GET /admin/students/{studentId}/delete-check`
- `DELETE /admin/students/{studentId}`

这三个接口都要求管理员 token。

## 删除约束

删除学生时，不是简单删除 `student_base` 一条记录，而是先检查关联数据。

### 允许自动清理的数据

- `student_profile`

这是学生扩展档案，属于附属资料。删除学生时会一并删除，不作为阻塞条件。

### 会阻止删除的数据

- `student_reward_exchange`
- `student_fee_history`
- `student_payment_order`

只要以上任一表存在该学生的历史记录，后端就会拒绝删除，并返回明确原因，例如：

- 已有 1 条奖励兑换记录
- 已有 3 条费用流水记录
- 已有 2 条支付订单记录

## 宿舍关联处理

学生删除成功后，会同步重算该宿舍的 `bed_available`：

- 学生从 `student_base` 删除
- 重新统计当前宿舍剩余在住人数
- 用 `bed_total - resident_count` 回写空床数

当前版本不会自动删除以下宿舍维度数据：

- `student_dorm_info`
- `student_dorm_fee`

这样做的原因是宿舍本身和宿舍水电账户属于宿舍维度数据，不应该随着单个学生删除而直接清空。

## 设计取舍

当前选择的是“严格物理删除 + 历史数据阻断”策略，而不是软删除。

优点：

- 规则简单，风险可控
- 不会把历史账务、订单和兑换数据删坏
- 不会破坏当前按宿舍维度统计的逻辑

限制：

- 有历史记录的学生暂时不能删除
- 如果后续业务需要“离校归档但保留历史”，更适合演进为软删除或状态停用

## 测试覆盖

测试文件：

- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/AdminStudentManagementControllerTest.java`

已覆盖：

- 无历史业务数据的学生可删除
- 删除后学生扩展档案会一并清理
- 删除后宿舍空床数会同步重算
- 有支付订单历史的学生禁止删除
