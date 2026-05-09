# 个人信息查询实现文档

## 功能目标

该功能用于展示当前学生的个人资料与宿舍资料，并在前端本地缓存和后端最新数据之间做同步。

## 前端入口

### 路由与页面

- 路由：`/personal-info`
- 页面：`low-carbon-dormitory-vue/src/router/student-router/profile/personal-info.vue`

页面行为：

- 初始优先读取 `studentTokenStore` 中缓存的 `studentInfo`
- 页面挂载后调用接口刷新数据
- 刷新成功后回写到 store

### 前端 API

- 文件：`low-carbon-dormitory-vue/src/api/modules/student.ts`
- 方法：`fetchStudentProfile(stuNum)`
- 请求：`GET /student/profile`

### 前端状态流

1. `personal-info.vue` 从 `studentTokenStore` 读取：
   - `stuNum`
   - `studentInfo`
   - `dormLabel`
2. 页面挂载时调用 `loadProfile()`
3. `loadProfile()` 调用 `fetchStudentProfile(stuNum)`
4. 成功后调用 `studentTokenStore.updateStudentProfile(profileData)`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentProfileController.java`
- Service：
  - `.../service/student/StudentContextService.java`
  - `.../service/student/StudentDormService.java`

## 后端调用链

`GET /student/profile` 的核心调用链如下：

1. `StudentProfileController.profile(stuNum)`
2. `StudentContextService.getRequiredStudent(stuNum)`
3. `StudentDormService.buildStudentProfile(student)`

## 核心实现细节

### 学生定位

`StudentContextService.getRequiredStudent(...)` 负责：

- 校验学号不为空
- 通过 `StudentBaseMapper` 查询学生
- 若学生不存在则抛业务异常
- 调用 `StudentDormService.attachResolvedDormId(...)` 补齐宿舍 ID

### 资料拼装

`StudentDormService.buildStudentProfile(...)` 负责组合多个来源的数据：

- `student_base` 基础资料
- `student_ext` 个性签名、入学日期等扩展资料
- `student_dorm_info` 宿舍类型、床位等信息

最终返回的字段包括：

- 学号、姓名、性别、手机号
- 学院、专业、班级、年级
- 宿舍楼栋、房间、宿舍号、宿舍类型
- 床位总数、剩余床位
- 个人低碳积分

### 宿舍号处理

宿舍号不是数据库单独字段，而是运行时通过：

- `buildDormNo(dormBuilding, dormRoom)`

拼成 `楼栋-房间` 的展示值。

## 关键数据对象

前端：

- `StudentProfile`

后端：

- `StudentProfileResponse`
- `StudentBase`
- `StudentExt`
- `DormInfo`

## 相关文件

- `low-carbon-dormitory-vue/src/router/student-router/profile/personal-info.vue`
- `low-carbon-dormitory-vue/src/stores/student-token.ts`
- `low-carbon-dormitory-vue/src/api/modules/student.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentProfileController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentContextService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentDormService.java`

## 维护注意点

- `studentInfo` 既是登录返回值的一部分，也是个人资料页的缓存源，修改响应结构时要同步检查登录流程。
- `StudentDormService.buildStudentProfile(...)` 同时被登录与资料页复用，字段改动会同时影响两个功能。
- 如果后续加强鉴权，`GET /student/profile` 现在是按 `stuNum` 查询的，需要重新评估是否继续允许前端显式传学号。
