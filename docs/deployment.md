# 服务器部署说明

## 1. 说明

本文档用于将 `low-carbon-dormitory` 发布到 Linux 服务器，包含：

- 前端静态资源发布
- Spring Boot 后端发布
- PostgreSQL 数据库初始化
- Nginx 反向代理
- 奖励图片上传到 Cloudflare R2 的配置

当前项目的发布产物统一整理到：

- `release/package/frontend/dist/`
- `release/package/backend/`
- `release/package/nginx/`

奖励图片不再保存在服务器本地磁盘，而是上传到 Cloudflare R2；数据库只保存图片 URL。

## 2. 推荐服务器目录

```text
/srv/low-carbon-dormitory/
  frontend/dist
  backend/
  logs/
```

其中：

- `/srv/low-carbon-dormitory/frontend/dist`：前端静态资源
- `/srv/low-carbon-dormitory/backend`：后端 Jar 与环境变量文件
- `/srv/low-carbon-dormitory/logs`：后端运行日志

## 3. 数据库准备

示例：

- 数据库：`lowcarbon`
- 用户名：`lowcarbon`
- 密码：按服务器实际设置

初始化命令：

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f low-carbon-dormitory-local-postgres-public.sql
```

SQL 文件位于：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`

## 4. 后端发布

### 4.1 上传后端文件

上传到 `/srv/low-carbon-dormitory/backend/`：

- `release/package/backend/low-carbon-dormitory-0.0.1-SNAPSHOT.jar`
- `release/package/backend/backend.env.example`
- `release/package/backend/start-backend.sh`

### 4.2 创建日志目录

```bash
mkdir -p /srv/low-carbon-dormitory/logs
```

### 4.3 配置后端环境变量

```bash
cd /srv/low-carbon-dormitory/backend
cp backend.env.example backend.env
```

至少配置：

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/lowcarbon
SPRING_DATASOURCE_USERNAME=lowcarbon
SPRING_DATASOURCE_PASSWORD=你的数据库密码
APP_AUTH_TOKEN_SECRET=替换成随机长密钥
APP_AUTH_TOKEN_TTL_SECONDS=86400
SERVER_PORT=3000
APP_UPLOAD_R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
APP_UPLOAD_R2_ACCESS_KEY_ID=你的R2AccessKeyId
APP_UPLOAD_R2_SECRET_ACCESS_KEY=你的R2SecretAccessKey
APP_UPLOAD_R2_BUCKET=low-carbon-dormitory
APP_UPLOAD_R2_PUBLIC_BASE_URL=https://pub-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.r2.dev
APP_UPLOAD_R2_REGION=auto
```

关键说明：

- `APP_UPLOAD_R2_ENDPOINT`：R2 的 S3 兼容接口地址
- `APP_UPLOAD_R2_ACCESS_KEY_ID` / `APP_UPLOAD_R2_SECRET_ACCESS_KEY`：R2 API 凭证
- `APP_UPLOAD_R2_BUCKET`：奖励图片上传的 Bucket
- `APP_UPLOAD_R2_PUBLIC_BASE_URL`：数据库中写入的图片访问前缀，建议使用绑定到 R2 的自定义域名
- `APP_UPLOAD_R2_REGION`：R2 推荐使用 `auto`

上传后的对象 Key 类似：

```text
202605/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.jpg
```

数据库中的 `image_url` 类似：

```text
https://pub-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.r2.dev/202605/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.jpg
```

### 4.4 启动后端

```bash
cd /srv/low-carbon-dormitory/backend
chmod +x start-backend.sh
./start-backend.sh
```

后台运行示例：

```bash
nohup ./start-backend.sh > /srv/low-carbon-dormitory/logs/backend.out.log 2> /srv/low-carbon-dormitory/logs/backend.err.log &
```

## 5. 前端发布

将 `release/package/frontend/dist/` 上传到：

```text
/srv/low-carbon-dormitory/frontend/dist
```

生产环境下前端默认通过同域访问：

- `/student/**`
- `/admin/**`
- `/public/**`
- `/api/restaurant/**`

奖励图片不再走本机 `/uploads/**` 代理，而是直接访问 R2 公网地址。

## 6. Nginx 配置

可参考：

- `release/package/nginx/low-carbon-dormitory.conf.example`

关键点：

1. 前端路由回退到 `index.html`
2. 后端接口代理到 `127.0.0.1:3000`
3. 餐厅接口 `/api/restaurant/**` 代理到 `http://39.98.69.153:8081/api/**`
4. 不再需要 `/uploads/**` 代理

## 7. 发布后重点验证

1. 登录接口是否正常返回 `200`
2. 学生端与管理端页面是否能正常打开
3. 管理端奖励管理页能否成功上传图片并创建奖励
4. 数据库 `image_url` 是否写入 R2 公网地址
5. 浏览器能否直接访问该图片 URL
6. 学生端奖励兑换页面是否能正常显示图片
7. 餐厅模块页面是否能正常请求 `/api/restaurant/**`

## 8. 常见问题

### 8.1 上传成功但页面图片不显示

优先检查：

1. `APP_UPLOAD_R2_PUBLIC_BASE_URL` 是否正确
2. R2 Bucket 或绑定域名是否允许公网读取
3. 数据库中的 `image_url` 是否为完整可访问地址
4. 浏览器直接打开该 URL 是否返回 `200`

### 8.2 上传时报错

当前后端限制：

- 仅支持 `JPG / PNG / WEBP`
- 单张图片最大 `2MB`

同时检查：

1. `APP_UPLOAD_R2_ENDPOINT` 是否正确
2. `APP_UPLOAD_R2_ACCESS_KEY_ID` / `APP_UPLOAD_R2_SECRET_ACCESS_KEY` 是否有效
3. `APP_UPLOAD_R2_BUCKET` 是否存在且当前凭证有写权限

### 8.3 部署后餐厅模块无数据

检查 Nginx 是否已配置：

```text
/api/restaurant/** -> http://39.98.69.153:8081/api/**
```
