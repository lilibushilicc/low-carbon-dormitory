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

前端现在支持通过环境变量 `VITE_API_BASE_URL` 控制请求前缀：

- 开发环境默认值：`/api/dorm`
- 生产环境默认值：空字符串，即直接请求 `/student/login`、`/admin/login`

可在 `low-carbon-dormitory-vue/.env.example` 基础上创建实际环境文件，例如：

```env
VITE_API_BASE_URL=
```

常见可选值：

- `VITE_API_BASE_URL=`：前端与后端同域部署，后端直接暴露根路径
- `VITE_API_BASE_URL=/api`：服务器把后端统一挂到 `/api`
- `VITE_API_BASE_URL=/api/dorm`：服务器显式保留了 `/api/dorm` 这一层代理前缀

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
