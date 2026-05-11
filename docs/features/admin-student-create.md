# 管理员学生创建实现文档

## 功能目标

该功能用于管理员在管理端录入新学生，并同步完成：

- 学生基础资料创建
- 宿舍关联建立
- 宿舍费用账户初始化
- 宿舍可用床位同步

## 前端入口

### 路由与页面

- 路由：`/manager/student-create`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/student/student-create.vue`

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/admin.ts`
- 方法：`createStudentByAdmin(payload)`
- 请求：`POST /admin/students`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- 管理服务：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- 共享注册服务：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentRegistrationService.java`

## 调用链路

1. `AdminManagementController.createStudent(...)`
2. `AdminManagementService.createStudent(...)`
3. `StudentRegistrationService.createByAdmin(...)`

## 创建流程

管理员建档与公开注册共用一套核心建档逻辑：

1. 校验学号是否重复
2. 查找或创建宿舍
3. 写入 `student_base`
4. 初始化宿舍费用账户
5. 同步宿舍可用床位

## 与公开注册的区别

管理员建档和公开注册的共同点：

- 都会创建学生档案
- 都会处理宿舍与费用账户初始化
- 都会同步床位余量

管理员建档的额外能力：

- 可以显式传入 `carbonScore`
- 仍属于管理员操作链路
- 继续通过管理端页面触发

## 维护注意点

- 当前密码仍为明文存储，若后续接入加密，需要同时修改注册与登录逻辑
- 若后续拆分“先建宿舍，再分配学生”，需要同步调整 `StudentRegistrationService`
- 新增公开注册接口后，管理员入口依然保留，二者职责不同，不要相互替代
