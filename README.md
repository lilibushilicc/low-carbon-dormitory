# low-carbon-dormitory

> 2026-05-16 更新：奖励图片上传已从服务器本地磁盘切换为 Cloudflare R2。后端不再写入 `storage/reward-images/`，也不再提供 `/uploads/rewards/**` 本地静态访问，数据库继续仅保存 `image_url`。  
> 2026-05-16 更新：前端已抽取共享导航与会话持久化工具，统一复用路由跳转、登出清理和 `localStorage` 读写逻辑，减少重复代码。  
> 2026-05-17 更新：管理员后台已支持直接读取、测试并保存奖励图片上传使用的 R2 配置；上传服务优先使用后台保存的配置，未配置时回退到环境变量。
> 2026-05-18 更新：R2 配置页新增“恢复已保存配置”和保存前确认，降低误填、误保存导致上传异常的风险。
> 2026-05-18 更新：水电费率维护已归入“低碳规则配置”页，保存费率后会同步影响规则预览、看板碳排换算和水电扣费逻辑；奖励管理页只保留奖励与 R2 存储配置。

低碳宿舍管理系统工作区，包含前端、后端、项目文档、规划资料、运行日志与发布产物目录。

## 项目结构

- `low-carbon-dormitory-vue/`：Vue 3 前端项目源码
- `low-carbon-dormitory-spring/`：Spring Boot 后端项目源码
- `docs/`：项目说明、功能文档、部署文档
- `log/`：运行日志、构建日志、排查记录
- `release/package/`：打包产物
- `release/docker/`：Docker 相关文件与产物
- `willplan/`：后续规划、预研资料
- `storage/`：运行期存储目录；当前不再用于奖励图片本地上传

说明：

- `low-carbon-dormitory-vue/` 和 `low-carbon-dormitory-spring/` 仅保留源码及运行所需文件
- 日志统一存放到 `log/`
- 项目说明统一存放到 `docs/`
- 未来规划统一存放到 `willplan/`

## 本地运行

### 后端

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw spring-boot:run
```

默认地址：

- `http://localhost:3000`

后端需要配置以下环境变量后，奖励图片上传功能才能正常写入 R2：

- `APP_UPLOAD_R2_ENDPOINT`
- `APP_UPLOAD_R2_ACCESS_KEY_ID`
- `APP_UPLOAD_R2_SECRET_ACCESS_KEY`
- `APP_UPLOAD_R2_BUCKET`
- `APP_UPLOAD_R2_PUBLIC_BASE_URL`
- `APP_UPLOAD_R2_REGION`，默认可用 `auto`

### 前端

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm install
npm run dev
```

开发环境下前端通过代理访问后端：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`
- `/api/restaurant/**` -> `http://39.98.69.153:8081/api/**`

## 奖励图片上传

- 管理端页面：`/manager/reward-manage`
- 上传方式：前端以 `multipart/form-data` 调用 `POST /admin/rewards/upload-image`
- 支持格式：`JPG`、`PNG`、`WEBP`
- 大小限制：单张不超过 `2MB`
- 默认对象 Key：`YYYYMM/<uuid>.<ext>`
- 默认访问地址：`<APP_UPLOAD_R2_PUBLIC_BASE_URL>/YYYYMM/<uuid>.<ext>`

管理员后台现可直接维护以下 R2 参数：

- `endpoint`
- `accessKeyId`
- `secretAccessKey`
- `bucket`
- `publicBaseUrl`
- `region`

R2 配置页当前支持：

- 测试连接
- 保存前确认
- 恢复最近一次加载或保存的配置

数据库仅保存图片 URL，不保存图片二进制内容。

## 登录与注册

现有登录接口：

- 学生登录：`POST /student/login`
- 管理员登录：`POST /admin/login`

学生建档入口保留两种方式：

1. 管理员建档：`POST /admin/students`
2. 外部公开注册：`POST /student/register`

说明：

- 登录页 `low-carbon-dormitory-vue/src/login.vue` 没有新增“注册学生”入口
- 管理端保留“新增学生”和“学生管理”
- `POST /student/register` 主要用于外部系统或第三方页面接入

## 打包与清理约定

- 打包前先清理 `log/` 下与本次构建相关的旧日志
- 前端构建产物最终整理到 `release/package/frontend/dist/`
- 后端构建产物最终整理到 `release/package/backend/`
- 不在 `low-carbon-dormitory-vue/` 长期保留 `dist/`
- 不在 `low-carbon-dormitory-spring/target/` 长期保留 `.jar`

## 相关文档

- [部署说明](./docs/deployment.md)
- [数据库部署说明](./docs/database-deployment.md)
- [管理员奖励与 R2 存储管理说明](./docs/features/admin-reward-and-rate-management.md)
- [低碳规则查看与配置](./docs/features/low-carbon-rules.md)
- `docs/features/auth-and-login.md`
- `docs/features/admin-student-create.md`
- `docs/features/admin-student-management.md`
- `docs/features/student-register.md`
