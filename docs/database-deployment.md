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

项目当前将数据库相关 SQL 统一放在：

- `release/package/low-carbon-dormitory-local-postgres-public.sql`
- `release/package/low-carbon-dormitory-sql-package.zip`
- `release/package/system-storage-r2-config.sql`
- `release/package/system-storage-r2-config-grant-lowcarbon.sql`
- `release/package/sql-import-readme.txt`

## 导入示例

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f release/package/low-carbon-dormitory-local-postgres-public.sql
```

## 2026-05-14 增量变更

为支持“水费不参与计费”开关，现有数据库需要给 `system_utility_rate_config` 增加 `enabled` 字段：

```sql
ALTER TABLE system_utility_rate_config
ADD COLUMN IF NOT EXISTS enabled boolean NOT NULL DEFAULT true;

UPDATE system_utility_rate_config
SET enabled = true
WHERE enabled IS NULL;
```

字段说明：

- `enabled = true`：该费用项继续参与计费
- `enabled = false`：该费用项停止计费

## 2026-05-18 R2 配置表

管理员后台保存 R2 配置时会使用表 `system_storage_r2_config`。如果目标数据库中还没有该表，请先执行：

```sql
CREATE TABLE IF NOT EXISTS system_storage_r2_config (
    config_key varchar(64) PRIMARY KEY,
    endpoint varchar(255) NOT NULL,
    access_key_id varchar(255) NOT NULL,
    secret_access_key varchar(255) NOT NULL,
    bucket varchar(255) NOT NULL,
    public_base_url varchar(255) NOT NULL,
    region varchar(50) NOT NULL,
    update_time timestamp without time zone NOT NULL DEFAULT now()
);
```

也可以直接执行现成文件：

```bash
psql -h 127.0.0.1 -p 5432 -U lowcarbon -d lowcarbon -f release/package/system-storage-r2-config.sql
```

当前程序固定使用的配置键为：

- `reward_image_r2`

## 2026-05-18 R2 表授权

如果 `system_storage_r2_config` 是用 `postgres` 或其他高权限账号建的，而后端运行账号是 `lowcarbon`，仅建表还不够，还需要继续授权，否则保存 R2 配置时会报“对表 system_storage_r2_config 权限不够”。

可直接执行：

```bash
psql -h 127.0.0.1 -p 5432 -U postgres -d lowcarbon -f release/package/system-storage-r2-config-grant-lowcarbon.sql
```

授权文件内容：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE system_storage_r2_config TO lowcarbon;
ALTER TABLE system_storage_r2_config OWNER TO lowcarbon;
```

## 排查顺序

如果管理员端保存 R2 配置失败，建议按顺序检查：

1. 后端实际连接的是否是 `lowcarbon` 库
2. `system_storage_r2_config` 表是否存在
3. 表字段是否与上面的建表 SQL 一致
4. 后端运行账号 `lowcarbon` 是否拥有该表的 `SELECT`、`INSERT`、`UPDATE`、`DELETE` 权限
5. 如果表由 `postgres` 创建，是否已经执行授权脚本

## 注意事项

- 当前 SQL 包默认面向已存在的 `lowcarbon` 数据库，不包含 `CREATE DATABASE`
- 执行增量 SQL 前建议先备份旧数据
- 如果数据库未执行 `system_utility_rate_config.enabled` 的增量 SQL，费率相关接口仍会因缺少字段而异常
