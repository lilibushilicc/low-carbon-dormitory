# 功能实现文档索引

本文档目录按功能拆分说明当前仓库的代码实现，重点覆盖：

- 前端入口页面与路由
- 前端 API 调用位置
- 后端 Controller / Service / Mapper 调用链
- 核心数据对象
- 已知限制与维护注意点

## 文档列表

- [登录与统一登录接入](./auth-and-login.md)
- [公开查询接口与权限边界](./public-query-and-permission-boundaries.md)
- [个人信息查询](./student-profile.md)
- [宿舍水电查询与刷新](./water-electricity.md)
- [缴费、支付订单与账单历史](./payment-and-history.md)
- [低碳看板](./low-carbon-dashboard.md)
- [低碳规则查看与配置](./low-carbon-rules.md)
- [奖励兑换](./rewards.md)
- [管理员学生创建](./admin-student-create.md)
- [管理员奖励管理与水电单价](./admin-reward-and-rate-management.md)
- [管理员宿舍扣费](./admin-dorm-fee-deduction.md)

## 阅读建议

如果你是第一次接手这个项目，建议按下面顺序阅读：

1. 登录与统一登录接入
2. 宿舍水电查询与刷新
3. 缴费、支付订单与账单历史
4. 低碳看板
5. 奖励兑换
6. 管理端相关文档

这样能先建立“学生主流程”，再补齐后台维护逻辑。
