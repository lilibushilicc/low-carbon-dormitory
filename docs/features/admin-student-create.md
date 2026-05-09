# 管理员学生创建实现文档

## 功能目标

该功能用于在管理端录入新学生，并同步完成：

- 学生基础资料创建
- 宿舍关联建立
- 宿舍账户初始化
- 宿舍床位可用数同步

## 前端入口

### 路由与页面

- 路由：`/manager/student-create`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/student/student-create.vue`

页面收集的字段包括：

- 学号
- 姓名
- 密码
- 宿舍楼栋
- 宿舍房间
- 宿舍人数
- 性别
- 手机号
- 身份证号
- 学院
- 专业
- 班级
- 年级
- 初始低碳积分

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/admin.ts`
- 方法：`createStudentByAdmin(payload)`
- 请求：`POST /admin/students`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- Service：`.../service/admin/AdminManagementService.java`

## 核心调用链

1. `AdminManagementController.createStudent(...)`
2. `AdminManagementService.createStudent(request)`

## 创建流程

`AdminManagementService.createStudent(...)` 的核心逻辑：

1. 取出学号并去重校验
2. 调用 `findOrCreateDorm(...)`
3. 调用 `buildStudent(...)`
4. 插入 `student_base`
5. 调用 `ensureDormFeeExists(dormId)` 初始化宿舍费用账户
6. 调用 `syncDormBedAvailable(dormId)` 同步宿舍可用床位
7. 返回新学生 ID、学号、宿舍 ID

## 宿舍创建逻辑

`findOrCreateDorm(...)` 的实现特征：

- 如果宿舍已存在，则复用该宿舍
- 如果已存在但床位总数不一致，则直接报错
- 如果不存在，则创建新的 `student_dorm_info`

这说明“学生创建”同时承担了宿舍初始化入口的角色。

## 初始化的附带效果

新增学生不仅是插入一条学生记录，还会连带影响：

- 宿舍基础信息表
- 宿舍费用账户表
- 宿舍可用床位统计

因此它本质上是一个“学生 + 宿舍”复合创建流程。

## 核心数据对象

前端：

- `AdminCreateStudentRequest`
- `AdminCreateStudentResponse`

后端：

- `AdminCreateStudentRequest`
- `AdminCreateStudentResponse`
- `StudentBase`
- `DormInfo`

## 相关文件

- `low-carbon-dormitory-vue/src/router/manager-router/student/student-create.vue`
- `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

## 维护注意点

- 当前密码是明文写入 `student_base.password`，如果后续接入密码加密，这里要和学生登录一起改。
- 宿舍已存在时会校验床位数一致，导入历史数据时要注意这个约束。
- 如果后续把学生创建拆成“先建宿舍、再分配学生”，需要同步拆分当前服务职责。
