# 公开学生注册接口说明

## 目的

为外部系统提供一个无需进入管理端页面的学生注册入口，通过传参直接完成学生建档。

说明：

- 本次仅新增后端接口
- 前端登录页没有新增“注册学生”入口
- 管理员端原有“新增学生”页面继续保留

## 接口定义

- 方法：`POST`
- 路径：`/student/register`

## 请求体

```json
{
  "stuNum": "20260001",
  "name": "张三",
  "password": "123456",
  "dormBuilding": "1号楼",
  "dormRoom": "101",
  "bedTotal": 4,
  "gender": 1,
  "idCard": "110101200001010011",
  "phone": "13800000000",
  "college": "计算机学院",
  "major": "软件工程",
  "className": "软工1班",
  "grade": "2026级"
}
```

必填字段：

- `stuNum`
- `name`
- `password`
- `dormBuilding`
- `dormRoom`

可选字段：

- `bedTotal`
- `gender`
- `idCard`
- `phone`
- `college`
- `major`
- `className`
- `grade`

限制：

- 公开注册不允许外部直接传入 `carbonScore`
- 新注册学生默认初始碳积分为 `0`

## 返回体

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "studentId": 123,
    "stuNum": "20260001",
    "dormId": 45
  }
}
```

## 处理逻辑

接口内部会执行：

1. 学号去重
2. 查找或创建宿舍
3. 创建学生基础档案
4. 初始化宿舍费用账户
5. 同步宿舍可用床位

## 调用建议

外部系统推荐按以下顺序调用：

1. `POST /student/register`
2. `POST /student/login`

不要把登录接口当成注册入口使用，因为 `POST /student/login` 仍然只负责身份校验。
