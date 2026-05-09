# 后端未使用代码高置信审计

本审计只覆盖 3 类目标：

- 未使用的私有方法
- DTO 中已不再被序列化返回的字段
- Mapper 中声明了但没调用的自定义查询方法

## 判定边界

- `Controller`、`@Service`、`@Component`、`@Configuration`、`@Mapper` 不按 import 次数判断是否存活。
- `BaseMapper<T>` 继承来的 CRUD 不算自定义方法。
- DTO 字段只做“service/controller 构建路径中是否命中对应 setter”的高置信审计。
- 不对 Lombok、Jackson、Spring、MyBatis 的运行期行为做推断式删除。

## 当前结论

- `Mapper` 自定义查询方法：`0` 项  
  当前 `mapper/` 包只包含 `BaseMapper<T>` 空扩展接口，没有额外声明方法。
- 未使用私有方法：`0` 项  
  按“方法名在本类中只出现 1 次”的保守规则扫描，当前未发现高置信候选。
- DTO 中已不再被序列化返回的字段：`0` 项  
  按“响应 DTO 字段对应 setter 在 service/controller 构建路径中出现 0 次”的保守规则扫描，当前未发现高置信候选。

## 已清理背景项

以下高置信死链已在此前清理：

- 荣誉链：`DormHonorService`、`DormHonorView`
- 周结算链：`DormWeeklyScoreSettlementService`、`DormWeeklyScoreSettlementMapper`、`DormWeeklyScoreSettlement`

## 复跑方式

在 `low-carbon-dormitory-spring` 目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\audit-backend-unused.ps1
.\mvnw.cmd test
```

接受标准：

- 审计脚本三项输出都为 `0 项`
- 后端测试继续全绿
