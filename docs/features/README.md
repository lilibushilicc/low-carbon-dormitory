# 功能文档索引

> 2026-05-12 更新：管理员后台已新增“餐厅管理”模块入口；宿舍管理与餐厅管理共享后台壳，但会按当前路由切换各自独立的左侧导航。
> 2026-05-12 更新：原管理员左侧导航栏中的“餐厅系统”外部跳转入口已升级为站内模块切换入口，进入餐厅后显示餐厅系统自己的菜单。

本文档目录按功能拆分说明当前仓库的代码实现，重点覆盖：

- 前端入口页面与路由
- 前端 API 调用位置
- 后端 Controller / Service 调用链路
- 核心数据对象
- 已知限制与维护注意事项

## 文档列表

- [登录与注册](./auth-and-login.md)
- [公开学生注册](./student-register.md)
- [公开查询接口与权限边界](./public-query-and-permission-boundaries.md)
- [个人信息查询](./student-profile.md)
- [宿舍水电查询与刷新](./water-electricity.md)
- [缴费、支付订单与账单历史](./payment-and-history.md)
- [低碳看板](./low-carbon-dashboard.md)
- [低碳规则查看与配置](./low-carbon-rules.md)
- [奖励兑换](./rewards.md)
- [管理员学生创建](./admin-student-create.md)
- [管理员学生管理与删除](./admin-student-management.md)
- [管理员奖励管理与水电单价](./admin-reward-and-rate-management.md)
- [管理员宿舍扣费](./admin-dorm-fee-deduction.md)

## 建议阅读顺序

1. 登录与注册
2. 公开学生注册
3. 宿舍水电查询与刷新
4. 缴费、支付订单与账单历史
5. 低碳看板
6. 管理端相关文档
