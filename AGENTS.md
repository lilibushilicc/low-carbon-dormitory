# 项目级规则

- 始终使用简体中文回复。
- 本规则仅针对当前项目 `D:\study\system\low-carbon-dormitory` 生效。
- 根目录的 `docs/` 用于存放项目说明文档、设计说明、功能说明和使用文档。
- 根目录的 `log/` 用于存放项目日志、运行日志、构建日志和排查记录。
- 根目录的 `release/package/` 用于存放打包产物。
- 根目录的 `release/docker/` 用于存放 Docker 相关文件与镜像构建产物。
- 根目录的 `WillPlan/` 用于存放将来实施的计划、预研文件和后续规划资料。
- `low-carbon-dormitory-vue/` 和 `low-carbon-dormitory-spring/` 目录只保留项目源码及代码运行所需文件，不在这两个目录内新增日志文档、说明文档、规划文档和其他长期沉淀文件。
- 如需新增日志文件，统一放到根目录 `log/`，不要放在 `low-carbon-dormitory-vue/` 或 `low-carbon-dormitory-spring/` 内。
- 如需新增说明文档，统一放到根目录 `docs/`，不要放在 `low-carbon-dormitory-vue/` 或 `low-carbon-dormitory-spring/` 内。
- 如需新增将来实施的计划、待办方案、预研材料，统一放到根目录 `WillPlan/`，不要放在 `low-carbon-dormitory-vue/` 或 `low-carbon-dormitory-spring/` 内。
- 每次执行打包时，先清理根目录 `log/` 下与本次打包相关的运行日志、构建日志和临时排查日志，再生成新的打包日志。
- 每次执行打包时，只覆盖本次重新生成的产物；如果某类旧产物本次没有生成新的，不主动删除该旧产物。
- 打包后的最终产物统一整理到根目录 `release/package/` 或 `release/docker/`，不要将产物长期留在源码目录。
- `D:\study\system\low-carbon-dormitory\low-carbon-dormitory-spring\target\` 内不应保留 `.jar` 成品，`D:\study\system\low-carbon-dormitory\low-carbon-dormitory-vue\` 内不应保留 `dist/` 目录；如构建过程临时生成，整理产物后应清理。
- 每次更新项目时，同步更新根目录 `README.md`，确保项目结构、运行方式、规划目录和产物位置与当前代码一致。
- 每次更新项目时，检查 `docs/` 中现有说明文档是否仍适用于当前项目；如果内容过时、缺失或与当前实现不一致，必须同步更新。
