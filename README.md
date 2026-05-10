# low-carbon-dormitory

低碳宿舍管理系统工作区，包含前端、后端、项目文档、日志和发布产物目录。

## 目录说明

- `low-carbon-dormitory-vue/`：Vue 3 前端项目代码
- `low-carbon-dormitory-spring/`：Spring Boot 后端项目代码
- `docs/`：项目说明、架构说明、功能说明、部署说明
- `log/`：运行日志、排查记录、构建日志
- `release/package/`：前后端打包产物目录
- `release/docker/`：Docker 相关产物目录

## 本地运行

### 前端

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm install
npm run dev
```

开发环境默认通过 Vite 代理访问后端：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`

### 后端

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw spring-boot:run
```

测试：

```bash
./mvnw test
```

## 生产打包

### 前端构建

在 `low-carbon-dormitory-vue/` 目录执行：

```bash
npm run build
```

构建产物整理要求：

- 将需要发布的前端文件复制或整理到 `release/package/frontend/dist/`
- `low-carbon-dormitory-vue/dist/` 只允许作为构建过程中的临时目录，整理完成后不保留在源码目录中

如需指定生产环境接口前缀，可先在 `low-carbon-dormitory-vue/` 下创建 `.env.production`，例如：

```env
VITE_API_BASE_URL=
```

### 后端构建

在 `low-carbon-dormitory-spring/` 目录执行：

```bash
./mvnw clean package -DskipTests
```

构建产物整理要求：

- 将需要发布的 Jar 和相关启动配置整理到 `release/package/backend/`
- `low-carbon-dormitory-spring/target/` 不保留最终发布用 `.jar`，如构建过程生成，整理完成后应清理

## 当前打包产物

本次仅生成数据库导入包，产物整理在 `release/package/`：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`：本地 PostgreSQL 导出的完整业务 SQL，已处理为兼容 PostgreSQL 12 导入
- `release/package/low-carbon-dormitory-sql-package.zip`：SQL 上传压缩包，内含 `.sql` 文件和导入说明
- `release/package/sql-import-readme.txt`：SQL 导入说明文本

本次构建日志位于：

- `log/package-sql-export.log`

## 部署说明

### 前端

将 `release/package/frontend/dist/` 下的内容上传到 Web 服务器静态目录。

前端请求基址由 `VITE_API_BASE_URL` 控制：

- 开发环境默认值：`/api/dorm`
- 生产环境默认值：空字符串，即直接请求 `/student/**`、`/admin/**`、`/public/**`

如果线上前端部署后登录出现 `404`，通常是因为前端请求了 `/api/dorm/student/login`，但服务器没有把 `/api/dorm/**` 反向代理到 Spring Boot 根路径。

### 后端

将 `release/package/backend/` 上传到服务器后，使用 Java 17 启动：

```bash
java -jar low-carbon-dormitory-0.0.1-SNAPSHOT.jar
```

如需修改数据库连接，可通过环境变量覆盖：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## 数据库配置

当前后端默认连接 PostgreSQL：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`lowcarbon`
- 用户名：`lowcarbon`

后端配置文件位置：

- `low-carbon-dormitory-spring/src/main/resources/application.properties`

数据库部署与导入说明见：

- `docs/database-deployment.md`

如需重新导入服务器，优先上传：

- `release/package/low-carbon-dormitory-sql-package.zip`

## 产物与日志约定

- 打包时只覆盖本次重新生成的产物，不因某类产物本次未生成而删除 `release/package/` 或 `release/docker/` 中其他旧产物
- 最终发布产物统一保存在 `release/package/` 和 `release/docker/`
- `low-carbon-dormitory-vue/` 目录内不保留 `dist/`，`low-carbon-dormitory-spring/target/` 内不保留最终发布用 `.jar`
- 日志统一放在根目录 `log/`
- 说明文档统一放在根目录 `docs/`

## 相关文档

- `docs/database-deployment.md`
- `docs/deployment.md`
- `docs/low-carbon-dormitory-architecture.md`
- `docs/features/README.md`
