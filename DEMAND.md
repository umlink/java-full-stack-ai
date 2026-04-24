# 全栈商城系统 + 秒杀功能 — 完整需求与技术方案

## 用户决策记录

| 决策项 | 选择结果 |
|--------|----------|
| UI 组件库 | shadcn/ui |
| 表单方案 | React Hook Form + Zod |
| 包管理 | yarn |
| 构建工具 | Vite 6 |
| 后端 ORM | MyBatis-Plus |
| 图片存储 | 七牛云对象存储 |
| 消息队列 | RocketMQ 5 |
| 秒杀时段 | 任意时间段 |
| 支付超时 | 15 分钟 |
| 商品搜索 | MySQL FULLTEXT + ngram parser |
| 购物车 | 未登录不可加购，登录后才能使用 |
| 角色体系 | 普通用户 + 运营 + 管理员 |
| 订单取消 | 下单后 30 分钟内可取消（已支付订单进入退款流程） |
| 数据看板 | 销售总额 + 秒杀数据 + 商品排行 + 订单趋势 |
| 支付方式 | 模拟支付（含退款） |
| 部署 | Docker Compose |
| 数据库迁移 | SQL 文件版本化管理（Flyway） |
| 环境配置 | 多环境（dev / prod） |
| 压测 | JMeter 脚本 |

---

## 一、技术栈详解

### 1.1 前端技术栈

```
框架：        Vite 6 + React 18 + TypeScript 5
样式：        TailwindCSS 4
UI 组件：     shadcn/ui（基于 Radix UI + TailwindCSS）
状态管理：    Zustand（auth、cart 等客户端状态）
服务端请求：  Axios 封装 + 自定义 useRequest hook（统一 loading/error/data）
路由：        React Router v7
表单：        React Hook Form + Zod 校验
HTTP 客户端： Axios（带拦截器）
包管理：      yarn
构建工具：    Vite 6（内置 Rollup + esbuild）
开发工具：    ESLint + Prettier
```

> **UI 实现说明**：前端 UI 实现时使用 `frontend-design` skill 辅助生成高质量组件代码。

### 1.2 后端技术栈

```
框架：         Spring Boot 3.2 + JDK 21
ORM：          MyBatis-Plus 3.5
数据库：       MySQL 8
缓存：         Redis 7 + Redisson（分布式锁）
消息队列：     RocketMQ 5（秒杀异步削峰 + 延迟消息处理超时订单）
安全：         Spring Security 6 + JWT (jjwt 0.12)
校验：         Hibernate Validator (jakarta.validation)
构建：         Maven
API 文档：     SpringDoc OpenAPI 2 (Swagger UI)
定时任务：     Spring @Scheduled + Redisson 分布式锁
日志：         SLF4J + Logback
测试：         JUnit 5 + Mockito
数据库迁移：   Flyway（SQL 文件版本化管理）
对象存储：     七牛云 Kodo（图片上传与访问）
XSS 防护：     OWASP Java HTML Sanitizer（富文本过滤）
```

### 1.3 基础设施（Docker Compose）

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: mall-mysql
    ports: ["3306:3306"]
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: mall
    volumes: [mysql-data:/var/lib/mysql]
    mem_limit: 1g
    cpus: "1.0"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: mall-redis
    ports: ["6379:6379"]
    volumes: [redis-data:/data]
    mem_limit: 512m
    command: redis-server --appendonly yes

  rocketmq-namesrv:
    image: apache/rocketmq:5.1.4
    container_name: mall-rmq-namesrv
    ports: ["9876:9876"]
    environment:
      JAVA_OPT_EXT: "-Xms256m -Xmx256m"
    command: sh mqnamesrv

  rocketmq-broker:
    image: apache/rocketmq:5.1.4
    container_name: mall-rmq-broker
    ports: ["10911:10911"]
    environment:
      JAVA_OPT_EXT: "-Xms512m -Xmx512m"
      NAMESRV_ADDR: "rocketmq-namesrv:9876"
    command: sh mqbroker -n rocketmq-namesrv:9876

  backend:
    build: ./backend
    container_name: mall-backend
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      MYSQL_HOST: mysql
      REDIS_HOST: redis
      ROCKETMQ_NAMESRV: rocketmq-namesrv:9876
      JWT_SECRET: ${JWT_SECRET}
      QINIU_ACCESS_KEY: ${QINIU_ACCESS_KEY}
      QINIU_SECRET_KEY: ${QINIU_SECRET_KEY}
    mem_limit: 512m
    depends_on:
      mysql: { condition: service_healthy }
      redis: { condition: service_started }
      rocketmq-namesrv: { condition: service_started }

  frontend:
    build: ./frontend
    container_name: mall-frontend
    ports: ["80:80"]
    mem_limit: 128m

volumes:
  mysql-data:
  redis-data:
```

### 1.4 多环境配置

```
backend/src/main/resources/
├── application.yml              # 公共配置（MyBatis-Plus、HikariCP、日志等）
├── application-dev.yml          # 开发环境（debug 日志、宽松限流、Flyway 自动迁移）
└── application-prod.yml         # 生产环境（info 日志、严格限流、Flyway 手动迁移）
```

| 配置项 | dev | prod |
|--------|-----|------|
| 数据库连接池 `maximumPoolSize` | 10 | 20 |
| 日志级别 | DEBUG | INFO |
| Flyway 策略 | `migrate-on-start: true` | `migrate-on-start: false`（手动执行） |
| 限流策略 | 宽松（100 req/s） | 严格（根据压测调整） |
| 七牛云 Bucket | `mall-dev` | `mall-prod` |
| 模拟支付 | 自动支付 | 自动支付（可切换真实支付） |

### 1.5 数据库连接池配置（HikariCP）

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20      # 最大连接数（prod），dev 为 10
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时（30s）
      idle-timeout: 600000       # 空闲回收（10min）
      max-lifetime: 1800000      # 连接最大存活（30min）
      validation-timeout: 5000   # 连接校验超时
```

### 1.6 优雅关闭

```yaml
# application.yml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Docker Compose 中 backend 的 `stop_grace_period: 35s`（比 Spring 超时多 5s）。

### 1.7 七牛云对象存储集成

**方案：前端直传**
1. 前端调用 `GET /api/upload/token?type=product` 获取七牛云 uploadToken
2. 后端签发 Token（含上传策略：允许文件类型/大小、指定 bucket、设置回调 URL）
3. 前端使用 Token 直传七牛云，减少后端带宽压力
4. 上传成功后前端将返回的 key 提交给业务接口（如商品创建）

```
用户 → 前端 → GET /api/upload/token(后端签发) → 前端 → 直传七牛云 → 返回 key → 业务接口
```

**安全策略**：
- Token 有效期 10 分钟
- 限制上传 MIME 类型：`image/jpeg, image/png, image/webp`
- 限制文件大小：商品图 5MB，头像 2MB
- 服务端魔数校验（业务接口收到 key 后，校验七牛云回调中的文件信息）

### 1.8 SQL 文件版本化管理（Flyway）

**目录结构**：
```
backend/src/main/resources/db/migration/
├── V1__init.sql                # 初始全部建表（user、product、orders 等）
├── V2__add_address_table.sql   # 收货地址表
├── V3__add_refund_fields.sql   # 退款字段
└── V4__add_mq_dedup_table.sql  # 消息去重表
```

**规则**：
- 文件命名：`V{version}__{description}.sql`，version 递增且不可重复
- 开发环境（dev）：`spring.flyway.migrate-on-start=true`，启动时自动执行
- 生产环境（prod）：`spring.flyway.migrate-on-start=false`，手动执行迁移
- 所有 DDL 通过 Flyway 管理，禁止手动修改数据库结构

---

## 二、功能模块与业务场景

### 2.1 用户模块

#### 2.1.1 注册
- **正常流程**：填写用户名/密码/邮箱 → 后端校验唯一性 → 密码 BCrypt 加密 → 入库 → 返回成功
- **异常场景**：
  - 用户名已存在 → 400 `USERNAME_EXISTS`，前端提示"该用户名已被注册"
  - 邮箱格式错误 → 422 字段级校验错误，前端 Zod 同步提示
  - 密码强度不足（<8 位 或 不含字母+数字）→ 同上
  - 用户名包含特殊字符 → 同上
  - 服务器内部错误 → 500，前端统一弹 "系统繁忙，请稍后重试"

#### 2.1.2 登录
- **正常流程**：用户名+密码 → 校验 → 生成 JWT → 返回 token + 用户基本信息
- **异常场景**：
  - 用户名不存在 → 401 `USER_NOT_FOUND`
  - 密码错误 → 401 `PASSWORD_ERROR`
  - 账号被禁用 → 403 `ACCOUNT_DISABLED`
  - 连续 5 次密码错误 → 验证码锁定 15 分钟（简易版：提示"请 15 分钟后再试"）

#### 2.1.3 JWT 机制
- Access Token：有效期 2 小时
- Token 携带用户 ID + 角色
- 前端 Axios 拦截器自动附加 `Authorization: Bearer <token>`
- 401 响应时自动跳转到登录页
- **续期策略**：Token 过期前 10 分钟，后台通过 Refresh Token 静默续期（可选实现）

### 2.2 商品模块

#### 2.2.1 商品列表页
- **功能**：分页展示商品、按分类筛选、按品牌筛选、按价格/销量/新品排序、关键词搜索
- **搜索**：MySQL FULLTEXT INDEX（ngram parser），对 `name` + `keywords` 字段做全文搜索，使用参数化查询防止 SQL 注入
- **列表卡片展示**：主图 + 商品名称 + 品牌 + 简介 + 价格 + 已售数
- **多规格商品**：列表页显示价格区间 "¥99.00 - ¥199.00"
- **分页**：基于 `page` + `pageSize` 偏移量分页，默认每页 20 条

**前端状态覆盖**：
| 状态 | 表现 |
|------|------|
| 加载中 | Skeleton 骨架屏，每行 4 个商品卡片骨架 |
| 空数据 | Empty 组件 + "暂无商品，去看看别的分类" + 推荐分类链接 |
| 搜索无结果 | Empty 组件 + "没有找到相关商品，试试其他关键词" |
| 网络错误 | 错误提示 + "重新加载"按钮，点击重新请求 |
| 分页加载更多 | "加载更多"按钮或滚动加载 |

#### 2.2.2 商品详情页
- **功能**：主图/多图切换 + 商品视频 + 品牌标识 + 标签(新品/热销) + 价格 + 已售 + 参数属性展示
- **SKU 选择器**：多规格商品展示规格选择器（颜色/存储容量等），选中后更新价格/库存/图片
- **加购按钮**：选择 SKU 后才能加购（多规格）/ 直接加购（单规格）
- **参数属性**：结构化属性参数表格展示（屏幕尺寸、电池容量等）

**异常场景**：
| 场景 | 前端表现 |
|------|----------|
| 商品不存在/已下架 | 404 页面 + "商品已下架" + 返回列表链接 |
| SKU 全部库存为 0 | "暂时缺货"灰色按钮，不可加购 |
| SKU 选中后库存不足 | 加购时提示"该规格库存不足" |
| 商品数据加载中 | 全页 Skeleton |

### 2.3 购物车模块

#### 2.3.1 规则
- **前提**：必须登录才能加购
- **未登录点击加购**：弹出引导弹窗"登录后即可使用购物车"，提供"去登录"和"稍后再说"两个选项（而非直接跳转登录页）
- **操作**：添加商品（选 SKU）、修改数量、删除商品、全选/取消全选
- **限购**：单种 SKU 最多 99 件（多规格下不同 SKU 各自计数）
- **库存校验**：加购时校验对应 SKU 库存 > 0
- **数据存储**：服务端 MySQL `cart_item` 表，记录 `product_id` + `sku_id`
- **SKU 展示**：购物车列表展示商品名 + SKU 规格名（如 "银色 128G"）

**前端状态覆盖**：
| 状态 | 表现 |
|------|------|
| 加载中 | 购物车列表 Skeleton |
| 空购物车 | Empty + "购物车是空的" + "去逛逛"按钮跳转商品列表 |
| 商品已下架 | 商品行标灰 + "该商品已下架" 文字，数量选择器禁用 |
| 商品库存不足 | 显示"仅剩 X 件"，数量自动调整为库存数 |
| 网络错误 | Toast 提示 + 操作失败回滚 |
| 数量操作 | 异步请求，loading 态禁用按钮，失败后恢复原数量 |

### 2.4 订单模块

#### 2.4.1 创建订单（普通商品）
1. 从购物车选择商品 → 点击结算
2. 进入确认页：展示商品清单 + 总金额
3. 选择收货地址（若无地址则提示添加）
4. 点击"提交订单" → 后端生成订单号 + 扣减库存 + 创建订单
5. 跳转到支付页面

**异常场景**：
- 库存不足 → 提示"XX 库存不足"，定位到具体商品
- 商品已下架 → 提示后移除该商品，跳回购物车
- 重复提交 → 后端幂等性处理（基于订单号去重）
- 订单创建失败 → 事务回滚，提示"下单失败，请重试"

#### 2.4.2 支付（模拟）
- 点击"立即支付" → 后端直接标记订单为"已支付" + 记录支付时间
- **注意**：秒杀订单支付需在 15 分钟内完成

#### 2.4.3 取消订单
- **规则**：下单后 30 分钟内可取消
- **待支付订单取消**：
  - 取消后还原普通商品库存
  - 秒杀商品归还库存到 Redis 秒杀池 + DB `flash_stock`（保证两者一致）
  - 超 30 分钟 → 提示"已超过取消时限"
- **已支付订单取消（退款）**：
  - 30 分钟内可申请取消，触发退款流程（详见 2.8 退款模块）
  - 取消后订单状态变更为"已取消"并记录退款金额，不可逆
  - 普通商品库存归还，秒杀商品库存归还 Redis + DB

**前端状态覆盖**：
| 状态 | 表现 |
|------|------|
| 订单为空 | Empty + "暂无订单" + "去逛逛" |
| 订单加载中 | Skeleton 列表 |
| 网络错误 | Toast + 重试按钮 |

### 2.5 商品搜索

#### 2.5.1 搜索功能
- 搜索框位于页面顶部导航栏
- 输入关键词后，跳转到搜索结果页 `/products?keyword=xxx`
- 后端：MySQL FULLTEXT INDEX（ngram parser 分词），使用参数化查询 `WHERE MATCH(name, keywords) AGAINST(#{keyword} IN BOOLEAN MODE) AND status = 1`
- 支持按价格/销量排序
- **中文分词**：ngram parser（`token_size=2`），适合中文搜索场景；V2 预留 Elasticsearch 升级路径
- **前端状态**：搜索中 → 骨架屏；搜索无结果 → "未找到相关商品"；输入为空 → 显示全部商品

### 2.6 收货地址模块

#### 2.6.1 规则
- **前置条件**：必须登录后才能管理收货地址
- **功能**：新增地址、编辑地址、删除地址（软删除 `status=0`）、设为默认地址
- **上限**：每个用户最多 20 个收货地址
- **默认地址**：每个用户只有一个默认地址，新增第一个时自动设为默认，设置新默认时取消原默认

**前端状态覆盖**：
| 状态 | 表现 |
|------|------|
| 地址为空 | Empty + "暂无收货地址，点击添加" |
| 加载中 | Skeleton |
| 表单校验 | 手机号格式、收货人必填、详细地址必填、所在地区必选 |
| 网络错误 | Toast + 表单数据保留 |
| 删除地址 | 二次确认弹窗 |

### 2.7 用户个人中心

#### 2.7.1 功能
- **个人信息**：查看/编辑头像、昵称、手机号、邮箱
- **密码修改**：输入旧密码 → 新密码 → 确认新密码（新密码最小 8 位，含字母+数字）
- **头像上传**：前端裁剪后上传至七牛云（小于 2MB，支持 jpg/png/webp）
- **订单列表**：查看个人全部订单，按状态筛选

**密码修改异常场景**：
- 旧密码错误 → 400 `OLD_PASSWORD_ERROR`
- 新密码与旧密码相同 → 400 `PASSWORD_SAME`
- 新密码强度不足 → 422 Zod 校验

### 2.8 退款模块

#### 2.8.1 退款规则
- **退款触发**：已支付订单在 30 分钟内取消 → 触发退款
- **退款流程**：订单状态变更为"已取消" → 模拟退款 → 记录退款金额
- **库存归还**：退款时普通商品归还库存，秒杀商品归还到 Redis 秒杀库存池 + DB flash_stock
- **退款后订单不可逆**：取消后的订单无法再次支付或操作

### 2.9 管理后台

#### 2.9.1 权限划分
| 角色 | 权限 |
|------|------|
| 管理员 | 全部权限（商品/订单/秒杀/数据/用户管理） |
| 运营 | 商品管理 + 秒杀管理 + 数据查看 |
| 普通用户 | 无后台权限 |

#### 2.9.2 商品管理（运营+管理员）
- 商品列表（分页、搜索）
- 新增/编辑商品（表单：名称、品牌、分类、价格、库存、规格定义、SKU管理、属性参数、主图、多图、描述）
- SKU 管理（批量生成、单独编辑价格/库存/图片）
- 上架/下架（切换状态）
- 删除商品（软删除 status=0）

#### 2.9.3 订单管理（管理员）
- 订单列表（分页、按状态筛选）
- 订单详情（商品、SKU、用户、金额、时间线）
- 发货模拟（点击发货 → 状态更新为"已发货"）

#### 2.9.4 秒杀管理（运营+管理员）
- 秒杀活动列表
- 新建活动（名称、开始时间、结束时间）
- 关联商品 + 设置秒杀价 + 秒杀库存 + 每人限购数量
- 启停活动（开始/结束/暂停）

#### 2.9.5 数据看板
| 模块 | 内容 |
|------|------|
| 销售概览 | 总订单数、总销售额、今日订单数、今日销售额 |
| 秒杀数据 | 各活动参与人数、成交订单数、售罄时间、成交率 |
| 商品排行 | 按销量排序的热门商品 Top 10 |
| 订单趋势 | 近 7 天/30 天每日订单量折线图、销售额折线图 |

---

### 2.10 管理端操作日志

#### 2.10.1 设计目标

- **可审计**：所有管理端关键操作可追溯，知道"谁在什么时候做了什么"
- **低侵入**：通过 AOP 切面自动收集日志，业务代码无需手动埋点
- **高性能**：异步落库，不阻塞主业务事务
- **可查询**：支持按时间、操作人、模块、操作类型筛选

#### 2.10.2 操作范围

需要记录日志的管理操作：

| 模块 | 操作类型 | 说明 |
|------|----------|------|
| 商品管理 | CREATE / UPDATE / DELETE / TOGGLE_STATUS | 新增/编辑/删除/上下架商品 |
| 订单管理 | UPDATE_STATUS | 发货操作 |
| 秒杀管理 | CREATE / UPDATE / DELETE / TOGGLE_STATUS | 活动 CRUD、启停 |
| 用户管理 | UPDATE_ROLE / TOGGLE_STATUS | 修改角色、禁用用户 |
| 登录相关 | LOGIN / LOGOUT | 管理员登录/登出 |

#### 2.10.3 技术方案：注解 + AOP + 异步落库

**① 自定义注解 `@OpLog`**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {
    String module();     // 模块: product / order / flash_sale / user
    String action();     // 操作: CREATE / UPDATE / DELETE / LOGIN / ...
    String description() default "";  // 操作描述，支持 SpEL: "下架商品 #{#id}"
}
```

**② AOP 切面 `OpLogAspect`**

```java
@Aspect
@Component
public class OpLogAspect {

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) {
        long start = System.currentTimeMillis();
        Object result;
        boolean success = true;
        String errorMsg = null;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // 异步收集日志，无论成功失败
            buildAndSaveLog(opLog, joinPoint, success, errorMsg, start);
        }
        return result;
    }

    @Async
    public void buildAndSaveLog(...) {
        // 从 SecurityContextHolder 获取当前用户
        // 解析 SpEL 获取动态描述
        // 从 RequestContextHolder 获取 IP
        // 组装 OperationLog 实体 → INSERT
    }
}
```

**③ 使用示例**

```java
@OpLog(module = "product", action = "CREATE", description = "新增商品: #{#req.name}")
@PostMapping("/api/admin/products")
public Result<ProductVO> create(@Valid @RequestBody ProductCreateReq req) {
    // 纯业务代码，无需关心日志
}
```

#### 2.10.4 日志落库策略

| 维度 | 策略 |
|------|------|
| **落库时机** | 操作**完成后**（finally 中触发），确保操作本身已提交或回滚 |
| **能否落库** | 日志用**独立事务传播级别**（`REQUIRES_NEW`），即使主事务回滚，操作日志仍保留 |
| **异步** | `@Async` 异步执行，不阻塞接口响应 |
| **性能影响** | 单条 INSERT < 5ms，异步后对接口延迟影响为 0 |
| **兜底** | `@Async` 线程池满时降级：日志写入内存队列，后台批量刷盘 |

#### 2.10.5 日志表（与第六章 operation_log 表一致）

```sql
CREATE TABLE `operation_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(32) NOT NULL COMMENT '操作人用户名',
  `operator_role` tinyint NOT NULL COMMENT '操作人角色',
  `module` varchar(32) NOT NULL COMMENT '操作模块: product/order/flash_sale/user',
  `action` varchar(32) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/LOGIN/...',
  `description` varchar(256) DEFAULT NULL COMMENT '操作描述',
  `target_id` varchar(64) DEFAULT NULL COMMENT '操作对象ID(商品ID/订单ID等)',
  `request_ip` varchar(45) DEFAULT NULL COMMENT '请求IP',
  `request_url` varchar(256) DEFAULT NULL COMMENT '请求URL',
  `http_method` varchar(8) DEFAULT NULL,
  `result` tinyint NOT NULL DEFAULT 1 COMMENT '操作结果: 1成功 0失败',
  `error_msg` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `duration_ms` int DEFAULT NULL COMMENT '耗时(毫秒)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_module_action` (`module`, `action`),
  INDEX `idx_operator` (`operator_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

#### 2.10.6 日志查询与管理

- **查询接口**：`GET /api/admin/operation-logs?page=1&module=product&action=CREATE&operatorId=xxx&startTime=xxx`
- **前端**：管理后台 → 系统管理 → 操作日志（表格 + 筛选）
- **保留**：默认 3 个月，定时任务按月清理过期日志
- **权限**：仅管理员可查看

#### 2.10.7 关键设计决策

| 问题 | 说明 |
|------|------|
| 为什么用 `@Async` 而非 MQ？ | 初期日志量不大，`@Async` + 独立事务足够。后续量大可升级 MQ |
| 为什么在 finally 中触发？ | 成功/失败都记录，方便排查。例如"创建商品失败"应记录尝试本身 |
| 为什么用独立事务？ | 主事务回滚时日志仍需保留。失败的操作日志有助于排查问题 |
| SpEL 有什么好处？ | 从方法参数动态构造描述，如 `"下架商品 #{#id}"`，无需手动拼接 |

---

## 三、秒杀核心 — 完整业务场景

### 3.1 角色流程

#### 运营端
1. 新建秒杀活动 → 设置名称 + 开始/结束时间
2. 在活动中添加商品 → 设置秒杀价 + 秒杀库存 + 每人限购数
3. 活动按设置时间自动开始/结束
4. 活动结束后可查看统计数据（参与人数、成交率）

#### 用户端
1. 进入秒杀专区 → 查看进行中 + 即将开始的秒杀活动列表
2. 每个活动显示倒计时
3. 点击活动进入秒杀商品列表
4. 秒杀未开始 → 显示倒计时 + "即将开始"按钮（按钮不可操作）
5. 秒杀进行中 → 显示秒杀价 + 库存进度 + "立即秒杀"按钮
6. 点击秒杀 → 校验通过 → 扣库存 → 返回"正在抢购中..."
7. 异步落单后 → 跳转到支付页面（15 分钟倒计时）
8. 支付成功 → 进入普通订单流程

### 3.2 秒杀下单状态机

```
用户点击秒杀
    │
    ▼
[前端校验] ──→ 已抢购过? → 提示"每人限购 X 件"
    │
    ▼
[后端入口] POST /api/flash-sale/order
    │
    ├── JWT 鉴权 → 401 → 跳登录
    ├── 请求频率校验 → 频率过高 → 429 "操作太快，请稍后再试"
    ├── 活动时间校验 → 未开始/已结束 → 400 "秒杀未开始/已结束"
    ├── 限购校验 → 已购数量 >= 限购数 → 400 "已达到限购数量"
    ├── Redis 预扣库存 DECR → 返回 <=0 → 回滚+返回 "已售罄"
    └── 发送 MQ 消息 → 返回 "排队中"
    │
    ▼
[前端轮询] GET /api/flash-sale/order/status?itemId=xxx
    │
    ├── pending → 继续轮询（3 秒间隔，最多 30 秒）
    ├── success + orderId → 跳转支付页面，15 分钟倒计时
    └── failed → "抢购失败"提示
    │
    ▼
[MQ 异步消费] — 幂等性：Redis SETNX + DB mq_deduplication 表双重保障
    ├── 幂等性校验 → messageKey(orderNo) 是否已处理? → 是则丢弃
    ├── 乐观锁扣 DB 库存(version) → 失败指数退避重试(50ms→200ms→500ms→1s→2s,最多5次)
    ├── 创建订单(orderNo + status=待支付, is_flash_sale=1) — 同上一个 DB 事务
    ├── 事务提交后 → 发送 RocketMQ 延迟消息(延迟 15 分钟触发超时检查)
    └── 更新 Redis 结果(success+orderNo) → 前端轮询可查到
```

### 3.3 超时未支付处理

**方案：RocketMQ 延迟消息**（替代 @Scheduled 定时扫描，避免分布式多实例重复执行）

1. **下单时发送延迟消息**：MQ 消费成功后，发送一条 RocketMQ 延迟消息，指定 15 分钟后投递
2. **消费延迟消息**：
   - 查询订单状态 → 若已支付则忽略
   - 若仍为待支付 → 分布式锁（`lock:order_timeout:{orderNo}`）→ 取消订单
   - **Redis INCR** `flash_stock:{itemId}` 归还秒杀库存
   - **DB 乐观锁** `UPDATE flash_sale_item SET flash_stock = flash_stock + 1, version = version + 1 WHERE id = #{itemId} AND version = #{version}`
   - Redis 与 DB 库存**同时恢复**，保证最终一致性
3. **消息重试**：消费失败重试 3 次，超过则进入死信队列人工处理
4. **幂等保障**：基于 `orderNo` + `mq_deduplication` 表去重（`uk_message_key_type` 唯一约束）

> **为什么用延迟消息替代定时扫描？**
> - 无需分布式锁协调定时任务，天然避免多实例重复执行
> - 精确触发，不浪费资源扫描已支付的订单
> - 15 分钟延迟，RocketMQ 5.x 支持任意时长延迟消息

---

## 四、缓存策略详解

### 4.1 缓存架构概览

```
用户请求 → Nginx → 后端 → Redis 缓存（优先）
                              │
                              ├── 命中 → 返回
                              │
                              └── 未命中 → 查询DB → 写入Redis → 返回
```

### 4.2 缓存穿透防护

| 场景 | 说明 | 策略 |
|------|------|------|
| **查询不存在的数据** | 大量请求查询不存在的商品ID，绕过 Redis 直接打 DB | ① **布隆过滤器**（推荐）：启动时将所有商品ID初始化到布隆过滤器，查询前先检查 ② **空值缓存**（兜底）：`SET product:{id}:null 1 EX 60`，TTL 短于正常缓存，避免占用内存 |
| **缓存空值过期窗口** | 空值缓存过期后的一瞬间仍有穿透风险 | 布隆过滤器 + 空值缓存双保险 |

### 4.3 缓存击穿防护

| 场景 | 说明 | 策略 |
|------|------|------|
| **热点商品过期** | 某个秒杀/热门商品缓存过期瞬间，大量请求同时打到 DB | **Redisson 分布式锁互斥加载**：`SET cache:mutex:{key} {threadId} NX EX 10`，只有获取锁的线程查 DB 并更新缓存，其他线程等待后重新读缓存 |

```java
// 互斥加载伪代码
public Product getProduct(Long id) {
    Product cached = redis.get("product:" + id);
    if (cached != null) return cached;

    RLock lock = redisson.getLock("cache:mutex:product:" + id);
    try {
        if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
            // 双重检查，其他线程可能已写入
            cached = redis.get("product:" + id);
            if (cached != null) return cached;

            Product db = productService.getById(id);
            if (db != null) {
                redis.set("product:" + id, toJson(db), 3600 + random(0, 600));
            } else {
                redis.set("product:" + id + ":null", "1", 60); // 空值缓存
            }
            return db;
        }
    } finally {
        lock.unlock();
    }
    // 获取锁失败 → 短暂等待后重试读缓存
}
```

### 4.4 缓存雪崩防护

| 场景 | 说明 | 策略 |
|------|------|------|
| **大量缓存同时过期** | 同一时间大量缓存失效，瞬间压力压垮 DB | **TTL 随机化**：`EX 3600 + random(0, 600)`（基础 60 分钟，随机偏移 0-10 分钟），分散过期时间 |
| **Redis 宕机** | 缓存服务不可用 | ① 熔断降级：Resilience4j CircuitBreaker，快速失败 ② 降级策略：商品列表返回静态/兜底数据 |

### 4.5 缓存更新策略（Cache-Aside）

```
读取：先读缓存 → 未命中 → 读DB → 写缓存
更新：先更新DB → 删除/更新缓存
删除：先删除DB → 删除缓存
```

| 操作 | 缓存动作 | 原因 |
|------|----------|------|
| 商品创建 | 不更新缓存（懒加载） | 等下次查询时按需加载 |
| 商品更新 | **删除缓存** | 避免缓存与 DB 数据不一致（比更新缓存更安全） |
| 商品下架/删除 | **删除缓存 + 删除列表缓存** | 列表页也可能包含该商品 |
| 库存变化（非秒杀） | 删除对应商品缓存 | 下次查询时重新加载最新库存 |

### 4.6 数据看板缓存

| Key | 内容 | TTL | 刷新机制 |
|-----|------|-----|----------|
| `dashboard:sales_overview` | 总订单数、总销售额、今日统计 | 5 分钟 | 定时任务 `@Scheduled` + 分布式锁，每 5 分钟异步更新 |
| `dashboard:product_ranking` | 商品销量 Top10 | 10 分钟 | 同上 |
| `dashboard:order_trend_7d` | 近 7 天每日订单量 | 30 分钟 | 同上 |
| `dashboard:order_trend_30d` | 近 30 天每日订单量 | 60 分钟 | 同上 |

### 4.7 商品列表缓存

```
Key 设计：product:list:{categoryId}:{brandId}:{minPrice}:{maxPrice}:{sort}:{page}:{pageSize}
TTL：10 分钟 + random(0, 120) 秒（防止雪崩）
```

| 注意事项 | 说明 |
|----------|------|
| **选择性缓存** | 仅缓存前 5 页热点数据，减少内存占用 |
| **关键词搜索不缓存** | 搜索条件组合无限，命中率低，直接查 DB（MySQL FULLTEXT） |
| **缓存失效** | 商品更新/下架时删除对应分类的列表缓存 |

---

## 五、事务设计详解

### 5.1 事务总览

| 场景 | 事务级别 | 传播级别 | 说明 |
|------|----------|----------|------|
| 普通下单 | `@Transactional` | `REQUIRED`（默认） | 扣库存 + 创建订单 + 创建订单商品，任一失败则全部回滚 |
| 取消订单 | `@Transactional` | `REQUIRED` | 更新订单状态 + 归还库存，保证一致性 |
| 退款处理 | `@Transactional` | `REQUIRED` | 更新订单状态 + 记录退款金额 + 归还库存 |
| 秒杀 MQ 消费 | `@Transactional` | `REQUIRED` | 乐观锁扣库存 + 创建订单；事务提交后发延迟消息 + 更新 Redis |
| 操作日志 | `@Transactional` | `REQUIRES_NEW` | 独立事务，即使主事务回滚，操作日志仍保留 |
| 购物车操作 | `@Transactional` | `REQUIRED` | 库存校验 + 加购/修改数量 |

### 5.2 普通下单事务边界

```java
@Transactional(rollbackFor = Exception.class)
public OrderResult createOrder(Long userId, CreateOrderReq req) {
    // ① 幂等性校验（基于 requestId 防止重复提交）
    // ② 校验商品是否存在 + 已上架
    // ③ 校验库存（SELECT ... FOR UPDATE 行级锁防止超卖）
    // ④ 扣减库存 product.total_stock = total_stock - quantity
    // ⑤ 扣减 SKU 库存 product_sku.stock = stock - quantity
    // ⑥ 创建订单 + 生成订单号（雪花算法或 Redis 自增）
    // ⑦ 创建订单商品快照（order_item 快照商品名/价格/SKU）
    // ⑧ 清空购物车中已下单的商品
    // 以上操作在同一事务中，任一失败全部回滚
}
```

### 5.3 取消订单事务边界

```java
@Transactional(rollbackFor = Exception.class)
public void cancelOrder(Long userId, Long orderId) {
    // ① 校验订单属于当前用户 + 状态可取消（待支付/已支付）
    // ② 校验 30 分钟内
    // ③ 更新订单状态为"已取消"
    // ④ 普通商品：product.total_stock, product_sku.stock 恢复
    // ⑤ 秒杀商品：DB flash_sale_item.flash_stock 恢复 + Redis INCR
    // ⑥ 已支付订单：记录 refund_amount + refunded_at
    // 以上操作在同一事务中
}
```

### 5.4 秒杀 MQ 消费事务边界

```java
@Transactional(rollbackFor = Exception.class)
public void consumeFlashSaleOrder(FlashOrderMessage msg) {
    // ① 幂等性校验：mq_deduplication 表唯一约束 + Redis SETNX 双重保障
    // ② 乐观锁扣库存：
    //    UPDATE flash_sale_item
    //    SET flash_stock = flash_stock - 1, version = version + 1
    //    WHERE id = #{itemId} AND version = #{version} AND flash_stock > 0
    // ③ 创建订单（orderNo + 快照 + 状态=待支付）
    // ④ 写入 mq_deduplication 去重记录
    // 事务 COMMIT 后：
    // ⑤ 发送 RocketMQ 延迟消息（15 分钟超时检查）
    // ⑥ 更新 Redis 结果（success + orderNo）→ 前端轮询可见
}
```

### 5.5 行级锁防止超卖

```sql
-- 普通商品下单时，对库存行加锁（在事务内执行）
SELECT id, stock FROM product WHERE id = #{id} FOR UPDATE;
SELECT id, stock FROM product_sku WHERE id = #{skuId} FOR UPDATE;
```

> **为什么用 `SELECT ... FOR UPDATE`？**
> MyBatis-Plus 的 `updateById` 在高并发下可能出现超卖（读-改-写 非原子）。`FOR UPDATE` 行级锁保证读和写之间其他事务无法修改该行。

---

## 六、安全防护详解

### 6.1 XSS 防护

| 环节 | 策略 |
|------|------|
| **输入过滤** | 商品描述（富文本 HTML）提交时，后端使用 **OWASP Java HTML Sanitizer** 过滤 script/iframe/onerror 等危险标签和属性，仅允许安全的白名单标签（p/br/b/img/a 等） |
| **输出编码** | 前端渲染时 React 默认转义 HTML，无需额外处理（dangerouslySetInnerHTML 使用时需经 DOMPurify 净化） |
| **URL 参数校验** | 对所有用户输入的 URL 参数做类型和格式校验 |

### 6.2 文件上传安全

| 策略 | 说明 |
|------|------|
| **魔数校验** | 服务端读取文件头魔数（magic bytes），校验文件真实类型，防止伪造 Content-Type |
| **扩展名白名单** | `.jpg, .jpeg, .png, .webp` 仅允许图片格式 |
| **大小限制** | 商品图片 ≤ 5MB，头像 ≤ 2MB（前端裁剪 + 服务端双重校验） |
| **文件名处理** | 服务端生成 UUID 文件名，不保留用户原始文件名（防止路径遍历攻击） |
| **七牛云直传** | 前端从后端获取 uploadToken（含上传策略），直传七牛云，减少后端带宽压力 |
| **文件内容扫描** | 可选：七牛云内容审核 API 检测违规图片 |

### 6.3 密码安全策略

| 策略 | 说明 |
|------|------|
| **强度要求** | 最小 8 位，必须包含字母 + 数字（前端 Zod + 后端 Hibernate Validator 双重校验） |
| **传输安全** | 前端对密码做一次 SHA-256 哈希后再传输（仅防明文传输，后端仍做 BCrypt） |
| **存储** | BCrypt 加密，`cost=12` |
| **修改密码** | 需验证旧密码正确性，新密码不能与旧密码相同 |

### 6.4 防暴力破解与限流

| 接口 | 限流维度 | 策略 |
|------|----------|------|
| 登录 | IP + 用户名 | `rate:login:{ip}` 60s 内最多 10 次；`rate:login:user:{username}` 15 分钟内最多 5 次失败 |
| 注册 | IP | `rate:register:{ip}` 1 小时内最多 3 次 |
| 秒杀 | 用户 + 商品 | `flash:rate_limit:{userId}:{itemId}` 1s 间隔 |
| 全接口 | IP | 令牌桶限流，默认 100 req/s（通过拦截器实现） |

### 6.5 敏感信息脱敏

| 场景 | 处理方式 |
|------|----------|
| **操作日志** | 请求体中的 `password`/`oldPassword`/`newPassword` 字段脱敏为 `******` |
| **日志输出** | Logback 配置脱敏规则，自动替换手机号中间 4 位、邮箱前缀部分 |
| **响应数据** | 永不返回密码字段（User 实体的 password 字段标注 `@JsonIgnore`） |

### 6.6 JWT 安全

| 策略 | 说明 |
|------|------|
| **存储位置** | localStorage（V1 简化方案），通过 XSS 防护降低盗取风险 |
| **Token 有效期** | Access Token 2 小时 |
| **续期** | Refresh Token（可选实现），过期前 10 分钟静默续期 |
| **密钥管理** | JWT 签名密钥存放在环境变量（`JWT_SECRET`），不硬编码 |

### 6.7 SQL 注入防护

| 策略 | 说明 |
|------|------|
| **参数化查询** | MyBatis-Plus 默认 `#{}` 预编译参数绑定，所有动态 SQL 强制使用参数化 |
| **LIKE 查询** | 使用 `CONCAT('%', #{keyword}, '%')` 传参，绝不拼字符串 |
| **FULLTEXT 搜索** | 使用 `AGAINST(#{keyword} IN BOOLEAN MODE)` 参数化 |
| **动态排序** | 排序字段使用白名单映射（`price_asc → ORDER BY price ASC`），不接受用户原始输入 |

---

## 七、全链路错误边界与异常处理

### 4.1 后端异常处理体系

#### 4.1.1 异常层次

```
RuntimeException
  ├── BusinessException(code, message)     # 业务异常，前端可展示
  │     ├── UserExistsException
  │     ├── PasswordErrorException
  │     ├── StockNotEnoughException        # 库存不足
  │     ├── FlashSaleNotStartedException   # 秒杀未开始
  │     ├── FlashSaleEndedException        # 秒杀已结束
  │     ├── FlashSaleLimitException        # 超过限购数
  │     ├── OrderTimeoutException          # 订单超时
  │     └── DuplicateOrderException        # 重复下单
  ├── RateLimitException                   # 限流异常 → 429
  └── AuthException                        # 认证异常 → 401
```

#### 4.1.2 GlobalExceptionHandler 统一处理

| 异常类型 | HTTP Status | code | message | 说明 |
|----------|-------------|------|---------|------|
| BusinessException | 400 | `业务 code` | 业务提示 | 前端直接展示 message |
| RateLimitException | 429 | `RATE_LIMIT` | "操作太频繁，请稍后再试" | 限流 |
| AuthException | 401 | `UNAUTHORIZED` | "登录已过期，请重新登录" | Token 失效 |
| AccessDeniedException | 403 | `FORBIDDEN` | "没有权限访问" | 角色不足 |
| MethodArgumentNotValid | 422 | `VALIDATION_ERROR` | 字段级错误列表 | 参数校验 |
| MissingServletRequestParameter | 400 | `PARAM_MISSING` | "缺少必要参数" | 缺少请求参数 |
| HttpMessageNotReadable | 400 | `PARAM_ERROR` | "请求参数格式错误" | JSON 解析失败 |
| 未捕获异常 | 500 | `SYSTEM_ERROR` | "系统繁忙，请稍后重试" | 兜底 |

**统一返回格式**：
```json
// 成功
{ "code": 200, "message": "success", "data": {}, "timestamp": 1700000000000 }

// 业务错误
{ "code": 40001, "message": "库存不足", "data": null, "timestamp": 1700000000001 }

// 校验错误（字段级）
{ "code": 422, "message": "VALIDATION_ERROR", "data": {
  "fields": [
    { "field": "username", "message": "用户名长度应在 2-32 字符之间" },
    { "field": "email", "message": "邮箱格式不正确" }
  ]
}, "timestamp": 1700000000002 }

// 分页返回
{ "code": 200, "message": "success", "data": {
  "records": [],
  "total": 100,
  "page": 1,
  "pageSize": 10,
  "totalPages": 10
}}
```

#### 4.1.3 各层异常处理职责

| 层 | 职责 |
|----|------|
| Controller | 参数校验（@Valid）+ 调用 Service，不捕获异常 |
| Service | 业务逻辑校验，抛 BusinessException |
| Mapper | MyBatis 异常，Service 层捕获并包装 |
| GlobalExceptionHandler | 统一切换异常为 JSON 响应，记录日志 |

### 4.2 前端错误边界体系

#### 4.2.1 错误边界层级

```
<App>
  └── <ErrorBoundary>                    # 全局兜底
      └── <BrowserRouter>
          └── <AuthProvider>             # 401 统一处理
              └── <Layout>
                  ├── <Header />
                  ├── <ErrorBoundary>    # 内容区隔离
                  │   └── <Outlet />    # 页面内容
                  └── <Footer />
```

#### 4.2.2 前端异常分类与处理

| 异常类型 | 来源 | 处理方式 | UI 表现 |
|----------|------|----------|---------|
| **401 Unauthorized** | Axios 响应拦截器 | 清除 Token + 跳转登录页 | 无感跳转，登录后回到原页面 |
| **403 Forbidden** | Axios 响应拦截器 | Toast 提示 | "没有权限访问" |
| **429 Rate Limit** | Axios 响应拦截器 | 计数+防抖 | "操作太频繁，请稍后再试" |
| **400 Business Error** | Axios 响应拦截器 | 展示 message | Toast / 行内提示 |
| **422 Validation Error** | Axios 响应拦截器 | 字段级映射到表单 | 表单字段下显示红色错误文案 |
| **Network Error** | Axios 响应拦截器 | 重试机制 | Toast "网络连接失败" + 重试按钮 |
| **500 Server Error** | Axios 响应拦截器 | 降级展示 | Toast "系统繁忙" + 已缓存数据继续展示 |
| **404 Not Found** | 页面路由 | 展示 404 页面 | "页面不存在" + 返回首页按钮 |
| **React 渲染崩溃** | ErrorBoundary | 捕获 + 降级 UI | "页面出错了" + "重新加载"按钮 |
| **请求超时** | Axios timeout | 取消请求 + 提示 | "请求超时，请检查网络" |

#### 4.2.3 Axios 拦截器流程

```
请求拦截器：
  └── 读取 localStorage token
      ├── 存在 → headers.Authorization = Bearer <token>
      └── 不存在 → 跳过

响应拦截器：
  └── 读取 response code
      ├── 200 → 返回 response.data
      ├── 401 → 清除 token → 跳转 /login?redirect=当前路径
      ├── 403 → Toast("没有权限访问")
      ├── 429 → Toast("操作太频繁")
      ├── 422 → 返回错误字段，表单组件映射显示
      └── 其他 → Toast(response.data.message || "系统错误")
```

---

## 八、错误场景速查表

### 8.1 注册/登录

| 场景 | 前端表现 | 后端响应 |
|------|----------|----------|
| 用户名已存在 | 输入框下方红字："该用户名已被注册" | 400, USER_EXISTS |
| 密码太短/太弱 | 输入框下方红字："密码至少 8 位，需包含字母和数字" | Zod 校验，不发送请求 |
| 用户名非法字符 | 输入框下方红字："仅支持字母、数字、中文" | Zod 校验 |
| 登录频繁 | Toast："操作太频繁，请稍后再试" | 429, LOGIN_RATE_LIMIT（IP+用户名双维度） |
| 注册频繁 | Toast："注册次数过多，请稍后再试" | 429, REGISTER_RATE_LIMIT |
| 登录密码错误 | Toast："密码错误" | 401, PASSWORD_ERROR |
| 账号被禁用 | Toast："账号已被禁用，请联系管理员" | 403, ACCOUNT_DISABLED |
| 连续输错多次 | 提示"密码错误过多，请 15 分钟后再试" | 429, LOGIN_LOCKED |

### 8.2 商品浏览

| 场景 | 前端表现 |
|------|----------|
| 商品列表加载中 | 4 列卡片 Skeleton，脉冲动画 |
| 商品列表为空（分类无商品） | Empty + "该分类暂无商品" + 返回全部分类 |
| 搜索无结果 | Empty + "没有找到相关商品" + 搜索建议 |
| 商品详情加载中 | 全页 Skeleton |
| 商品不存在/已下架 | 404 页面 + "商品已下架" + 返回列表 |
| 网络错误 | 内容区显示错误提示 + "重新加载"按钮 |
| 分页加载更多 | 加载中禁用按钮，显示 loading spinner |

### 8.3 收货地址

| 场景 | 前端表现 | 后端响应 |
|------|----------|----------|
| 地址已达上限（20个） | Toast："最多添加 20 个地址" | 400, ADDRESS_LIMIT |
| 手机号格式错误 | 输入框红色提示 | 422, VALIDATION_ERROR |
| 必填字段为空 | 输入框红色提示 | 422, VALIDATION_ERROR |
| 设置默认地址 | 异步切换，旧默认取消，新默认生效 | - |

### 8.4 购物车

| 场景 | 前端表现 | 后端响应 |
|------|----------|----------|
| 未登录访问购物车 | 跳转登录页，登录后回到购物车 | - |
| 购物车为空 | Empty + "购物车是空的" + "去逛逛"按钮 | - |
| 加购时商品已下架 | Toast："该商品已下架" | 400, PRODUCT_OFFLINE |
| 加购时库存不足 | Toast："库存不足" | 400, STOCK_NOT_ENOUGH |
| 加购数量超过 99 | 输入框限制不超 99，超过按 99 处理 | 400, MAX_LIMIT |
| 修改数量异步失败 | 数量回滚到修改前 + Toast | 500 |
| 删除商品 | 二次确认弹窗 → 删除成功 → 移除该项 | - |
| 批量删除 | 选中后出现"删除选中"按钮，二次确认 | - |

### 8.5 普通下单

| 场景 | 前端表现 | 后端响应 |
|------|----------|----------|
| 库存不足 | 弹窗："XX 商品库存不足，请调整购物车" | 400, STOCK_NOT_ENOUGH |
| 商品已下架 | 弹窗后自动移除，跳回购物车 | 400, PRODUCT_OFFLINE |
| 重复点击提交 | 按钮 loading，防止重复提交 | 幂等处理，返回相同订单号 |
| 下单成功 | 跳转支付页 + 显示订单金额 | 200, { orderId, orderNo } |

### 8.6 秒杀下单

| 场景 | 前端表现 | 后端响应 |
|------|----------|----------|
| 未登录 | 跳登录页，登录后回到秒杀页 | 401 |
| 秒杀未开始 | 按钮 "即将开始" + 倒计时，不可点击 | - |
| 秒杀已结束 | 按钮 "已结束" 灰色，文案替换 | - |
| 重复秒杀同一商品 | Toast："您已抢购过该商品" | 400, FLASH_SALE_DUPLICATE |
| 超过限购数 | Toast："每人限购 X 件" | 400, FLASH_SALE_LIMIT |
| 操作太快（1 秒内多次） | Toast："操作太频繁" | 429 |
| 库存已空 | Toast："手慢了，商品已售罄" + 按钮变灰 | 400, SOLD_OUT |
| 排队中 | 展示 "正在抢购..." + 旋转动画 | 202, { status: "pending" } |
| 抢购成功 | 自动跳转支付页 + 15 分钟倒计时 | 200, { orderId } |
| 抢购失败（队列消费异常） | Toast："抢购失败，请重试" | 轮询返回 failed |
| 支付超时 | 弹窗："支付超时，订单已取消" | 自动取消 |
| 轮询超时（30 秒无结果） | Toast："系统繁忙，请查看订单确认" | - |

### 8.7 后台管理

| 场景 | 前端表现 |
|------|----------|
| 普通用户访问 /admin | 跳转 403 页面 |
| 运营访问用户管理 | 菜单隐藏或按钮禁用 |
| 表单提交失败 | Toast + 表单保持已填数据 |
| 删除商品 | 二次确认弹窗："确定要删除 XX 吗？" |
| 上架/下架切换 | 异步操作，loading 态，成功后更新状态 |
| 创建秒杀活动（结束时间 < 开始时间） | 表单校验："结束时间不能早于开始时间" |
| 关联已关联的商品到新活动 | Toast："该商品已在其他秒杀活动中" |

---

## 九、数据库详细设计

### 6.1 完整建表 SQL

```sql
-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE `user` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(256) NOT NULL COMMENT 'BCrypt 加密密码',
  `phone` varchar(11) DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像URL',
  `role` tinyint NOT NULL DEFAULT 1 COMMENT '角色: 1普通用户 2运营 3管理员',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
  `login_attempts` tinyint DEFAULT 0 COMMENT '连续登录失败次数',
  `lock_until` datetime DEFAULT NULL COMMENT '锁定截止时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 1.1 收货地址表
-- ============================================================
CREATE TABLE `user_address` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `receiver_name` varchar(32) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(11) NOT NULL COMMENT '收货人手机号',
  `province` varchar(32) NOT NULL COMMENT '省份',
  `city` varchar(32) NOT NULL COMMENT '城市',
  `district` varchar(32) NOT NULL COMMENT '区/县',
  `detail` varchar(128) NOT NULL COMMENT '详细地址',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认地址: 1是 0否',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1正常 0删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================================================
-- 2. 商品分类表
-- ============================================================
CREATE TABLE `category` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类ID(0为顶级)',
  `level` tinyint DEFAULT 1 COMMENT '层级',
  `sort_order` int DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `status` tinyint DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ============================================================
-- 3. 品牌表
-- ============================================================
CREATE TABLE `brand` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '品牌名称',
  `logo` varchar(256) DEFAULT NULL COMMENT '品牌Logo',
  `description` varchar(256) DEFAULT NULL COMMENT '品牌描述',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

-- ============================================================
-- 4. 商品表（增强版：支持品牌、多规格SKU、属性参数）
-- ============================================================
CREATE TABLE `product` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '商品名称',
  `brief` varchar(256) DEFAULT NULL COMMENT '商品简介/卖点(列表页展示)',
  `description` text COMMENT '商品详情(富文本HTML，详情页展示)',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `brand_id` bigint DEFAULT NULL COMMENT '品牌ID',
  `unit` varchar(8) NOT NULL DEFAULT '件' COMMENT '单位: 件/箱/斤/台',
  `weight` decimal(10,2) DEFAULT NULL COMMENT '重量(kg)',
  `has_sku` tinyint NOT NULL DEFAULT 0 COMMENT '是否有SKU: 0单规格(直接库存) 1多规格',
  `price` decimal(10,2) NOT NULL COMMENT '显示价格(单规格=售价，多规格=起售价)',
  `min_price` decimal(10,2) DEFAULT NULL COMMENT 'SKU最低价(多规格时使用)',
  `max_price` decimal(10,2) DEFAULT NULL COMMENT 'SKU最高价(多规格时使用)',
  `total_stock` int NOT NULL DEFAULT 0 COMMENT '总库存(冗余，各SKU库存之和或单规格直接库存)',
  `sales` int NOT NULL DEFAULT 0 COMMENT '已售数量',
  `main_image` varchar(256) NOT NULL COMMENT '商品主图',
  `images` json DEFAULT NULL COMMENT '商品多图(数组)',
  `video_url` varchar(256) DEFAULT NULL COMMENT '商品视频',
  `specs` json DEFAULT NULL COMMENT '规格模板(多规格时定义): [{"name":"颜色","values":["银","黑"]},{"name":"存储","values":["128G","256G"]}]',
  `attrs` json DEFAULT NULL COMMENT '属性参数(结构化参数): [{"name":"屏幕尺寸","value":"6.1英寸"},{"name":"电池容量","value":"3000mAh"}]',
  `tags` json DEFAULT NULL COMMENT '商品标签: ["新品","热销","推荐"]',
  `keywords` varchar(256) DEFAULT NULL COMMENT '搜索关键词(逗号分隔，辅助搜索)',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1上架 0下架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_category` (`category_id`),
  INDEX `idx_brand` (`brand_id`),
  INDEX `idx_status_sort` (`status`, `sort_order`),
  INDEX `idx_sales` (`sales`),
  FULLTEXT INDEX `ft_search` (`name`, `keywords`) /* WITH PARSER ngram — 需在 MySQL 配置中设置 ngram_token_size=2 */
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================================
-- 5. 商品SKU表（多规格商品的核心）
-- ============================================================
CREATE TABLE `product_sku` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '所属商品ID',
  `name` varchar(128) NOT NULL COMMENT 'SKU名称: "银色 128G"',
  `attrs` json NOT NULL COMMENT 'SKU属性组合: {"颜色":"银色","存储":"128G"}',
  `price` decimal(10,2) NOT NULL COMMENT 'SKU售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT 'SKU库存',
  `code` varchar(64) DEFAULT NULL COMMENT 'SKU编码/货号(唯一)',
  `image` varchar(256) DEFAULT NULL COMMENT 'SKU专属图片(覆盖商品主图)',
  `weight` decimal(10,2) DEFAULT NULL COMMENT 'SKU重量(覆盖商品重量)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_product` (`product_id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- ============================================================
-- 6. 秒杀活动表
-- ============================================================
CREATE TABLE `flash_sale_event` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '活动名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待开始 1进行中 2已结束 3暂停',
  `remark` varchar(256) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_time` (`start_time`, `end_time`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- ============================================================
-- 7. 秒杀商品关联表
-- ============================================================
CREATE TABLE `flash_sale_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '活动ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `flash_price` decimal(10,2) NOT NULL COMMENT '秒杀价',
  `flash_stock` int NOT NULL COMMENT '秒杀专用库存',
  `limit_per_user` int NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_event` (`event_id`),
  INDEX `idx_product` (`product_id`),
  UNIQUE KEY `uk_event_product` (`event_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品关联表';

-- ============================================================
-- 8. 订单表
-- ============================================================
CREATE TABLE `orders` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号(唯一)',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待支付 1已支付 2已发货 3已完成 4已取消',
  `payment_method` varchar(16) DEFAULT NULL COMMENT '支付方式',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `address_snapshot` json DEFAULT NULL COMMENT '收货地址快照',
  `remark` varchar(256) DEFAULT NULL COMMENT '订单备注',
  `cancel_reason` varchar(128) DEFAULT NULL COMMENT '取消原因',
  `canceled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `refund_amount` decimal(10,2) DEFAULT NULL COMMENT '退款金额',
  `refunded_at` datetime DEFAULT NULL COMMENT '退款时间',
  `shipped_at` datetime DEFAULT NULL COMMENT '发货时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `is_flash_sale` tinyint NOT NULL DEFAULT 0 COMMENT '是否秒杀订单: 1是 0否',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_order_no` (`order_no`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_user_status_time` (`user_id`, `status`, `created_at`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 9. 订单商品表（增强版：增加SKU快照）
-- ============================================================
CREATE TABLE `order_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(128) NOT NULL COMMENT '商品名称(快照)',
  `product_image` varchar(256) DEFAULT NULL COMMENT '商品图片(快照)',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID(多规格时)',
  `sku_name` varchar(128) DEFAULT NULL COMMENT 'SKU名称(快照): "银色 128G"',
  `price` decimal(10,2) NOT NULL COMMENT '成交单价',
  `quantity` int NOT NULL COMMENT '购买数量',
  `subtotal` decimal(10,2) NOT NULL COMMENT '小计金额',
  INDEX `idx_order` (`order_id`),
  INDEX `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表';

-- ============================================================
-- 10. 购物车表（增强版：增加SKU支持）
-- ============================================================
CREATE TABLE `cart_item` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `sku_id` bigint DEFAULT NULL COMMENT 'SKU ID(多规格时必填)',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` tinyint NOT NULL DEFAULT 1 COMMENT '是否选中: 1是 0否',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 注意：sku_id 可为 NULL，MySQL 中 UNIQUE KEY 对 NULL 视为不同值，因此改为普通索引 + 应用层去重
  INDEX `idx_user_product_sku` (`user_id`, `product_id`, `sku_id`),
  INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ============================================================
-- 11. 管理端操作日志表
-- ============================================================
CREATE TABLE `operation_log` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(32) NOT NULL COMMENT '操作人用户名',
  `operator_role` tinyint NOT NULL COMMENT '操作人角色: 1用户 2运营 3管理员',
  `module` varchar(32) NOT NULL COMMENT '操作模块: product/order/flash_sale/user',
  `action` varchar(32) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/TOGGLE_STATUS/...',
  `description` varchar(256) DEFAULT NULL COMMENT '操作描述',
  `target_id` varchar(64) DEFAULT NULL COMMENT '操作对象ID',
  `request_ip` varchar(45) DEFAULT NULL COMMENT '请求IP',
  `request_url` varchar(256) DEFAULT NULL COMMENT '请求URL',
  `http_method` varchar(8) DEFAULT NULL,
  `result` tinyint NOT NULL DEFAULT 1 COMMENT '操作结果: 1成功 0失败',
  `error_msg` varchar(512) DEFAULT NULL COMMENT '失败原因',
  `duration_ms` int DEFAULT NULL COMMENT '请求耗时(ms)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_module_action` (`module`, `action`),
  INDEX `idx_operator` (`operator_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 12. 消息去重表（秒杀 MQ 消费幂等性保障）
-- ============================================================
CREATE TABLE `mq_deduplication` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `message_key` varchar(64) NOT NULL COMMENT '消息唯一标识(orderNo/txId)',
  `message_type` varchar(32) NOT NULL COMMENT '消息类型: FLASH_SALE_ORDER / ORDER_TIMEOUT',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2处理失败',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_message_key_type` (`message_key`, `message_type`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息去重表';
```

### 6.2 Redis 数据结构

```redis
# ============================================================
# 秒杀相关
# ============================================================
# 秒杀商品库存（运行时扣减用）
SET flash_stock:{itemId} {stock}

# 秒杀商品信息缓存
HSET flash_item:{itemId} eventId {id} productId {id} flashPrice {9.9} limitPerUser {1}

# 秒杀活动时间段
SET flash_event:{eventId} '{ "start": 1700000000, "end": 1700003600, "status": 1 }'

# 用户已购记录集合（防止重复秒杀）
SADD flash:users:{itemId} {userId}

# 用户秒杀限流（1秒间隔）
SET flash:rate_limit:{userId}:{itemId} {timestamp} EX 1

# ============================================================
# 商品相关
# ============================================================
# 商品缓存（减少DB压力）
SET product:{id} '{json数据}' EX 3600

# ============================================================
# 登录限流
# ============================================================
SET login:attempts:{username} {count} EX 900

# 用户锁定
SET login:locked:{username} 1 EX 900

# ============================================================
# 缓存防护
# ============================================================
# 空值缓存（防止缓存穿透，TTL 短于正常缓存）
SET product:{id}:null 1 EX 60

# 缓存加载互斥锁（防止缓存击穿，热点数据）
SET cache:mutex:{key} {threadId} NX EX 10

# 商品列表缓存（分类+筛选条件组合，TTL 随机化防雪崩）
SET product:list:{categoryId}:{brandId}:{sort}:{page} '{json}' EX 600

# 数据看板缓存
SET dashboard:sales_overview '{json}' EX 300

# ============================================================
# 幂等去重 & 分布式锁
# ============================================================
# 秒杀下单幂等（防止重复下单）
SET flash:dedup:{userId}:{itemId}:{requestId} 1 EX 3600

# 订单超时检查锁（防止多实例重复处理）
SET lock:order_timeout_release 1 EX 60

# 定时任务分布式锁
SET task:lock:{taskName} {instanceId} EX 30

# 登录/注册限流
SET rate:login:{ip} {count} EX 60
SET rate:register:{ip} {count} EX 3600
SET rate:login:user:{username} {count} EX 900
```

---

## 十、实施步骤

### 第一阶段：工程初始化
| # | 任务 | 产出 |
|---|------|------|
| 1 | 创建前端项目：Vite 6 + React + TS + TailwindCSS + shadcn/ui | `frontend/` |
| 2 | 创建后端项目：Spring Boot 3 + MyBatis-Plus + Maven | `backend/` |
| 3 | 配置 docker-compose（MySQL + Redis + RocketMQ NameServer + Broker） | `docker-compose.yml` |
| 3.1 | Flyway 数据库迁移初始化（V1__init.sql） | `db/migration/` |
| 4 | 后端基础框架（统一返回体、全局异常处理、CORS） | `Result.java`, `GlobalExceptionHandler.java` |
| 5 | 前端基础框架（Axios 拦截器、ErrorBoundary、路由布局） | `api/client.ts`, `App.tsx` |

### 第二阶段：用户认证
| # | 任务 | 产出 |
|---|------|------|
| 6 | 后端：用户表 + 注册/登录接口 + JWT | `UserController`, `JwtProvider` |
| 7 | 后端：Spring Security 配置 + Token 校验过滤器 | `SecurityConfig`, `JwtAuthFilter` |
| 8 | 后端：角色权限控制注解 | `@RequireRole` |
| 9 | 前端：登录/注册页面（React Hook Form + Zod） | `Login.tsx`, `Register.tsx` |
| 10 | 前端：AuthProvider + 路由守卫 + Token 管理 | `AuthProvider`, `AuthGuard` |

### 第三阶段：商品模块
| # | 任务 | 产出 |
|---|------|------|
| 11 | 后端：收货地址 CRUD 接口 | `AddressController` |
| 12 | 后端：品牌 CRUD 接口 | `BrandController` |
| 13 | 后端：分类 CRUD 接口 | `CategoryController` |
| 13 | 后端：商品 CRUD + SKU 管理 + 分页 + 搜索 + 筛选 | `ProductController`, `SkuController` |
| 14 | 前端：商品列表页（分页、筛选、搜索、骨架屏、空状态） | `ProductList.tsx` |
| 15 | 前端：商品详情页（SKU选择器、图片切换、加购） | `ProductDetail.tsx` |

### 第四阶段：购物车 + 订单
| # | 任务 | 产出 |
|---|------|------|
| 16 | 后端：购物车接口（增删改查，支持SKU） | `CartController` |
| 17 | 后端：普通下单接口（库存扣减 + 订单创建 + 事务） | `OrderController` |
| 18 | 后端：订单管理（列表、详情、取消30min限制、模拟支付） | `OrderService` |
| 19 | 前端：购物车页面（空状态、库存不足、下架） | `Cart.tsx` |
| 20 | 前端：结算/支付/订单页面 | `Checkout`, `OrderList`, `OrderDetail` |

### 第五阶段：秒杀核心（重点）
| # | 任务 | 产出 |
|---|------|------|
| 21 | 后端：秒杀活动 CRUD + 商品关联 | `AdminFlashSaleController` |
| 22 | 后端：秒杀下单核心接口（Redis 预扣 → RocketMQ 发送 → 排队中返回） | `FlashSaleController.flashOrder()` |
| 23 | 后端：RocketMQ 消费者（乐观锁扣库存 → 事务内创建订单 → 发送延迟消息） | `FlashSaleOrderConsumer` |
| 24 | 后端：Redis 预热 + 定时任务（预热、状态更新，含 Redisson 分布式锁） | `FlashScheduledTask` |
| 24.1 | 后端：RocketMQ 延迟消息消费者（15 分钟超时释放库存，Redis + DB 同步归还） | `OrderTimeoutConsumer` |
| 25 | 后端：限流拦截器（秒杀接口+登录/注册 双维度限流） | `RateLimitInterceptor` |
| 25.1 | 后端：七牛云 uploadToken 签发接口 | `UploadController` |
| 26 | 前端：秒杀专区列表页（活动列表 + 倒计时） | `FlashSaleList.tsx` |
| 27 | 前端：秒杀商品详情页（秒杀按钮状态机 + 排队动画 + 轮询） | `FlashSaleDetail.tsx` |
| 28 | 前端：秒杀支付倒计时（15 分钟超时提示） | `useCountdown` hook |

### 第六阶段：管理后台
| # | 任务 | 产出 |
|---|------|------|
| 29 | 前端：后台布局（侧边栏 + 权限菜单） | `AdminLayout.tsx` |
| 30 | 前端：商品管理页（列表 + 表单 + SKU 管理 + 上下架） | `AdminProducts.tsx` |
| 31 | 前端：订单管理页（列表 + 详情 + 发货） | `AdminOrders.tsx` |
| 32 | 前端：秒杀管理页（活动 + 商品关联） | `AdminFlashSales.tsx` |
| 33 | 前端：数据看板（ECharts/Chart.js 折线图 + 统计卡片） | `Dashboard.tsx` |
| 34 | 后端：数据统计接口 | `StatisticsController` |
| 35 | 后端：操作日志 AOP 切面 + @OpLog 注解 | `OpLogAspect.java`, `OpLog.java` |
| 36 | 后端：操作日志查询接口 | `OperationLogController` |
| 37 | 前端：操作日志管理页面（仅管理员可查看） | `AdminOperationLogs.tsx` |

### 第七阶段：Docker 构建 + 压测
| # | 任务 | 产出 |
|---|------|------|
| 38 | 后端 Dockerfile（多阶段构建，JDK 21 运行） | `backend/Dockerfile` |
| 39 | 前端 Vite 构建 + Nginx 部署配置 | `frontend/Dockerfile`, `nginx.conf` |
| 40 | 完整 docker-compose（nginx + backend + mysql + redis + rocketmq） | `docker-compose.yml` |
| 41 | JMeter 秒杀压测脚本（模拟 500 并发用户抢购） | `jmeter/flash-sale.jmx` |
| 42 | 端到端功能联调 + 压测 | 测试报告 |

### 第八阶段：测试与联调
| # | 任务 | 产出 |
|---|------|------|
| 43 | 后端单元测试（Service 层核心逻辑） | `*Test.java` |
| 44 | 前端组件测试（可选） | `*.test.tsx` |
| 45 | 全功能回归 + 边界场景验证 | 验收清单 |

---

## 十一、验证清单

### 11.1 功能验证

- [ ] 注册 → 登录 → 浏览商品 → 搜索商品 → 加购 → 下单 → 支付 → 订单查看 → 取消订单（含退款）
- [ ] 收货地址：新增 → 编辑 → 设为默认 → 删除
- [ ] 用户中心：头像上传 → 个人信息编辑 → 密码修改
- [ ] 管理后台：商品CRUD + SKU管理 → 订单管理 → 秒杀活动管理 → 数据看板 → 操作日志
- [ ] 秒杀：创建活动 → 关联商品 → 活动预热 → 用户秒杀 → 排队落单 → 支付 → 超时释放（Redis + DB 同步归还）

### 11.2 边界验证

- [ ] 购物车空状态、加载中、网络错误
- [ ] 商品列表空数据、搜索无结果、加载中
- [ ] 秒杀未开始/已结束/库存为0的按钮状态
- [ ] 重复秒杀同一商品被拒绝
- [ ] 秒杀操作过快被限流
- [ ] 下单后 30 分钟外取消被拒绝
- [ ] 未登录访问需登录页面 → 跳登录 → 登录后回到原页
- [ ] 普通用户访问 /admin → 403
- [ ] 缓存穿透防护：查询不存在商品返回空值缓存
- [ ] 缓存击穿防护：Redis 互斥锁防止热点数据同时回源

### 11.3 秒杀压测（JMeter）

- [ ] 并发 200 用户 + 100 库存：最终订单数 <= 100，无超卖
- [ ] 同一用户重复请求：每人最多 1 单
- [ ] Redis 库存 = DB flash_stock，数据一致（超时释放后均恢复）
- [ ] Redis 库存 = DB 已售总数，数据一致

### 11.4 前端 UI 检查（使用 frontend-design skill）

- [ ] 页面视觉一致性（颜色、字体、间距）
- [ ] 响应式布局（移动端/桌面端断点）
- [ ] 交互反馈（加载、悬停、点击、禁用状态）
- [ ] 错误状态展示（错误提示、重试、降级）

---

## 十二、目录结构

```
full-stack/
├── docker-compose.yml
├── nginx/
│   └── default.conf
├── frontend/
│   ├── vite.config.ts
│   ├── package.json
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   ├── Dockerfile
│   └── src/
│       ├── index.tsx
│       ├── App.tsx
│       ├── api/
│       │   ├── client.ts          # Axios 拦截器
│       │   ├── auth.ts
│       │   ├── product.ts
│       │   ├── cart.ts
│       │   ├── order.ts
│       │   └── flash-sale.ts
│       ├── stores/
│       │   ├── authStore.ts
│       │   └── cartStore.ts
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useCountdown.ts
│       │   └── useFlashSale.ts
│       ├── components/
│       │   ├── ui/                # shadcn/ui 组件
│       │   ├── ErrorBoundary.tsx
│       │   ├── Layout.tsx
│       │   └── AuthGuard.tsx
│       ├── pages/
│       │   ├── Home/
│       │   ├── Login/
│       │   ├── Register/
│       │   ├── NotFound.tsx       # 404 页面
│       │   ├── Forbidden.tsx      # 403 页面
│       │   ├── ProductList/
│       │   ├── ProductDetail/
│       │   ├── Cart/
│       │   ├── Checkout/
│       │   ├── OrderList/
│       │   ├── OrderDetail/
│       │   ├── FlashSale/
│       │   └── Admin/
│       ├── router/
│       ├── types/
│       └── utils/
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/example/mall/
│       ├── config/
│       ├── common/
│       ├── auth/
│       ├── module/
│       └── interceptor/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/           # Flyway SQL 版本文件
│       ├── MallApplication.java
│       ├── config/
│       ├── common/
│       ├── auth/
│       ├── module/
│       │   ├── user/
│       │   ├── product/
│       │   ├── cart/
│       │   ├── order/
│       │   └── flashsale/
│       └── interceptor/
│
└── jmeter/
    └── flash-sale.jmx
```
