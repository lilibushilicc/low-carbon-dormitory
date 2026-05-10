低碳宿舍 SQL 导入包

1. 导入目标：PostgreSQL 12+
2. 推荐导入命令：
   psql -h <host> -p 5432 -U <user> -d lowcarbon -f low-carbon-dormitory-local-postgres-public.sql
3. 本 SQL 包会先 DROP 再重建 public 下当前业务表。
4. 导入前请先备份服务器数据库。