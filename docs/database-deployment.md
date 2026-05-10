# 数据库部署与导入说明

## 当前后端默认连接

后端默认连接的 PostgreSQL 信息如下：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`lowcarbon`
- 用户名：`lowcarbon`

对应配置文件：

- `low-carbon-dormitory-spring/src/main/resources/application.properties`

如果服务器端口或账号密码变更，可通过环境变量覆盖：

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/lowcarbon
SPRING_DATASOURCE_USERNAME=lowcarbon
SPRING_DATASOURCE_PASSWORD=000000
```

## SQL 导入包

当前发布包 `release/package/` 已包含本次可直接上传的 SQL 导入文件：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`
- `release/package/low-carbon-dormitory-sql-package.zip`
- `release/package/sql-import-readme.txt`

其中：

- `.sql` 为完整导入脚本
- `.zip` 为上传用压缩包，适合服务器面板或文件传输时直接上传
- `sql-import-readme.txt` 为导入命令说明

该 SQL 文件来源于本机 PostgreSQL：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`postgres`
- schema：`public`

本次导出的 SQL 已额外移除 PostgreSQL 18 导出中的 `\restrict`、`\unrestrict` 和 `SET transaction_timeout = 0;`，可兼容 PostgreSQL 12 导入。

兼容性验证结果：

- 已使用本机 PostgreSQL 12（`127.0.0.1:5433`）创建临时数据库完成实际导入验证
- 验证结果为 `public` 下 16 张业务表全部成功恢复

如需重新导出，可在数据库所在环境参考执行：

```bash
pg_dump --clean --if-exists --inserts --column-inserts --no-owner --no-privileges -h 127.0.0.1 -p 5432 -U postgres -d postgres > low-carbon-dormitory-local-postgres-public.sql
```

建议导出内容至少包含：

- `public` schema 下当前项目使用表的建表语句
- 主键、唯一约束、外键、索引
- 所有数据插入语句
- 序列当前值

## 导入建议

建议先在服务器数据库中备份原数据，再导入新的 SQL 文件。

常见导入方式：

```bash
psql -h 服务器IP -p 5432 -U 数据库用户 -d lowcarbon -f low-carbon-dormitory-local-postgres-public.sql
```

如果通过宝塔导入：

1. 先进入对应 PostgreSQL 数据库 `lowcarbon`
2. 选择 SQL 导入
3. 先上传并解压 `low-carbon-dormitory-sql-package.zip`，或直接上传 `low-carbon-dormitory-local-postgres-public.sql`
4. 执行导入

## 注意事项

- 当前 SQL 包默认面向已有 `lowcarbon` 数据库执行，不包含 `CREATE DATABASE`
- 导入脚本会先删除并重建当前项目使用的业务表
- SQL 文件不依赖本地 `postgres` 角色或 owner 信息，可直接导入到服务器现有库
- 如果服务器端 PostgreSQL 未开放远程访问，需要先在安全组、防火墙和 PostgreSQL 配置中放行
