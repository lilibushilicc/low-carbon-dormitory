# Admin Student Delete Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为系统管理员补齐学生列表与删除功能，并在删除时安全处理宿舍关联和历史数据约束。

**Architecture:** 后端在现有 `AdminManagementController` / `AdminManagementService` 上补学生列表与删除接口，删除前检查学生是否存在历史账单、支付订单、奖励兑换或扩展档案等关联。前端新增学生管理页，复用管理端导航和现有样式体系，通过列表展示、删除确认和失败原因提示完成交互。

**Tech Stack:** Spring Boot, MyBatis-Plus, JdbcTemplate, Vue 3, TypeScript, Element Plus

---

### Task 1: 明确删除规则并补后端接口

**Files:**
- Modify: `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Modify: `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- Modify: `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentRegistrationService.java`
- Create: `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/dto/response/AdminStudentListItemResponse.java`

- [ ] 增加管理员学生列表接口
- [ ] 增加管理员删除学生接口
- [ ] 删除前检查 `student_profile`、`student_reward_exchange`、`student_fee_history`、`student_payment_order` 是否存在关联
- [ ] 删除成功后重算宿舍 `bed_available`
- [ ] 保留宿舍与宿舍水电账户，不自动删除历史宿舍数据

### Task 2: 补前端学生管理页与删除交互

**Files:**
- Modify: `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- Modify: `low-carbon-dormitory-vue/src/components/admin-global-nav.vue`
- Modify: `low-carbon-dormitory-vue/src/router/router.ts`
- Create: `low-carbon-dormitory-vue/src/router/manager-router/student/student-manage.vue`

- [ ] 新增管理员学生列表 API 和删除 API
- [ ] 在导航中加入“学生管理”入口
- [ ] 新增管理页，展示学号、姓名、宿舍、学院、积分和删除按钮
- [ ] 删除时展示二次确认，失败时展示后端返回的约束原因

### Task 3: 补测试与文档

**Files:**
- Create: `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/AdminStudentManagementControllerTest.java`
- Modify: `README.md`
- Modify: `docs/features/README.md`
- Create: `docs/features/admin-student-management.md`

- [ ] 覆盖“无历史关联可删除”
- [ ] 覆盖“有历史关联禁止删除”
- [ ] 更新 README 中管理员学生功能说明
- [ ] 更新功能文档索引与新增学生管理文档
