# Cleanup And Package Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 清理本次仓库中可确认的冗余代码，并重新打包前后端产物到 `release/package/`。

**Architecture:** 优先处理已经改动过且能证明不影响行为的冗余点，避免扩大修改面。完成代码清理后分别执行前端和后端构建，把新产物整理到发布目录，并清理源码目录中的临时构建输出。

**Tech Stack:** Vue 3、TypeScript、Vite、Spring Boot、Maven、PowerShell

---

### Task 1: 清理前端冗余实现

**Files:**
- Modify: `low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange-antd.vue`
- Delete: `low-carbon-dormitory-vue/src/router/student-router/reward/reward-exchange-antd1.vue`

- [ ] **Step 1: 识别奖励页中的重复分页状态与未引用页面文件**
- [ ] **Step 2: 抽取统一的分页状态逻辑，保持奖励列表与兑换记录行为不变**
- [ ] **Step 3: 删除未被路由或组件引用的重复页面文件**
- [ ] **Step 4: 运行前端类型检查与构建，确认清理未引入回归**

### Task 2: 重新整理发布产物

**Files:**
- Update output: `release/package/frontend/dist/`
- Update output: `release/package/backend/`
- Verify: `README.md`
- Verify: `docs/deployment.md`
- Verify: `docs/database-deployment.md`

- [ ] **Step 1: 执行前端构建并将新生成的 `dist` 整理到 `release/package/frontend/dist/`**
- [ ] **Step 2: 执行后端构建并将新的 Jar 保留在 `release/package/backend/`**
- [ ] **Step 3: 清理 `low-carbon-dormitory-vue/dist/` 与 `low-carbon-dormitory-spring/target/` 中不应长期保留的发布产物**
- [ ] **Step 4: 复核 README 与现有部署文档是否仍与当前产物位置和运行方式一致**
