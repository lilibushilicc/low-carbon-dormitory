# 数据库部署与导入说明

## 默认本地连接

后端默认连接 PostgreSQL，配置文件位于：

- `low-carbon-dormitory-spring/src/main/resources/application.properties`

默认参数：

- 主机：`127.0.0.1`
- 端口：`5432`
- 数据库：`lowcarbon`
- 用户名：`lowcarbon`
- 密码：`000000`

## SQL 产物位置

项目当前将数据库发布相关文件统一放在：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`
- `release/package/low-carbon-dormitory-sql-package.zip`
- `release/package/sql-import-readme.txt`

## 导入示例

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f release/package/low-carbon-dormitory-local-postgres-public.sql
```

## 2026-05-14 增量变更

为支持“水费不计入计算 / 不参与计费”开关，现有数据库需要给 `system_utility_rate_config` 增加 `enabled` 字段。

```sql
ALTER TABLE system_utility_rate_config
ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;

UPDATE system_utility_rate_config
SET enabled = true
WHERE enabled IS NULL;
```

字段说明：

- `enabled = true`：该费用项继续参与计费。
- `enabled = false`：该费用项停止计费。

当前主要用于 `fee_type = 'WATER'` 的水费开关，但字段设计也兼容电费。

## 注意事项

- 当前 SQL 包默认面向已存在的 `lowcarbon` 数据库，不包含 `CREATE DATABASE`。
- 导入或手工执行增量 SQL 前，建议先备份旧数据。
- 本次代码升级后，如果数据库未补 `enabled` 字段，费率相关接口会因缺少列而无法正常工作。
