# low-carbon-dormitory

> 2026-05-12 更新：学生端全局左侧导航现在仅在“学生路由 + 已登录 + 非移动端”条件下显示，未登录访问时不会再渲染左侧导航栏。

> 2026-05-12 更新：已抽取手机端水电链路的查询参数与宿舍 ID 解析工具，`/water-electricity-antd`、`/pay-up-antd`、`/history-fee-antd` 复用同一套 `stuNum`、`dormId`、`source=utility-mobile` 拼装规则，减少重复代码并保持移动端跳转闭环。

> 2026-05-12 更新：手机端 `/pay-up-antd` 缴费成功后会固定返回手机端 `/water-electricity-antd` 水电页面，避免登录态移动访问因缺少查询参数跳回桌面水电页。

> 2026-05-12 更新：`pay-up` 与 `history-fee` 已恢复为桌面版页面，并新增 `pay-up-antd` 与 `history-fee-antd` 两个手机端 `Ant Design Vue` 页面，专门用于嵌入式移动访问；从 `/water-electricity-antd?stuNum=学号` 进入时，会继续透传 `stuNum` 到这两个手机页。
> 2026-05-12 更新：桌面版 `/pay-up` 页面样式已调整为居中大白卡布局，参考现网缴费页的标题、宿舍条、单价卡、费用切换、金额输入与支付方式排布。
> 2026-05-12 更新：移动端 `pay-up` 与 `history-fee` 页面进一步压缩了头部和卡片尺寸；历史订单默认只展示金额增减与费用类型，其余字段改为按条展开查看详情。
> 2026-05-12 更新：已移除桌面学生页中进入手机端页面的可视化入口；手机端页面现在仅支持通过 URL 直接访问。

> 2026-05-11 更新：新增学生端 `water-electricity-antd` 手机版水电费页面，采用 Ant Design Vue 风格展示当前余额、近七天费用构成环形图、近七天费用趋势折线图与最近缴费记录。
> 2026-05-11 更新：`/water-electricity-antd` 现支持通过 `?stuNum=学号` 方式未登录直达访问，便于外部移动端按学号打开对应学生的水电费手机页面。

> 2026-05-11 更新：奖励兑换手机端背景图静态资源已放入 `low-carbon-dormitory-vue/public/images/reward-exchange-mobile-bg.jpg`，供 `/reward-exchange-antd` 页面使用；该页面现按手机端全屏响应布局展示，背景图直接参与页面视觉，奖励列表与兑换记录两种视图统一使用同一套内容盒尺寸与间距规则。

低碳宿舍管理系统工作区，包含前端、后端、项目文档、规划文件、运行日志与发布产物目录。

## 项目结构

- `low-carbon-dormitory-vue/`：Vue 3 前端项目源码
- `low-carbon-dormitory-spring/`：Spring Boot 后端项目源码
- `docs/`：项目说明、功能文档、部署文档
- `WillPlan/`：将来实施的计划、预研材料、待落地方案
- `log/`：运行日志、构建日志、排查记录
- `release/package/`：打包产物
- `release/docker/`：Docker 相关文件和镜像构建产物

说明：

- `low-carbon-dormitory-vue/` 和 `low-carbon-dormitory-spring/` 仅保留源码及运行所需文件
- 日志文件统一放在 `log/`
- 说明文档统一放在 `docs/`
- 将来实施的计划和规划文件统一放在 `WillPlan/`

## 本地运行

### 后端

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw spring-boot:run
```

默认服务地址：

- `http://localhost:3000`

### 前端

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm install
npm run dev
```

开发环境下前端通过代理访问后端：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`

## 登录与注册

### 现有登录接口

- 学生登录：`POST /student/login`
- 管理员登录：`POST /admin/login`

### 学生注册入口

当前项目保留两种学生建档方式：

1. 管理员建档：`POST /admin/students`
2. 外部公开注册：`POST /student/register`

说明：

- 登录页 `low-carbon-dormitory-vue/src/login.vue` 没有新增“注册学生”入口
- 管理端保留“新增学生”，并新增“学生管理”页面用于查看学生列表和删除学生
- `POST /student/register` 主要供外部系统或第三方页面传参调用

### 管理员学生管理

当前管理员端学生相关能力包括：

- `GET /admin/students`：获取学生列表
- `GET /admin/students/{studentId}/delete-check`：删除前校验是否存在历史关联
- `DELETE /admin/students/{studentId}`：删除学生

删除规则：

- 仅当学生不存在奖励兑换记录、费用流水记录、支付订单记录时允许删除
- 删除时会同步清理 `student_profile` 扩展档案
- 删除后会重算宿舍空床数
- 不会自动删除宿舍信息与宿舍水电账户

### `POST /student/register` 请求体

```json
{
  "stuNum": "20260001",
  "name": "张三",
  "password": "123456",
  "dormBuilding": "1号楼",
  "dormRoom": "101",
  "bedTotal": 4,
  "gender": 1,
  "idCard": "110101200001010011",
  "phone": "13800000000",
  "college": "计算机学院",
  "major": "软件工程",
  "className": "软工1班",
  "grade": "2026级"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "studentId": 123,
    "stuNum": "20260001",
    "dormId": 45
  }
}
```

注册成功后可直接调用 `POST /student/login` 登录。

## 打包与清理约定

- 打包前先清理 `log/` 下与本次打包相关的旧日志
- 前端构建产物最终整理到 `release/package/frontend/dist/`
- 后端构建产物最终整理到 `release/package/backend/`
- 不在 `low-carbon-dormitory-vue/` 长期保留 `dist/`
- 不在 `low-carbon-dormitory-spring/target/` 长期保留最终 `.jar`
- `low-carbon-dormitory-vue/` 和 `low-carbon-dormitory-spring/` 不存放长期日志、文档或规划文件

## 相关文档

- `docs/features/README.md`
- `docs/features/auth-and-login.md`
- `docs/features/admin-student-create.md`
- `docs/features/admin-student-management.md`
- `docs/features/student-register.md`
- `docs/deployment.md`
- `docs/database-deployment.md`
