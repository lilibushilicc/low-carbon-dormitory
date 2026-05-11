# 数据库部署与导入说明

## 默认本地数据库连接

后端当前默认连接本机 PostgreSQL，配置位于：

- `low-carbon-dormitory-spring/src/main/resources/application.properties`

默认参数如下：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`lowcarbon`
- 用户名：`lowcarbon`
- 密码：`000000`

对应配置项：

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/lowcarbon
spring.datasource.username=lowcarbon
spring.datasource.password=000000
```

如需在本地或服务器临时覆盖，可使用环境变量：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/lowcarbon
SPRING_DATASOURCE_USERNAME=lowcarbon
SPRING_DATASOURCE_PASSWORD=000000
```

## 本地联调建议顺序

1. 先确认 PostgreSQL 已启动，且存在 `lowcarbon` 数据库与对应账号。
2. 在 `low-carbon-dormitory-spring/` 中启动后端服务。
3. 访问后端登录接口或启动日志，确认后端已成功连接数据库。
4. 在 `low-carbon-dormitory-vue/` 中启动前端，通过 Vite 代理完成联调。

## SQL 发布包

当前 SQL 发布包统一整理在 `release/package/`：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`
- `release/package/low-carbon-dormitory-sql-package.zip`
- `release/package/sql-import-readme.txt`

说明：

- `.sql` 文件为完整导入脚本
- `.zip` 文件适合上传到服务器后再解压导入
- `sql-import-readme.txt` 为文本版导入说明

## 导入方式

命令行导入示例：

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f release/package/low-carbon-dormitory-local-postgres-public.sql
```

如果通过可视化面板导入：

1. 选择目标数据库 `lowcarbon`
2. 上传并执行 `low-carbon-dormitory-local-postgres-public.sql`
3. 或先上传并解压 `low-carbon-dormitory-sql-package.zip` 后再执行其中 SQL

## 注意事项

- 当前 SQL 包默认面向已存在的 `lowcarbon` 数据库，不包含 `CREATE DATABASE`
- 导入脚本会重建当前项目使用的业务表，导入前应先备份旧数据
- 只覆盖本次重新生成的 SQL 产物；若本次未重新生成其他发布物，不主动删除旧产物
- 最终发布 SQL 与相关说明长期保留在 `release/package/`，不放回源码目录
