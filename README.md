# low-carbon-dormitory

低碳宿舍管理系统工作区，包含前端、后端、项目文档、运行日志和发布产物目录。

## 项目结构

- `low-carbon-dormitory-vue/`：Vue 3 前端项目代码
- `low-carbon-dormitory-spring/`：Spring Boot 后端项目代码
- `docs/`：项目说明、设计说明、功能说明、部署与数据库文档
- `docs/superpowers/plans/`：任务执行计划与实施拆分记录
- `log/`：运行日志、构建日志与排查记录
- `release/package/`：前后端打包产物与 SQL 发布包
- `release/docker/`：Docker 相关文件与镜像构建产物

## 本地联调

### 1. 启动本地 PostgreSQL

后端默认连接本机 PostgreSQL，无需额外改代码：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`lowcarbon`
- 用户名：`lowcarbon`
- 密码：`000000`

对应配置文件：
- `low-carbon-dormitory-spring/src/main/resources/application.properties`

如需临时覆盖，可通过环境变量设置：

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:5432/lowcarbon"
$env:SPRING_DATASOURCE_USERNAME="lowcarbon"
$env:SPRING_DATASOURCE_PASSWORD="000000"
```

### 2. 启动后端

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw spring-boot:run
```

默认服务端口：

- `http://localhost:3000`

### 3. 启动前端

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm install
npm run dev
```

开发环境默认通过 Vite 代理访问后端：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`

前端请求基址规则如下：

- 开发环境默认使用 `/api/dorm`
- 生产环境默认直接请求后端根路径
- 如有需要，可用 `VITE_API_BASE_URL` 显式覆盖

## 登录与路由行为

- 访问 `/` 时会重定向到 `/login`
- 未登录访问学生端或管理端受保护页面时，会被路由守卫拦回 `/login`
- 已有有效登录态时，访问 `/login` 会按角色自动跳转到默认首页
- 学生端移动端页面默认隐藏左侧全局导航，桌面端保留该导航
- 手机奖励兑换页会对奖励列表和兑换记录做前端分页，避免长列表一次性拉满整页高度
- 手机奖励兑换页的“奖励列表 / 兑换记录”视图导航已调整为头部下方居中的嵌入式切换条，不再以悬浮凸起方式覆盖页面
- 手机奖励兑换页现在采用更精简的单列卡片布局：头部与搜索区整体缩小，摘要区仅保留当前积分和推荐奖励
- 手机奖励兑换页头部已移除副标题说明和顶部积分徽标，返回按钮固定在右上角，首屏信息更收敛
- 手机奖励兑换页在 `520px` 和 `390px` 以下屏宽会继续自适应，但不再做过度压缩；摘要区、分页区和兑换按钮会保持可点击尺寸
- 手机奖励兑换页的奖励卡默认只展示奖励图片、奖励名称、所需积分和兑换按钮；详细描述、库存与兑换说明统一收进兑换弹窗
- 手机奖励兑换页在超窄屏下会自动减少每页奖励数量，优先换取卡片留白、按钮可读性和分页稳定性
- 学生端与管理端页面切换统一采用轻量淡入滑入过渡，并在新路由进入后自动回到页面顶部
- 导航点击后会显示全局顶部进度条，当前正在前往的导航项与横幅按钮会进入短暂的 `pending` 高亮态
- 管理端导航与外层壳布局已对齐学生端的绿色视觉主题，学生端与管理端保持统一配色语言
- 学生端低碳模块当前只保留两类积分口径：`个人积分` 和 `宿舍周期积分`；个人积分会在看板、水电、奖励等页面按最新接口结果自动同步
- 宿舍低碳总览中的“优秀/良好/预警/异常”按全局排名分层判定，并对无有效数据宿舍单独降级提示
- 管理端宿舍总览页在大屏下会自动限制内容宽度，避免卡片和表格横向铺得过满
- 管理端宿舍总览页在大屏下会进一步收口内容宽度，保持卡片区和明细区更紧凑
- 管理端宿舍总览页默认每页显示 `6` 个宿舍，卡片区与明细表都会显示当前页、总页数和总数；筛选后会自动回到第一页并同步刷新
- 管理端宿舍明细表已调整为更宽松的汇总表格，关键指标和状态信息按分组展示，减少横向拥挤

登录与统一登录接入说明见：

- `docs/features/auth-and-login.md`

## 数据库与 SQL 包

当前发布目录中保留了本地 PostgreSQL 导出的 SQL 包：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`
- `release/package/low-carbon-dormitory-sql-package.zip`
- `release/package/sql-import-readme.txt`

数据库导入与默认连接说明见：

- `docs/database-deployment.md`

## 构建与发布

### 前端构建

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm run build
```

整理要求：

- 将最终发布文件整理到 `release/package/frontend/dist/`
- `low-carbon-dormitory-vue/dist/` 只作为构建过程临时目录，整理完成后不长期保留

### 后端构建

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw clean package -DskipTests
```

整理要求：

- 将最终发布 Jar 及相关配置整理到 `release/package/backend/`
- `low-carbon-dormitory-spring/target/` 中不长期保留最终发布用 `.jar`

### 服务器试跑包

一次完整打包后，建议用于服务器试跑的产物如下：

- `release/package/frontend/dist/`：前端静态资源
- `release/package/backend/low-carbon-dormitory-0.0.1-SNAPSHOT.jar`：后端可执行包
- `release/package/backend/backend.env.example`：后端环境变量示例
- `release/package/backend/start-backend.sh`：后端启动脚本
- `release/package/nginx/low-carbon-dormitory.conf.example`：Nginx 同域反向代理示例
- `release/package/low-carbon-dormitory-local-postgres-public.sql`：数据库导入脚本

默认推荐“同域部署”试跑：

- 前端页面与后端接口使用同一域名
- 前端生产环境直接请求 `/student/**`、`/admin/**`、`/public/**`
- Nginx 将这些接口路径转发到 `127.0.0.1:3000`

## 产物与文档约定

- 打包时只覆盖本次重新生成的产物，不主动删除本次未生成的新旧其他产物
- 最终发布产物统一保存在 `release/package/` 或 `release/docker/`
- 日志统一放在 `log/`
- 说明文档统一放在 `docs/`

## 相关文档

- `docs/database-deployment.md`
- `docs/deployment.md`
- `docs/low-carbon-dormitory-architecture.md`
- `docs/features/README.md`
