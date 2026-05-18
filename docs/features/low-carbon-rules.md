# 低碳规则查看与配置实现文档

## 功能目标

该功能用于维护和展示低碳评分规则，主要包括两类内容：

- 评分规则
- 荣誉规则

学生端只读查看，管理员端可以编辑并预览公式效果。

## 前端入口

### 学生端只读页

- 路由：`/low-carbon-rule-readonly`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/rules/low-carbon-rule-readonly.vue`

### 管理端配置页

- 路由：`/manager/low-carbon-rule-config`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/config/low-carbon-rule-config.vue`

页面能力：

- 加载当前规则配置
- 维护水电费率配置
- 编辑评分参数
- 编辑荣誉规则列表
- 用示例电费和水费做公式预览
- 保存整套规则

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/low-carbon-rule.ts`
- 方法：
  - `fetchLowCarbonRuleConfig()`
  - `updateLowCarbonRuleConfig(payload)`
  - `previewLowCarbonRule(payload)`
  - `fetchStudentLowCarbonRules()`

水电费率使用 `low-carbon-dormitory-vue/src/api/modules/admin.ts` 中的：

- `fetchUtilityRates()`
- `updateUtilityRate(...)`

## 后端入口

### 学生端

- Controller：`.../controller/student/StudentLowCarbonRuleController.java`
- 接口：`GET /student/low-carbon-rules`

### 管理端

- Controller：`.../controller/admin/LowCarbonRuleAdminController.java`
- 接口：
  - `GET /admin/low-carbon-rules`
  - `PUT /admin/low-carbon-rules`
  - `POST /admin/low-carbon-rules/preview`
  - `GET /admin/utility-rates`
  - `PUT /admin/utility-rates/{feeType}`

## 水电费率配置

水电费率现在放在管理端“低碳规则配置”页维护，而不是放在奖励管理页。原因是水电单价并不是奖励业务配置，它直接参与以下计算：

- 规则预览：费用除以单价得到用量，再乘以水/电碳排系数。
- 宿舍看板：看板按当前费率和当前低碳规则实时换算碳排与积分。
- 扣费限制：当某项费率关闭计费时，对应费用项不参与计费和低碳指标计算。

保存费率后，页面会重新执行规则预览，避免预览结果仍使用旧单价。

## 核心服务

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/rule/LowCarbonRuleService.java`

这是规则功能的核心服务，负责：

- 读取当前有效评分规则
- 读取当前有效荣誉规则
- 提供学生只读视图
- 更新整套规则
- 预览公式计算结果

## 读取规则流程

读取逻辑核心入口：

- `getCurrentConfig()`
- `getReadonlyConfig()`
- `getActiveScoreRuleSnapshot()`

读取过程：

1. 从 `student_low_carbon_score_rule` 中取当前有效规则
2. 从 `student_low_carbon_honor_rule` 中取当前有效荣誉规则
3. 如果数据库中没有规则，则构造默认规则
4. 转换成 `LowCarbonRuleConfigResponse`

## 保存规则流程

管理员保存配置时，走：

- `LowCarbonRuleService.updateConfig(...)`

当前实现比较直接：

1. 清空现有评分规则表
2. 清空现有荣誉规则表
3. 插入新的评分规则
4. 逐条插入新的荣誉规则
5. 返回新的配置视图

这说明当前保存策略是“整套覆盖”，不是增量修改。

## 预览流程

管理员页面点击“预览结果”时会调用：

- `POST /admin/low-carbon-rules/preview`

服务端流程：

1. 使用当前表单里的评分规则，或当前已生效规则
2. 读取水电单价
3. 构造临时 `FeeAggregate`
4. 调用 `LowCarbonDashboardSupport.buildMetric(...)`
5. 返回：
   - 电量换算
   - 水量换算
   - 电碳排
   - 水碳排
   - 总碳排
   - 得分
   - 公式文本
   - 荣誉规则预览文本

## 与看板功能的关系

看板功能不会自己保存规则，它只消费 `LowCarbonRuleService` 提供的规则快照。  
因此这里的规则配置会直接影响：

- 宿舍总览看板
- 个人低碳看板
- 积分与排名解释文本

## 核心数据对象

前端：

- `ScoreRuleConfig`
- `HonorRuleConfig`
- `LowCarbonRuleConfig`
- `LowCarbonRulePreview`

后端：

- `LowCarbonRuleConfigRequest`
- `LowCarbonRulePreviewRequest`
- `LowCarbonRuleConfigResponse`
- `LowCarbonRulePreviewResponse`
- `LowCarbonScoreRule`
- `LowCarbonHonorRule`

## 相关文件

- `low-carbon-dormitory-vue/src/router/manager-router/config/low-carbon-rule-config.vue`
- `low-carbon-dormitory-vue/src/api/modules/low-carbon-rule.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentLowCarbonRuleController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/LowCarbonRuleAdminController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/rule/LowCarbonRuleService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardSupport.java`

## 维护注意点

- 保存逻辑当前是全量删除再插入，后续如果要支持版本化、审计或局部修改，需要重构。
- 默认规则是代码内建的，数据库空表时不会报错，而是自动回退默认值。
- 页面预览结果依赖当前水电单价，所以改费率后预览值也会变化。
