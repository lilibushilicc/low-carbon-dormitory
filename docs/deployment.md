# 服务器发布说明

## 1. 说明

本文档用于将 `low-carbon-dormitory` 发布到 Linux 服务器，包含：

- 前端静态资源发布
- Spring Boot 后端发布
- PostgreSQL 数据库初始化
- Nginx 反向代理
- 奖励图片上传到服务器目录的配置

当前项目的发布产物统一整理到：

- `release/package/frontend/dist/`
- `release/package/backend/`
- `release/package/nginx/`

运行时图片不打进前端或数据库二进制中，而是保存在服务器磁盘目录，数据库只保存图片 URL。

## 2. 推荐服务器目录

推荐使用如下目录结构：

```text
/srv/low-carbon-dormitory/
  frontend/dist
  backend/
  storage/reward-images/
  logs/
```

其中：

- `/srv/low-carbon-dormitory/frontend/dist`：前端静态资源
- `/srv/low-carbon-dormitory/backend`：后端 Jar 与环境变量文件
- `/srv/low-carbon-dormitory/storage/reward-images`：奖励图片上传目录
- `/srv/low-carbon-dormitory/logs`：后端运行日志

## 3. 数据库准备

先在服务器准备 PostgreSQL 数据库和账号，例如：

- 数据库：`lowcarbon`
- 用户名：`lowcarbon`
- 密码：按服务器实际设置

初始化命令示例：

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f low-carbon-dormitory-local-postgres-public.sql
```

SQL 文件位于：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`

## 4. 后端发布

### 4.1 上传后端文件

将以下文件上传到服务器目录 `/srv/low-carbon-dormitory/backend/`：

- `release/package/backend/low-carbon-dormitory-0.0.1-SNAPSHOT.jar`
- `release/package/backend/backend.env.example`
- `release/package/backend/start-backend.sh`

### 4.2 创建奖励图片目录

首次部署前，手动创建图片上传目录：

```bash
mkdir -p /srv/low-carbon-dormitory/storage/reward-images
mkdir -p /srv/low-carbon-dormitory/logs
```

### 4.3 配置后端环境变量

在服务器上复制环境变量模板：

```bash
cd /srv/low-carbon-dormitory/backend
cp backend.env.example backend.env
```

然后按实际环境修改 `backend.env`，至少包括：

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/lowcarbon
SPRING_DATASOURCE_USERNAME=lowcarbon
SPRING_DATASOURCE_PASSWORD=你的数据库密码
APP_AUTH_TOKEN_SECRET=替换成随机长密钥
APP_AUTH_TOKEN_TTL_SECONDS=86400
SERVER_PORT=3000
APP_UPLOAD_REWARD_DIR=/srv/low-carbon-dormitory/storage/reward-images
APP_UPLOAD_REWARD_URL_PREFIX=/uploads/rewards
```

这里最关键的是：

- `APP_UPLOAD_REWARD_DIR`
  作用：指定奖励图片实际保存到服务器哪个目录
- `APP_UPLOAD_REWARD_URL_PREFIX`
  作用：指定数据库里保存的图片 URL 前缀

上传后的图片会保存成类似：

```text
/srv/low-carbon-dormitory/storage/reward-images/202605/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.jpg
```

数据库里存储的值类似：

```text
/uploads/rewards/202605/xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.jpg
```

### 4.4 启动后端

启动命令：

```bash
cd /srv/low-carbon-dormitory/backend
chmod +x start-backend.sh
./start-backend.sh
```

如果要后台运行，建议：

```bash
nohup ./start-backend.sh > /srv/low-carbon-dormitory/logs/backend.out.log 2> /srv/low-carbon-dormitory/logs/backend.err.log &
```

启动成功后，后端默认监听：

- `http://127.0.0.1:3000`

## 5. 前端发布

将 `release/package/frontend/dist/` 整个目录上传到：

```text
/srv/low-carbon-dormitory/frontend/dist
```

前端生产环境默认配置为：

- 宿舍系统接口：同域直连后端根路径
- 餐厅模块接口：`/api/restaurant`

因此生产环境需要 Nginx 正确代理：

- `/student/**`
- `/admin/**`
- `/public/**`
- `/uploads/**`
- `/api/restaurant/**`

## 6. Nginx 配置

可参考发布包中的示例文件：

- `release/package/nginx/low-carbon-dormitory.conf.example`

关键点如下：

1. 前端路由回退到 `index.html`
2. 后端接口代理到 `127.0.0.1:3000`
3. 上传后的图片 URL `/uploads/**` 也代理到 `127.0.0.1:3000`
4. 餐厅接口 `/api/restaurant/**` 代理到 `http://39.98.69.153:8081/api/**`

## 7. 发布后重点验证

发布完成后，至少验证以下内容：

1. 登录接口是否正常返回 `200`
2. 学生端与管理端页面是否能正常打开
3. 奖励管理页能否创建奖励
4. 本地上传图片后，数据库 `image_url` 是否写入 `/uploads/rewards/...`
5. 浏览器能否直接访问上传后的图片 URL
6. 学生端奖励兑换页面是否能正常显示上传图片
7. 餐厅模块页面是否能正常请求 `/api/restaurant/**`

## 8. 常见问题

### 8.1 上传图片后页面显示不了

优先检查：

1. `APP_UPLOAD_REWARD_DIR` 是否配置到真实存在的服务器目录
2. 该目录是否有 Java 进程写权限
3. Nginx 是否把 `/uploads/**` 转发到了 Spring Boot
4. 数据库里的 `image_url` 是否是 `/uploads/rewards/...`

### 8.2 上传时报错

当前后端限制：

- 仅支持 `JPG / PNG / WEBP`
- 单张图片最大 `2MB`

### 8.3 部署后餐厅模块无数据

检查 Nginx 是否已配置：

```text
/api/restaurant/** -> http://39.98.69.153:8081/api/**
```
