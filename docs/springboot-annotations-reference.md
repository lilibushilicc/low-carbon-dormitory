# Spring Boot 重要注解速查表

> 文档日期：2026-05-14  
> 适用项目：`D:\study\system\low-carbon-dormitory`  
> 使用场景：课程答辩、后端复习、源码阅读、面试速查

## 1. 文档说明

本文档结合当前项目 `low-carbon-dormitory-spring/` 的实际代码，对常见且重要的 Spring Boot 注解进行整理，包含三部分内容：

- 注解的作用是什么
- 在当前项目中的位置在哪里
- 在答辩时可以怎么解释

## 2. 启动类相关注解

### 2.1 `@SpringBootApplication`

**作用：**

- 标记这是一个 Spring Boot 启动类。
- 它是一个组合注解，通常包含：
  - `@Configuration`
  - `@EnableAutoConfiguration`
  - `@ComponentScan`

**项目位置：**

- [LowCarbonDormitoryApplication.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/LowCarbonDormitoryApplication.java)

**答辩表述：**

`@SpringBootApplication` 是整个后端项目的启动入口，它负责告诉 Spring Boot 从这里开始完成自动配置、组件扫描和应用启动。

### 2.2 `@MapperScan`

**作用：**

- 扫描指定包下的 MyBatis Mapper 接口。
- 避免每个 Mapper 都手动注入配置。

**项目位置：**

- [LowCarbonDormitoryApplication.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/LowCarbonDormitoryApplication.java)

**答辩表述：**

`@MapperScan` 负责自动扫描数据访问层接口，让 MyBatis-Plus 能识别各个 Mapper 并生成对应的数据访问能力。

## 3. 控制器层注解

控制器层主要位于：

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/`

### 3.1 `@RestController`

**作用：**

- 标记当前类是 REST 风格控制器。
- 返回值会自动转为 JSON，不需要再写 `@ResponseBody`。

**项目位置示例：**

- [StudentAuthenticationController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java)
- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)
- [PublicStudentDashboardController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/publicapi/PublicStudentDashboardController.java)

**答辩表述：**

`@RestController` 用来定义接口控制器，前端调用接口时，后端会以 JSON 形式返回数据。

### 3.2 `@RequestMapping`

**作用：**

- 定义类级别或方法级别的请求路径。
- 可以为一组接口统一加前缀。

**项目位置示例：**

- [StudentAuthenticationController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java)
- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)

**答辩表述：**

`@RequestMapping` 用来统一定义接口路径前缀，比如学生接口统一放在 `/student` 下，管理员接口统一放在 `/admin` 下。

### 3.3 `@GetMapping`

**作用：**

- 标记 GET 请求接口。
- 一般用于查询数据。

**项目位置示例：**

- [StudentProfileController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentProfileController.java)
- [StudentFeeHistoryController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentFeeHistoryController.java)

**答辩表述：**

`@GetMapping` 主要用于查询类接口，比如查询个人信息、查询账单、查询低碳看板。

### 3.4 `@PostMapping`

**作用：**

- 标记 POST 请求接口。
- 一般用于新增、提交或触发业务操作。

**项目位置示例：**

- [StudentAuthenticationController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java)
- [StudentPaymentController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentPaymentController.java)
- [StudentRewardController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentRewardController.java)

**答辩表述：**

`@PostMapping` 主要用于提交类操作，比如登录、注册、充值、兑换奖励。

### 3.5 `@PutMapping`

**作用：**

- 标记 PUT 请求接口。
- 一般用于更新已有数据。

**项目位置示例：**

- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)
- [LowCarbonRuleAdminController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/LowCarbonRuleAdminController.java)

**答辩表述：**

`@PutMapping` 主要用于更新操作，比如更新水电单价、更新奖励库存、更新低碳规则。

### 3.6 `@DeleteMapping`

**作用：**

- 标记 DELETE 请求接口。
- 一般用于删除数据。

**项目位置示例：**

- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)

**答辩表述：**

`@DeleteMapping` 用于删除类操作，比如管理员删除学生、删除奖励。

### 3.7 `@RequestBody`

**作用：**

- 将前端传来的 JSON 请求体转换成 Java 对象。

**项目位置示例：**

- [StudentAuthenticationController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java)
- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)

**答辩表述：**

`@RequestBody` 让后端能够直接接收前端传来的 JSON 数据，并自动封装成请求对象。

### 3.8 `@PathVariable`

**作用：**

- 获取 URL 路径中的动态参数。

**项目位置示例：**

- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)
- [StudentPaymentController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentPaymentController.java)

**答辩表述：**

`@PathVariable` 用来接收路径参数，例如宿舍 ID、奖励 ID、订单号。

## 4. 业务层注解

业务层主要位于：

- `low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/`

### 4.1 `@Service`

**作用：**

- 标记当前类是业务服务类。
- 交给 Spring 容器管理。

**项目位置示例：**

- [StudentPaymentService.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/StudentPaymentService.java)
- [RewardService.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/student/RewardService.java)
- [LowCarbonDashboardService.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardService.java)

**答辩表述：**

`@Service` 用来标记业务逻辑层，把核心计算、校验、聚合等逻辑集中到 Service 中，而不是直接写在 Controller 里。

## 5. 配置与组件注解

### 5.1 `@Configuration`

**作用：**

- 标记当前类是配置类。
- 通常用来注册拦截器、跨域配置、Bean 配置等。

**项目位置：**

- [WebCorsConfig.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/WebCorsConfig.java)

**答辩表述：**

`@Configuration` 用于定义 Spring 的配置类，比如本项目中就通过它完成跨域和拦截器注册。

### 5.2 `@Component`

**作用：**

- 标记普通 Spring 组件。
- 适合一些不明显属于 Controller、Service、Repository 的通用类。

**项目位置示例：**

- [AuthTokenInterceptor.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/config/AuthTokenInterceptor.java)
- [LowCarbonDashboardSupport.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/service/dashboard/LowCarbonDashboardSupport.java)

**答辩表述：**

`@Component` 用来标记可被 Spring 扫描和管理的通用组件，例如本项目中的 Token 拦截器和看板辅助组件。

## 6. 全局异常处理注解

### 6.1 `@RestControllerAdvice`

**作用：**

- 标记全局异常处理类。
- 所有控制器抛出的异常都可以统一在这里处理。

**项目位置：**

- [GlobalExceptionHandler.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/common/GlobalExceptionHandler.java)

**答辩表述：**

`@RestControllerAdvice` 用于统一处理接口异常，避免每个控制器都重复写 try-catch，提高后端代码规范性。

### 6.2 `@ExceptionHandler`

**作用：**

- 指定当前方法处理哪一类异常。

**项目位置：**

- [GlobalExceptionHandler.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/common/GlobalExceptionHandler.java)

**答辩表述：**

`@ExceptionHandler` 用来细分不同异常类型的处理方式，比如参数异常、权限异常、系统异常都可以分别返回不同提示。

## 7. 参数校验注解

### 7.1 `@Valid`

**作用：**

- 对请求对象进行参数校验。
- 通常和请求 DTO 上的校验规则配合使用。

**项目位置示例：**

- [StudentAuthenticationController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/student/StudentAuthenticationController.java)
- [AdminManagementController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/AdminManagementController.java)
- [LowCarbonRuleAdminController.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/controller/admin/LowCarbonRuleAdminController.java)

**答辩表述：**

`@Valid` 用来在进入业务逻辑前先校验请求参数是否合法，这样可以提前拦截错误输入，减少无效请求进入业务层。

## 8. MyBatis-Plus 相关注解

### 8.1 `@Mapper`

**作用：**

- 标记当前接口是 Mapper 数据访问接口。

**项目位置示例：**

- [StudentBaseMapper.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/mapper/StudentBaseMapper.java)
- [RewardItemMapper.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/mapper/RewardItemMapper.java)

**答辩表述：**

`@Mapper` 用来定义数据库访问接口，后端通过它完成对数据表的增删改查。

### 8.2 `@TableName`

**作用：**

- 指定实体类对应的数据表名称。

**项目位置示例：**

- [StudentBase.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/entity/StudentBase.java)
- [DormFee.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/entity/DormFee.java)

**答辩表述：**

`@TableName` 让实体类和数据库表建立映射关系，便于 ORM 框架操作数据库。

### 8.3 `@TableId`

**作用：**

- 指定实体类中的主键字段。
- 可以配置主键生成策略。

**项目位置示例：**

- [StudentBase.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/entity/StudentBase.java)
- [RewardItem.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/entity/RewardItem.java)

**答辩表述：**

`@TableId` 用来标记数据库主键字段，并指明主键生成方式，比如自增。

## 9. Lombok 常见注解

### 9.1 `@Data`

**作用：**

- 自动生成 getter、setter、toString、equals、hashCode 等方法。
- 简化实体类和 DTO 类代码。

**项目位置示例：**

- [StudentBase.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/entity/StudentBase.java)
- [Result.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/main/java/com/example/lowcarbondormitory/common/Result.java)
- `dto/request/` 与 `dto/response/` 下大量类

**答辩表述：**

`@Data` 是 Lombok 提供的注解，用于减少 Java Bean 中重复的样板代码，提高开发效率。

## 10. 测试相关注解

测试类主要位于：

- `low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/`

### 10.1 `@SpringBootTest`

**作用：**

- 启动完整 Spring Boot 测试环境。
- 适合做集成测试。

**项目位置示例：**

- [StudentRegisterControllerTest.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/StudentRegisterControllerTest.java)
- [LowCarbonDashboardServiceTest.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/LowCarbonDashboardServiceTest.java)

**答辩表述：**

`@SpringBootTest` 用于启动完整测试上下文，验证项目真实运行时的业务流程。

### 10.2 `@AutoConfigureMockMvc`

**作用：**

- 自动配置 MockMvc。
- 适合测试控制器接口而不真正启动服务器。

**项目位置示例：**

- [StudentPublicAccessByStuNumTest.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/StudentPublicAccessByStuNumTest.java)
- [AdminStudentManagementControllerTest.java](/D:/study/system/low-carbon-dormitory/low-carbon-dormitory-spring/src/test/java/com/example/lowcarbondormitory/AdminStudentManagementControllerTest.java)

**答辩表述：**

`@AutoConfigureMockMvc` 用来模拟 HTTP 请求测试接口，不需要真的部署服务器就能验证控制器逻辑。

## 11. 最常用注解总结

如果答辩时只讲最核心的一批注解，可以重点说这几个：

- `@SpringBootApplication`：启动整个 Spring Boot 项目。
- `@RestController`：定义后端接口控制器。
- `@RequestMapping` / `@GetMapping` / `@PostMapping`：定义接口路径和请求方式。
- `@Service`：定义业务逻辑层。
- `@Configuration`：定义配置类。
- `@Component`：定义通用组件。
- `@Valid`：做参数校验。
- `@RestControllerAdvice` + `@ExceptionHandler`：做全局异常处理。
- `@Mapper` / `@TableName` / `@TableId`：完成实体与数据库映射。
- `@SpringBootTest`：做集成测试。

## 12. 一段可直接背诵的答辩话术

在后端实现中，我主要使用了 Spring Boot 常见的分层注解。首先，通过 `@SpringBootApplication` 启动整个应用，通过 `@MapperScan` 扫描数据访问层。控制器层使用 `@RestController` 和各种 Mapping 注解定义接口，业务层使用 `@Service` 组织核心逻辑，配置层使用 `@Configuration` 和 `@Component` 管理拦截器与配置组件。为了提高系统健壮性，我使用 `@Valid` 做参数校验，使用 `@RestControllerAdvice` 和 `@ExceptionHandler` 做全局异常处理。在数据持久化层，我使用 `@Mapper`、`@TableName`、`@TableId` 完成 MyBatis-Plus 的实体映射。在测试中，通过 `@SpringBootTest` 和 `@AutoConfigureMockMvc` 对核心接口和业务流程进行验证。
