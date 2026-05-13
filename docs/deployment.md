# 部署说明

## 0. 构建产物约定

- 打包时仅覆盖本次重新生成的发布产物，不因某类产物本次未生成而删除 `release/package/` 或 `release/docker/` 中其他旧文件
- 最终对外发布的前后端产物统一整理到根目录 `release/package/` 或 `release/docker/`
- `low-carbon-dormitory-vue/dist/` 和 `low-carbon-dormitory-spring/target/` 仅允许作为构建过程中的临时输出目录，整理完成后不应继续保留前端 `dist/` 和后端发布用 `.jar`

## 1. 前后端地址关系

当前项目的后端接口本身暴露的是这些根路径：

- `POST /student/login`
- `POST /admin/login`
- 其他学生端与管理端接口也都直接挂在 `/student/**`、`/admin/**`、`/public/**`

前端开发环境之所以可以请求成功，是因为 `vite.config.ts` 配置了本地代理：

- `/api/dorm/**` -> `http://localhost:3000/**`
- `/api/**` -> `http://localhost:3000/**`

这意味着：

- 开发环境中，浏览器访问 `/api/dorm/student/login` 时，会被 Vite 代理转发成后端的 `/student/login`
- 生产环境中，如果没有配置反向代理，而前端仍然请求 `/api/dorm/student/login`，就会直接得到 `404`

## 2. 前端 API 基址配置

前端现在支持通过环境变量 `VITE_API_BASE_URL` 控制宿舍系统请求前缀，通过 `VITE_RESTAURANT_API_BASE_URL` 控制餐厅模块请求前缀：

- 开发环境默认值：`/api/dorm`
- 生产环境默认值：空字符串，即直接请求 `/student/login`、`/admin/login`
- 餐厅模块开发环境默认值：`/api/restaurant`
- 餐厅模块生产环境默认值：`http://39.98.69.153:8081/api`

可在 `low-carbon-dormitory-vue/.env.example` 基础上创建实际环境文件，例如：

```env
VITE_API_BASE_URL=
VITE_RESTAURANT_API_BASE_URL=
```

当前仓库已新增 `low-carbon-dormitory-vue/.env.production`，其中默认配置为：

```env
VITE_API_BASE_URL=
VITE_RESTAURANT_API_BASE_URL=/api/restaurant
```

这表示生产打包后的前端页面会同域请求 `/api/restaurant/**`，不再直接从浏览器跨域访问 `http://39.98.69.153:8081/api/**`。

常见可选值：

- `VITE_API_BASE_URL=`：前端与后端同域部署，后端直接暴露根路径
- `VITE_API_BASE_URL=/api`：服务器把后端统一挂到 `/api`
- `VITE_API_BASE_URL=/api/dorm`：服务器显式保留了 `/api/dorm` 这一层代理前缀
- `VITE_RESTAURANT_API_BASE_URL=/api/restaurant`：开发环境通过 Vite 代理转发餐厅接口，避免浏览器直接跨域到远端餐厅服务
- `VITE_RESTAURANT_API_BASE_URL=http://39.98.69.153:8081/api`：生产环境或允许直连的环境直接访问默认餐厅接口

如果使用当前仓库默认的生产配置，则服务器必须额外提供：

- `/api/restaurant/**` -> `http://39.98.69.153:8081/api/**`

否则部署后餐厅首页、按周碳排放、按月碳排放等页面会因为请求不到餐厅接口而显示无数据或报错。

## 3. 推荐部署方式

### 方案 A：同域直连后端根路径

适用场景：

- 前端静态资源与后端接口在同一域名下
- Web 服务器直接把 `/student/**`、`/admin/**`、`/public/**` 转发给 Spring Boot

前端建议：

```env
VITE_API_BASE_URL=
```

### 方案 B：通过 `/api` 反向代理后端

适用场景：

- 希望把后端接口统一收敛到 `/api/**`

前端建议：

```env
VITE_API_BASE_URL=/api
```

此时需要保证服务器把：

- `/api/student/**` 转发到后端 `/student/**`
- `/api/admin/**` 转发到后端 `/admin/**`
- `/api/public/**` 转发到后端 `/public/**`

### 方案 C：继续沿用 `/api/dorm`

如果线上已经约定前端请求 `/api/dorm/**`，也可以保留：

```env
VITE_API_BASE_URL=/api/dorm
```

但必须同时保证 Web 服务器存在对应反向代理，把 `/api/dorm/**` 转发到 Spring Boot 根路径。

## 4. 本次 404 的直接原因

浏览器控制台里的登录 404，通常就是下面这类不匹配：

- 前端发出：`/api/dorm/student/login`
- 后端实际只有：`/student/login`
- 线上缺少 `/api/dorm` -> `/` 的代理规则

## 5. 排查顺序

出现登录 404 时，优先检查：

1. 浏览器网络面板里实际请求的 URL 是什么
2. Spring Boot 是否正常启动在 `3000` 端口
3. 前端构建时是否配置了正确的 `VITE_API_BASE_URL`
4. Nginx 或网关是否把对应前缀正确转发到后端

## 6. 当前仓库的试跑发布包

执行过一次成功构建后，当前项目建议按下面的发布包结构上传服务器：

- `release/package/frontend/dist/`：前端静态文件
- `release/package/backend/low-carbon-dormitory-0.0.1-SNAPSHOT.jar`：Spring Boot 可执行包
- `release/package/backend/backend.env.example`：后端环境变量示例
- `release/package/backend/start-backend.sh`：Linux 启动脚本
- `release/package/nginx/low-carbon-dormitory.conf.example`：同域试跑的 Nginx 示例配置
- `release/package/low-carbon-dormitory-local-postgres-public.sql`：数据库导入脚本

其中前端默认按“同域直连后端根路径”构建，即：

- 页面请求会直接访问 `/student/**`、`/admin/**`、`/public/**`
- Nginx 只需要把这些路径转发到 Spring Boot 即可

## 7. 服务器试跑步骤

### 7.1 导入数据库

先在服务器准备 PostgreSQL 数据库与账号，再执行：

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f low-carbon-dormitory-local-postgres-public.sql
```

### 7.2 启动后端

1. 进入 `release/package/backend/`
2. 复制环境变量模板：

```bash
cp backend.env.example backend.env
```

3. 按服务器实际数据库修改 `backend.env`
4. 启动服务：

```bash
chmod +x start-backend.sh
./start-backend.sh
```

### 7.3 部署前端

把 `release/package/frontend/dist/` 上传到服务器静态目录，例如：

```bash
/srv/low-carbon-dormitory/frontend/dist
```

### 7.4 配置 Nginx

如果采用同域试跑，可参考：

- `release/package/nginx/low-carbon-dormitory.conf.example`

核心要求只有两点：

1. `/student/**`、`/admin/**`、`/public/**` 必须反向代理到 `127.0.0.1:3000`
2. 前端路由必须回退到 `/index.html`

## 8. 本次试跑前建议重点验证

上线后优先验证这些逻辑是否正常：

1. 登录接口是否返回 `200`，而不是 `404`
2. 学生端和管理端路由守卫是否会错误重定向
3. 奖励兑换、水电、低碳看板页面是否能正常读取最新数据
4. 切换学生端/管理端后，本地 token 是否会串用或残留
