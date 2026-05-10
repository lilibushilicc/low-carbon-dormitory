# low-carbon-dormitory

低碳宿舍项目工作区，包含前端、后端、说明文档、日志和发布产物目录。

## 目录说明

- `low-carbon-dormitory-vue/`：前端项目代码。
- `low-carbon-dormitory-spring/`：后端项目代码。
- `docs/`：项目说明文档、架构文档、功能文档。
- `log/`：运行日志、调试日志、构建日志。
- `release/package/`：打包产物目录。
- `release/docker/`：Docker 相关文件与构建产物目录。

## 维护规则

- 前后端子目录只保留代码及运行所需文件，不在子目录内新增日志文档和说明文档。
- 所有说明文档统一维护在 `docs/`。
- 所有日志统一维护在 `log/`。
- 每次打包前先清理 `release/package/` 和 `release/docker/` 中的旧内容。
- 每次项目更新后同步更新本文件，并检查 `docs/` 下文档是否仍与当前项目一致。
