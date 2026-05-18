# 管理员奖励与 R2 存储管理说明

## 功能目标

该页面是管理端的奖励运营维护页面，负责两类配置：

- 奖励项目维护
- 奖励图片上传使用的 R2 存储配置维护

水电费率已经归入“低碳规则配置”页维护，因为该配置直接参与碳排换算、规则预览和看板计算。

## 前端入口

- 路由：`/manager/reward-manage`
- 页面：`low-carbon-dormitory-vue/src/router/manager-router/reward/reward-manage.vue`

页面包含三块能力：

- 读取、测试和保存 R2 配置
- 新增奖励
- 修改奖励库存或删除奖励

## 前端 API

位于 `low-carbon-dormitory-vue/src/api/modules/admin.ts`：

- `fetchRewardsByAdmin()`
- `createRewardByAdmin(...)`
- `updateRewardStockByAdmin(...)`
- `deleteRewardByAdmin(...)`
- `fetchR2StorageConfigByAdmin()`
- `updateR2StorageConfigByAdmin(...)`
- `testR2StorageConfigByAdmin(...)`

## 后端入口

- Controller：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- R2 Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminR2StorageConfigService.java`
- 奖励 Service：`low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`

相关接口：

- `GET /admin/storage/r2`
- `PUT /admin/storage/r2`
- `POST /admin/storage/r2/test`
- `GET /admin/rewards`
- `POST /admin/rewards/upload-image`
- `POST /admin/rewards`
- `PUT /admin/rewards/{rewardId}/stock`
- `DELETE /admin/rewards/{rewardId}`

## R2 配置管理

管理员页面可直接读取并维护奖励图片上传使用的 R2 配置，包括：

- `endpoint`
- `accessKeyId`
- `secretAccessKey`
- `bucket`
- `publicBaseUrl`
- `region`

页面当前交互：

1. 页面加载时读取当前已保存配置并缓存为“最近一次加载或保存的配置”。
2. 管理员可先点击“测试连接”验证当前输入参数。
3. 点击“保存配置”前会弹出确认框，展示本次将写入的关键参数。
4. 如果当前输入有误，可点击“恢复已保存配置”回退到最近一次加载或保存的值。

后端行为：

1. 上传服务优先读取数据库表 `system_storage_r2_config` 中的最新配置。
2. 如果后台尚未保存配置，则回退到环境变量。
3. 管理员可在保存前通过 `POST /admin/storage/r2/test` 执行连接测试。

## 奖励图片上传实现

管理员新增奖励页支持先上传本地图片，再把返回的图片地址写入 `imageUrl`：

1. 前端选择本地图片文件。
2. 通过 `POST /admin/rewards/upload-image` 以 `multipart/form-data` 提交。
3. 后端校验文件类型与大小，仅允许 `JPG/PNG/WEBP`，单张不超过 `2MB`。
4. 后端把文件上传到 Cloudflare R2，对象 Key 形如 `YYYYMM/xxxxxx.png`。
5. 后端返回可直接访问的公网地址，例如 `https://<your-r2-public-base-url>/202605/xxxxxx.png`。
6. 前端再调用 `POST /admin/rewards` 创建奖励，数据库仅保存 `image_url`。

当前运行配置：

- `app.upload.r2.endpoint=${APP_UPLOAD_R2_ENDPOINT}`
- `app.upload.r2.access-key-id=${APP_UPLOAD_R2_ACCESS_KEY_ID}`
- `app.upload.r2.secret-access-key=${APP_UPLOAD_R2_SECRET_ACCESS_KEY}`
- `app.upload.r2.bucket=${APP_UPLOAD_R2_BUCKET}`
- `app.upload.r2.public-base-url=${APP_UPLOAD_R2_PUBLIC_BASE_URL}`
- `app.upload.r2.region=${APP_UPLOAD_R2_REGION:auto}`

## 奖励管理实现

### 奖励列表

`AdminManagementService.listRewards()`：

- 查询全部奖励
- 按 `sortOrder`、`rewardId` 排序

### 新增奖励

`AdminManagementService.createReward(...)`：

1. 组装 `RewardItem`
2. 设置默认排序、状态、创建时间、更新时间
3. 插入奖励表

### 更新库存

`AdminManagementService.updateRewardStock(...)`：

1. 按 ID 查询奖励
2. 校验奖励存在
3. 更新库存
4. 更新时间
5. 返回最新记录

### 删除奖励

`AdminManagementService.deleteReward(...)`：

1. 校验 `rewardId`
2. 校验奖励存在
3. 直接物理删除数据库记录

注意：当前仅删除奖励记录，不会联动删除 R2 中已上传的历史图片对象。

## 与学生端奖励中心的关系

学生端奖励中心读取的是：

- `status = 1` 的奖励

因此管理端维护的这些字段会直接影响学生端：

- 是否可见
- 所需积分
- 库存数量
- 展示顺序

## 相关文件

- `low-carbon-dormitory-vue/src/router/manager-router/reward/reward-manage.vue`
- `low-carbon-dormitory-vue/src/api/modules/admin.ts`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminManagementService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminRewardImageStorageService.java`
- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/admin/AdminR2StorageConfigService.java`

## 维护注意点

- 当前上传完成后数据库保存的是完整公网 URL，不再依赖本地 `/uploads/**` 静态映射。
- 如果后续需要支持删除奖励时同步删除 R2 对象，建议额外存储 `objectKey`。
- 如果后续图片访问域名变更，历史数据里保存的是完整 URL，需评估是否需要迁移。
