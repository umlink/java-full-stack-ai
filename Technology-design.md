# 全栈商城系统 + 秒杀功能 — 技术方案设计

> **版本**: v1.0  
> **状态**: 设计完成，待开发  
> **关联文档**: [DEMAND.md](./DEMAND.md) | [PLAN.md](./PLAN.md)

---

## 一、文档概述

### 1.1 项目背景

构建一个全栈电商系统，涵盖用户注册/登录、商品浏览/搜索、购物车、下单支付、订单管理等完整交易链路，并重点实现**秒杀（Flash Sale）**高并发场景。

### 1.2 技术选型总览

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| **前端框架** | React + TypeScript | 18 / 5 | 生态成熟，类型安全 |
| **构建工具** | Vite 6 | 6 | 极速 HMR，Rollup + esbuild 双引擎 |
| **样式方案** | TailwindCSS | 4 | 原子化 CSS，零运行时开销 |
| **UI 组件库** | shadcn/ui | latest | 基于 Radix UI，可定制性强 |
| **前端状态管理** | Zustand | latest | 轻量、无模板代码 |
| **前端路由** | React Router | v7 | 声明式路由，支持懒加载 |
| **表单方案** | React Hook Form + Zod | latest | 高性能表单 + 类型安全校验 |
| **HTTP 客户端** | Axios | latest | 拦截器机制，请求/响应统一处理 |
| **后端框架** | Spring Boot | 3.2 | 微服务生态，自动配置 |
| **JDK** | Java | 21 | LTS 版本，虚拟线程支持 |
| **ORM** | MyBatis-Plus | 3.5 | 零 SQL CRUD + 复杂查询灵活性 |
| **数据库** | MySQL | 8.0 | 成熟关系型数据库，支持 FULLTEXT ngram |
| **缓存** | Redis + Redisson | 7 / latest | 高并发缓存 + 分布式锁 |
| **消息队列** | RocketMQ | 5.1 | 延迟消息、事务消息、高吞吐 |
| **安全框架** | Spring Security + JWT | 6 / jjwt 0.12 | 无状态认证，RBAC 控制 |
| **API 文档** | SpringDoc OpenAPI | 2 | Swagger UI，开发联调 |
| **数据库迁移** | Flyway | latest | SQL 版本化管理，dev/prod 差异化策略 |
| **对象存储** | 七牛云 Kodo | — | 前端直传，减少后端带宽 |
| **部署** | Docker Compose | — | 一键启动全部基础设施 |

### 1.3 设计原则

1. **读写分离**：高并发场景读 Redis，写异步落 DB
2. **数据一致性**：秒杀库存以 DB 为准，Redis 为快速扣减层，最终一致
3. **幂等保障**：所有写操作基于唯一键去重，MQ 消费双重幂等
4. **故障隔离**：缓存不可用时降级，MQ 不可用时限流，保证核心链路不垮
5. **安全第一**：参数化查询防注入、BCrypt 存密码、敏感信息脱敏

---

## 二、系统架构设计

### 2.1 整体架构

```
                         ┌─────────────────────┐
                         │    Browser / App     │
                         └──────────┬──────────┘
                                    │ HTTPS
                                    ▼
                         ┌─────────────────────┐
                         │   Nginx (frontend)   │
                         │     Port: 80         │
                         │   SPA + 反向代理     │
                         └──────────┬──────────┘
                                    │ /api/* → backend:8080
                                    ▼
                         ┌─────────────────────┐
                         │   Spring Boot        │
                         │   Port: 8080         │
                         │   ┌───────────────┐  │
                         │   │ Filter Chain  │  │
                         │   │ ├─ RateLimit   │  │
                         │   │ ├─ JWT Auth    │  │
                         │   │ └─ Security    │  │
                         │   ├───────────────┤  │
                         │   │ Controllers   │  │
                         │   ├───────────────┤  │
                         │   │ Services      │  │
                         │   │  + @Tx        │  │
                         │   ├───────────────┤  │
                         │   │ Mappers       │  │
                         │   └───────────────┘  │
                         └──┬──────┬───────┬────┘
                            │      │       │
                   ┌────────┘      │       └──────────┐
                   ▼               ▼                  ▼
          ┌──────────┐   ┌──────────────┐   ┌──────────────┐
          │  MySQL 8 │   │   Redis 7    │   │  RocketMQ 5  │
          │ :3306    │   │   :6379      │   │ :9876/10911  │
          │ HikariCP │   │  + Redisson  │   │ NameSrv+Brok │
          └──────────┘   └──────────────┘   └──────────────┘
```

### 2.2 后端分层架构

```
┌────────────────────────────────────────────────────────┐
│                  Interceptor Layer                      │
│   RateLimitInterceptor │ JwtAuthFilter │ OpLogAspect    │
├────────────────────────────────────────────────────────┤
│                  Controller Layer                       │
│   @Valid 参数校验 │ @PreAuthorize 权限 │ 统一返回       │
├────────────────────────────────────────────────────────┤
│                   Service Layer                         │
│   业务逻辑 │ @Transactional 事务 │ 抛 BusinessException  │
├────────────────────────────────────────────────────────┤
│                   Mapper Layer                          │
│   MyBatis-Plus BaseMapper │ 自定义 SQL │ 参数化查询     │
├────────────────────────────────────────────────────────┤
│                   Infrastructure                        │
│   MySQL │ Redis │ RocketMQ │ 七牛云 │ Flyway            │
└────────────────────────────────────────────────────────┘
```

### 2.3 前端架构

```
<App>
  └── <ErrorBoundary>                          ← 全局错误兜底
      └── <BrowserRouter>
          └── <AuthProvider>                    ← 401 统一处理
              └── <Routes>
                  ├── <Layout>                  ← 公共布局
                  │   ├── <Header />            ← 导航 + 搜索
                  │   ├── <Outlet />            ← 路由内容区
                  │   └── <Footer />
                  │
                  ├── / → Home
                  ├── /login → Login
                  ├── /register → Register
                  ├── /products → ProductList
                  ├── /products/:id → ProductDetail
                  ├── /cart → AuthGuard → Cart
                  ├── /checkout → AuthGuard → Checkout
                  ├── /orders → AuthGuard → OrderList
                  ├── /orders/:id → AuthGuard → OrderDetail
                  ├── /flash-sale → FlashSaleList
                  ├── /flash-sale/:id → FlashSaleDetail
                  ├── /user/* → AuthGuard → UserCenter
                  └── /admin/* → AdminGuard → AdminLayout
                      ├── /admin/products
                      ├── /admin/orders
                      ├── /admin/flash-sales
                      ├── /admin/dashboard
                      ├── /admin/users
                      └── /admin/operation-logs
```

**状态管理数据流**：

```
Zustand Stores:
  authStore:    { user, token, isAuthenticated, login(), logout() }
  cartStore:    { items, totalCount, addItem(), removeItem(), updateQty() }

API Layer (Axios):
  client.ts → request interceptor (attach token)
           → response interceptor (401/403/422/429/500 handling)

Custom Hooks:
  useRequest<T>  → { data, loading, error, refresh }
  useAuth        → 登录状态 + 角色判断
  useCountdown   → 倒计时（秒杀开始/支付超时）
  useFlashSale   → 秒杀状态轮询
```

### 2.4 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| ORM 选型 | MyBatis-Plus | 复杂 SQL 灵活控制（FULLTEXT 搜索、FOR UPDATE 行锁），单表 CRUD 无需手写 |
| 消息队列 | RocketMQ 5 | 支持任意时长延迟消息（订单超时），事务消息，比 RabbitMQ 更适合电商场景 |
| 缓存策略 | Cache-Aside | 上游控制缓存逻辑，缓存失效时仍可降级到 DB，避免缓存层故障影响主流程 |
| 秒杀落单 | 异步（MQ） | 解耦"抢购"和"落库"，先返回排队减少用户等待，后端平滑消费削峰 |
| 超时处理 | RocketMQ 延迟消息 | 替代 @Scheduled 定时扫描，避免分布式多实例重复执行，精确触发 |
| 购物车存储 | 服务端 MySQL | 用户决策：登录后方可使用，多设备同步 |
| 数据库迁移 | Flyway | SQL 版本化管理，可追溯、可回滚，dev 自动执行 / prod 手动执行 |

---

## 三、模块详细设计

### 3.1 用户认证模块

#### 3.1.1 注册流程

```
  用户                    前端                    后端                    MySQL
  ────                   ────                    ────                    ────
  填写表单               Zod校验
    │                     │                       │                      │
    ├─ 格式错误 ──────────┤                       │                      │
    │  行内红字提示       │                       │                      │
    │                     │  POST /api/auth/register                    │
    │                     ├──────────────────────►│                      │
    │                     │                       │  ① 校验用户名唯一     │
    │                     │                       ├─────────────────────►│
    │                     │                       │◄───── exists? ──────┤
    │                     │                       │  ② 校验邮箱唯一       │
    │                     │                       │  ③ BCrypt 加密密码    │
    │                     │                       │  ④ INSERT user       │
    │                     │                       ├─────────────────────►│
    │                     │◄── { code:200, data:{id, username} } ──────┤
    │                     │                       │                      │
    │◄─ 跳转登录页 ───────┤                       │                      │
```

**异常分支**：

| 条件 | HTTP | Code | 前端表现 |
|------|------|------|----------|
| 用户名已存在 | 400 | USER_EXISTS | 输入框下方红字"该用户名已被注册" |
| 邮箱格式错误 | 422 | VALIDATION_ERROR | Zod 同步拦截，不发送请求 |
| 密码强度不足 | 422 | VALIDATION_ERROR | Zod 拦截，"密码至少 8 位，含字母+数字" |
| 注册频率过高 | 429 | REGISTER_RATE_LIMIT | Toast "注册次数过多，请稍后再试" |

#### 3.1.2 登录流程 + JWT 机制

```
  用户                    前端                     后端                     Redis
  ────                   ────                     ────                     ────
  输入用户名+密码         Zod校验
    │                     POST /api/auth/login
    │                     ├──────────────────────►│
    │                     │                       │ ① 限流检查
    │                     │                       ├────────────────────────►│
    │                     │                       │  rate:login:{ip}       │
    │                     │                       │  60s ≤10次             │
    │                     │                       │◄───────────────────────┤
    │                     │                       │ ② 限流检查
    │                     │                       ├────────────────────────►│
    │                     │                       │  rate:login:user:{name}│
    │                     │                       │  15min ≤5次失败        │
    │                     │                       │◄───────────────────────┤
    │                     │                       │ ③ 查用户               │
    │                     │                       │ ④ BCrypt 校验密码       │
    │                     │                       │ ⑤ 生成 JWT (2h有效期)   │
    │                     │◄── { token, user } ──┤                       │
    │                     │                       │                      │
    │◄─ 存localStorage ───┤                       │                      │
    │   跳转首页          │                       │                      │
```

**JWT Payload 设计**：

```json
{
  "sub": "user_id",
  "username": "zhangsan",
  "role": 1,
  "iat": 1700000000,
  "exp": 1700007200
}
```

- **Access Token**：有效期 2 小时，签名为 HMAC-SHA256，密钥来自环境变量 `JWT_SECRET`
- **刷新策略**：Token 过期前 10 分钟，前端可用 Refresh Token 静默续期（可选实现）
- **前端存储**：localStorage（V1），通过 XSS 防护降低盗取风险
- **注销**：前端清除 Token + 清除 Zustand 状态

#### 3.1.3 角色权限模型

```
┌──────────────┬────────────────────────────────────────────┐
│ 角色         │ 权限                                       │
├──────────────┼────────────────────────────────────────────┤
│ 普通用户 (1) │ 浏览商品、搜索、购物车、下单、个人中心       │
│ 运营 (2)     │ 普通用户权限 + 商品管理 + 秒杀管理 + 数据看板 │
│ 管理员 (3)   │ 全部权限（含用户管理、订单管理、操作日志）    │
└──────────────┴────────────────────────────────────────────┘
```

实现方式：
- **后端**：`@PreAuthorize("hasRole('ADMIN')")` + `@RequireRole(min=Role.OPERATOR)` 自定义注解
- **前端**：`AuthGuard` 组件 + 路由 meta 角色信息，菜单根据角色动态展示

#### 3.1.4 登录限流

| 接口 | 维度 | 规则 | Redis Key |
|------|------|------|-----------|
| 登录 | IP | 60s 内最多 10 次 | `rate:login:{ip}` EX 60 |
| 登录 | 用户名 | 15min 内最多 5 次失败 | `rate:login:user:{username}` EX 900 |
| 注册 | IP | 1h 内最多 3 次 | `rate:register:{ip}` EX 3600 |

连续 5 次密码错误后，锁定 15 分钟：`SET login:locked:{username} 1 EX 900`

#### 3.1.5 密码安全链

```
用户输入密码 (明文)
    │
    ▼
前端 SHA-256 (传输层防护，防明文泄露)
    │
    ▼
HTTPS 传输
    │
    ▼
后端 BCrypt (cost=12, 存储层防护)
    │
    ▼
入库: password VARCHAR(256)
```

---

### 3.2 商品模块

#### 3.2.1 商品模型：SPU + SKU 两层模型

```
┌─────────────────────────────────────────────────────┐
│  product (SPU 层)                                    │
│  ┌─────────────────────────────────────────────────┐ │
│  │ id │ name │ category_id │ brand_id │ status     │ │
│  │ has_sku │ price │ min_price │ max_price         │ │
│  │ total_stock (冗余) │ main_image │ images (JSON) │ │
│  │ specs (JSON) │ attrs (JSON) │ keywords          │ │
│  └─────────────────────────────────────────────────┘ │
│                         │ 1:N                        │
│                         ▼                            │
│  ┌─────────────────────────────────────────────────┐ │
│  │ product_sku (SKU 层)                             │ │
│  │ name: "银色 128G" │ attrs: {"颜色":"银","存储":"128G"} │ │
│  │ price: 199.00 │ stock: 50 │ code: "SKU-001"    │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘

单规格商品 (has_sku=0):
  product.price = 售价, product.total_stock = 库存
  无 product_sku 记录

多规格商品 (has_sku=1):
  product.price = 最低售价 (min_price)
  product_sku 每条记录代表一个规格组合
  product.total_stock = SUM(product_sku.stock)，冗余存储
```

**specs 字段（规格模板）**：

```json
[
  {"name": "颜色", "values": ["银色", "黑色", "金色"]},
  {"name": "存储", "values": ["128G", "256G", "512G"]}
]
```

**attrs 字段（属性参数）**：

```json
[
  {"name": "屏幕尺寸", "value": "6.1英寸"},
  {"name": "电池容量", "value": "3000mAh"},
  {"name": "重量", "value": "170g"}
]
```

#### 3.2.2 商品搜索方案

```
┌──────────────┐     ┌──────────────────────────────────┐
│  搜索输入     │────►│  MySQL FULLTEXT INDEX            │
│  "手机 银色"   │     │  WITH PARSER ngram               │
└──────────────┘     │  MATCH(name, keywords) AGAINST    │
                     │  (#{keyword} IN BOOLEAN MODE)     │
                     │  ngram_token_size = 2              │
                     └──────────────────────────────────┘
                              │
                              ▼
                     ┌──────────────────────────────────┐
                     │  Future: Elasticsearch (分词更精准) │
                     │  预留升级路径，MySQL FULLTEXT 过渡   │
                     └──────────────────────────────────┘
```

**关键设计**：
- 使用 MySQL 参数化查询防止注入：`AGAINST(#{keyword} IN BOOLEAN MODE)`
- ngram token_size=2 适合中文：支持无空格分词
- 搜索关键词不入缓存（组合无限，命中率低）

#### 3.2.3 商品列表缓存策略

```
Key 格式:  product:list:{categoryId}:{brandId}:{minPrice}:{maxPrice}:{sort}:{page}:{pageSize}
数据类型:  String (JSON)
TTL:       600s + random(0, 120)s (基础10分钟，随机偏移防止雪崩)

选择性缓存:
  - 仅缓存前 5 页热门数据
  - 关键词搜索不缓存（直接查 DB）
  - 商品更新/下架时删除对应分类的列表缓存
```

#### 3.2.4 SKU 选择器交互逻辑

```
用户进入详情页
    │
    ▼
加载 product + product_sku 列表
    │
    ├─ has_sku=0 (单规格) → 直接显示价格/库存，"加入购物车"可点击
    │
    └─ has_sku=1 (多规格)
        │
        ▼
    渲染规格选择器（specs 驱动）
        │
        ▼
    用户选择属性组合（如：颜色=银色 + 存储=128G）
        │
        ▼
    前端匹配 product_sku.attrs
        │
        ├─ 匹配成功 → 更新价格/库存/图片（SKU专属图片优先，否则使用主图）
        │            → "加入购物车"可点击
        │
        └─ 该组合不存在（如：银色+256G 无货）
            → 显示"该组合暂无库存"，按钮禁用
```

#### 3.2.5 前端状态覆盖

| 状态 | 组件 | 表现 |
|------|------|------|
| 加载中 | Skeleton | 4列骨架屏，脉冲动画 |
| 空数据 | Empty | "该分类暂无商品" + 推荐分类 |
| 搜索无结果 | Empty | "没有找到相关商品，试试其他关键词" |
| 网络错误 | Error + Button | "重新加载"按钮 |
| 商品下架 | 404页面 | "商品已下架" + 返回列表 |
| 全部库存为0 | Button(disabled) | "暂时缺货"灰色按钮 |

---

### 3.3 购物车模块

#### 3.3.1 存储策略与加购流程

```
  用户点击"加入购物车"
    │
    ▼
  ┌─ 是否已登录？ ──→ 否 ──→ 弹窗："登录后即可使用购物车"
  │                           ├─ "去登录" → /login?redirect=当前页
  │                           └─ "稍后再说" → 关闭弹窗
  │
  ▼ 是
  ┌─ 商品是否上架？
  │   否 → Toast "该商品已下架"
  ▼ 是
  ┌─ 多规格商品是否已选 SKU？
  │   否 → 焦点回到 SKU 选择器，提示"请选择规格"
  ▼ 是
  ┌─ SKU 库存 > 0？
  │   否 → Toast "该规格库存不足"
  ▼ 是
  ┌─ 已有数量 >= 99？
  │   是 → Toast "最多购买 99 件"
  ▼ 否
  POST /api/cart/add
  → INSERT INTO cart_item (user_id, product_id, sku_id, quantity) 或 UPDATE quantity
  → 返回最新购物车列表
```

#### 3.3.2 cart_item 表设计约束

```sql
-- sku_id 可为 NULL，MySQL 中 UNIQUE KEY 对 NULL 视为不同值
-- 因此用普通索引 + 应用层去重：
-- 加购前先查询是否已存在相同 (user_id, product_id, sku_id) 记录
-- 若已存在则 UPDATE quantity = quantity + N，否则 INSERT
```

| 限制 | 值 | 处理 |
|------|-----|------|
| 单 SKU 最多 | 99 件 | 前端输入框 + 后端双重校验 |
| 多规格商品 | 必须有 sku_id | 后端校验 |
| 单规格商品 | sku_id = NULL | 允许 |

#### 3.3.3 前端状态

```
Zustand cartStore:
  进入购物车页面时:
    ① 显示 Skeleton
    ② GET /api/cart → 获取购物车列表
    ③ 检查每个商品状态：
       ├─ 已下架 → 行标灰 + "该商品已下架"
       ├─ 库存不足 → 显示"仅剩 X 件"，数量自动限制
       └─ 正常 → 正常显示
    ④ 网络错误 → Toast + 重试按钮

  修改数量:
    ① set cartStore.loading = true (按钮禁用)
    ② PUT /api/cart/{itemId}?quantity=N
    ③ 失败 → 恢复原数量 + Toast
    ④ set cartStore.loading = false

  删除商品:
    ① 二次确认弹窗
    ② DELETE /api/cart/{itemId}
    ③ 成功后从列表移除
```

---

### 3.4 订单模块

#### 3.4.1 订单状态机

```
                    ┌──────────┐
                    │  0 待支付  │
                    └────┬─────┘
                         │
              ┌──────────┼──────────┐
              │ 支付      │ 超时(15min)│ 取消(30min内)
              ▼          ▼           ▼
         ┌──────────┐ ┌──────────┐
         │ 1 已支付  │ │ 4 已取消  │
         └────┬─────┘ └──────────┘
              │                    ▲
              │ 发货               │ 退款(30min内已支付取消)
              ▼                    │
         ┌──────────┐             │
         │ 2 已发货  │─────────────┘
         └────┬─────┘
              │ 用户确认收货
              ▼
         ┌──────────┐
         │ 3 已完成  │
         └──────────┘
```

#### 3.4.2 普通下单流程

```
  用户              前端               后端                     MySQL
  ────              ────              ────                     ────
  购物车选商品
  → 点击结算
  确认商品+地址
  点击提交订单
                    POST /api/orders
                    ├───────────────►│
                    │                │ ① 幂等：Redis SETNX 去重
                    │                │    flash:dedup:{userId}:{requestId}
                    │                │    已存在 → 返回已创建的订单号
                    │                │
                    │                │ ② 校验商品上架 + 库存
                    │                │    锁定库存：SELECT ... FOR UPDATE
                    │                ├────────────────────────────────►│
                    │                │◄─── locked rows ────────────────┤
                    │                │
                    │                │ ③ 扣减库存 (事务内)
                    │                │    UPDATE product SET total_stock = total_stock - ?
                    │                │    UPDATE product_sku SET stock = stock - ?
                    │                │
                    │                │ ④ 创建订单 (事务内)
                    │                │    INSERT INTO orders (order_no, user_id, total_amount...)
                    │                │    INSERT INTO order_item (snapshot: name/price/sku)
                    │                │    ⚠️ order_item 保存的是快照，不随商品更新变化
                    │                │
                    │                │ ⑤ 清空购物车已下单项 (事务内)
                    │                │    DELETE FROM cart_item WHERE id IN (...)
                    │                │
                    │                │ ⑥ 事务 COMMIT
                    │                │
                    │◄── { orderId, orderNo } ─┤
                    │                │
  跳转支付页面       │                │
```

**事务隔离级别**：READ_COMMITTED (MySQL 默认)  
**行锁策略**：`SELECT ... FOR UPDATE` 锁定 product + product_sku 行，防止超卖

#### 3.4.3 取消订单流程

```
  POST /api/orders/{orderId}/cancel
    │
    ▼
  ① 校验：订单属于当前用户
  ② 校验：订单状态为 待支付(0) 或 已支付(1)
  ③ 校验：created_at + 30min > now
    ├─ 超时 → 400 "已超过取消时限"
    │
  ④ @Transactional
    ├─ UPDATE orders SET status=4, canceled_at=NOW(), cancel_reason=?
    ├─ 待支付 → 归还库存
    │   ├─ UPDATE product SET total_stock = total_stock + ?
    │   ├─ UPDATE product_sku SET stock = stock + ?
    │   └─ 秒杀商品：Redis INCR flash_stock:{itemId} + DB flash_stock+1
    ├─ 已支付 → 记录退款
    │   └─ UPDATE orders SET refund_amount=?, refunded_at=NOW()
    └─ 归还秒杀库存（同上）
```

#### 3.4.4 模拟支付

```
POST /api/orders/{orderId}/pay
  → 调用模拟支付接口
  → UPDATE orders SET status=1, paid_at=NOW(), payment_method='SIMULATED'
  → 返回支付成功

秒杀订单：15分钟内须支付，否则 RocketMQ 延迟消息触发超时取消
```

---

### 3.5 秒杀模块（核心）

#### 3.5.1 秒杀架构全景

```
  运营端：
    创建活动 → 关联商品 → 设置秒杀价 + 库存 + 限购数
    → 活动按时间自动启停（@Scheduled + Redisson 分布式锁）
    → 开始前预热：Redis HST flash_stock + flash_item 信息

  用户端：
    浏览秒杀专区 → 倒计时 → 点击秒杀
      ↓
  ╔══════════════════════════════════════════════════╗
  ║              秒杀核心链路                          ║
  ║                                                    ║
  ║  ① [前端] 已抢购？(本地标记)                        ║
  ║       ↓                                            ║
  ║  ② [后端-限流] rate:flash:{userId}:{itemId} 1s     ║
  ║       ↓                                            ║
  ║  ③ [后端-校验] 时间 / 限购 / 已购                   ║
  ║       ↓                                            ║
  ║  ④ [Redis] DECR flash_stock:{itemId} (预扣)        ║
  ║       ├─ >0 → 发送 MQ 消息                          ║
  ║       └─ ≤0 → 回复库存 + 返回 "已售罄"              ║
  ║       ↓                                            ║
  ║  ⑤ [返回] 202 { status:"pending" }                  ║
  ║       ↓                                            ║
  ║  ⑥ [前端] 轮询 (3s间隔, 最多30s)                     ║
  ║       ↓                                            ║
  ║  ⑦ [MQ消费者] 幂等 → 乐观锁扣DB → 创建订单           ║
  ║     → COMMIT → 延迟消息 → 更新Redis结果              ║
  ║       ↓                                            ║
  ║  ⑧ [前端] 轮询查到 orderId → 跳转支付               ║
  ║                                                    ║
  ║  ⑨ [延迟消息] 15min后检查 → 未支付 → 取消 → 归还库存 ║
  ╚══════════════════════════════════════════════════════╝
```

#### 3.5.2 秒杀接口详细流程

```
POST /api/flash-sale/order  { itemId }
    │
    ▼
┌─ JWT 鉴权 ──→ 失败 → 401
│
▼ 鉴权通过
┌─ 限流检查 (Redis SET rate key NX EX 1)
│   失败 → 429 "操作太快，请稍后再试"
│
▼ 限流通过
┌─ 活动时间校验 (从 Redis flash_event:{eventId} 获取)
│   未开始 → 400 "秒杀尚未开始"
│   已结束 → 400 "秒杀已结束"
│
▼ 时间合法
┌─ 限购校验 (SISMEMBER flash:users:{itemId} {userId})
│   已购买 → 400 "您已抢购过该商品"
│
▼ 未超限购
┌─ Redis 预扣库存 DECR flash_stock:{itemId}
│   ├─ >0  → 继续
│   └─ ≤0  → INCR flash_stock:{itemId} 回滚 + 400 "已售罄"
│
▼ 预扣成功
┌─ 构造 FlashOrderMessage { orderNo, userId, itemId }
│   发送到 RocketMQ Topic: flash-sale-order
│
▼ 发送成功
┌─ 返回 202 { status: "pending", requestId }
│
▼
前端开始轮询 GET /api/flash-sale/order/status?requestId=xxx
    3秒间隔，最多30秒
    ├─ pending → 继续轮询
    ├─ success + orderNo → 跳转支付 15分钟倒计时
    └─ failed → Toast "抢购失败，请重试"
```

#### 3.5.3 RocketMQ 消费者设计

```java
// Topic: flash-sale-order
// ConsumerGroup: flash-sale-order-consumer
@RocketMQMessageListener(topic = "flash-sale-order", consumerGroup = "flash-sale-order-consumer")
public class FlashSaleOrderConsumer implements RocketMQListener<FlashOrderMessage> {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(FlashOrderMessage msg) {
        // ① 幂等性校验 (双重)
        //    a) Redis SETNX flash:dedup:order:{orderNo} EX 3600
        //    b) INSERT INTO mq_deduplication (message_key, message_type)
        //       unique key: uk_message_key_type 防止重复消费
        //    若已处理 → 直接 return ACK

        // ② 乐观锁扣库存 (最多 5 次重试，指数退避)
        //    UPDATE flash_sale_item
        //    SET flash_stock = flash_stock - 1, version = version + 1
        //    WHERE id = #{itemId} AND version = #{version} AND flash_stock > 0
        //    受影响行数 = 0 → 重试 (50ms → 200ms → 500ms → 1s → 2s)
        //    全部失败 → 抛异常触发 MQ 重试

        // ③ 创建订单 (与②同一事务)
        //    INSERT INTO orders (order_no, is_flash_sale=1, status=0...)
        //    INSERT INTO order_item (快照)
        //    INSERT INTO mq_deduplication (确认消息已消费)

        // 事务 COMMIT 后:
        // ④ 发送 RocketMQ 延迟消息 (DELAY 15min)
        //    Topic: order-timeout, delayLevel: 15min

        // ⑤ 更新 Redis 结果
        //    SET flash:result:{requestId} '{"status":"success","orderNo":"xxx"}' EX 60

        // ⑥ 记录用户已购
        //    SADD flash:users:{itemId} {userId}
    }
}
```

**乐观锁重试指数退避**：

```
第1次失败 → sleep 50ms   → 重试
第2次失败 → sleep 200ms  → 重试
第3次失败 → sleep 500ms  → 重试
第4次失败 → sleep 1s     → 重试
第5次失败 → sleep 2s     → 重试

全部5次失败 → 抛异常 → MQ 消费重试（3次）→ 死信队列
```

#### 3.5.4 超时未支付：RocketMQ 延迟消息

```
下单时发送延迟消息:
  Message msg = new Message("order-timeout", orderNo);
  msg.setDelayTimeLevel(15_minutes_level);
  rocketMQTemplate.send("order-timeout", msg);

15分钟后触发消费:
  @RocketMQMessageListener(topic = "order-timeout", ...)
  public void onMessage(Message msg) {
      ① 查询订单状态
         ├─ 已支付 (status=1) → ACK 忽略
         └─ 待支付 (status=0) → 继续

      ② 分布式锁 Redisson
         RLock lock = redisson.getLock("lock:order_timeout:" + orderNo);
         lock.tryLock(10, 30, SECONDS);

      ③ @Transactional 取消订单
         ├─ UPDATE orders SET status=4, cancel_reason='支付超时'
         ├─ Redis INCR flash_stock:{itemId} (归还秒杀库存)
         └─ UPDATE flash_sale_item SET flash_stock = flash_stock + 1,
             version = version + 1 WHERE id = ? AND version = ? (DB归还)

      ④ 释放锁
  }
```

**对比 @Scheduled 扫描方案**：

| 维度 | RocketMQ 延迟消息 | @Scheduled 定时扫描 |
|------|------------------|---------------------|
| 精确性 | 精确 15min 触发 | 依赖扫描间隔（如 30s） |
| 分布式 | 天然支持多实例 | 需分布式锁协调，多实例易重复 |
| 性能 | O(1) 单条触发 | O(n) 扫描所有待支付订单 |
| 可靠性 | MQ 重试机制 | 实例宕机丢失当次扫描 |

#### 3.5.5 幂等性双重保障

```
Redis: SETNX flash:dedup:order:{orderNo} 1 EX 3600
  → 快速判重，但有 TTL 限制

DB: mq_deduplication 表
  → UNIQUE KEY uk_message_key_type (message_key, message_type)
  → 永久去重，事务层面保障
  → INSERT ... ON DUPLICATE KEY UPDATE 或 try-catch DuplicateKeyException
```

#### 3.5.6 库存预热

```java
@Scheduled(fixedRate = 30000) // 每30秒检查
public void preheatFlashSaleStock() {
    RLock lock = redisson.getLock("task:lock:flash_preheat");
    if (lock.tryLock()) {
        try {
            // 查询即将开始（≤5分钟）且处于预热状态的秒杀活动
            List<FlashSaleItem> items = flashSaleItemService.getNeedPreheat();
            for (FlashSaleItem item : items) {
                // 初始化 Redis 库存
                String key = "flash_stock:" + item.getId();
                if (redisTemplate.opsForValue().get(key) == null) {
                    redisTemplate.opsForValue().set(key, item.getFlashStock());
                }
                // 缓存秒杀商品信息
                redisTemplate.opsForHash().putAll("flash_item:" + item.getId(), buildItemMap(item));
            }
        } finally {
            lock.unlock();
        }
    }
}
```

#### 3.5.7 前端秒杀交互设计

```
秒杀按钮状态机:
  ┌──────────┐
  │ 即将开始   │ 灰色按钮 + 倒计时，不可点击
  └─────┬────┘
        │ start_time 到达
        ▼
  ┌──────────┐
  │ 立即秒杀   │ 红色/橙色高亮按钮，可点击
  └─────┬────┘
        │ 用户点击
        ▼
  ┌──────────┐
  │ 正在抢购   │ 按钮禁用 + 旋转动画
  │ 排队中...  │ 轮询结果 (3s/最多30s)
  └─────┬────┘
        ├─ success + orderNo → 跳转支付页 (15min倒计时)
        ├─ failed → Toast "抢购失败" + 按钮恢复
        └─ timeout (30s) → Toast "系统繁忙，请查看订单"
  ┌──────────┐
  │ 已售罄    │ 灰色按钮"已售罄"
  └─────┬────┘
        │ end_time 到达
        ▼
  ┌──────────┐
  │ 已结束    │ 灰色按钮"已结束"
  └──────────┘

倒计时组件 useCountdown:
  - 秒杀开始倒计时: "距开始 02:30:15"
  - 支付超时倒计时: "请在 14:59 内完成支付"
  - 到达 0 触发回调
```

---

### 3.6 管理后台模块

#### 3.6.1 权限控制架构

```
后端:
  接口层: @PreAuthorize("hasRole('ADMIN')") 或 @RequireRole
  URL 层: SecurityConfig 配置不同角色可访问的路径模式
          .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "ADMIN")

前端:
  路由层: <AdminGuard minRole="OPERATOR">
           └─ 检查 authStore.user.role >= minRole
               ├─ 通过 → 渲染页面
               └─ 不通过 → <Forbidden />

  菜单层: AdminLayout 根据 role 动态显示/隐藏菜单项
          ├─ 运营: 商品管理 | 秒杀管理 | 数据看板
          └─ 管理员: 全部菜单（含用户管理 | 订单管理 | 操作日志）
```

#### 3.6.2 操作日志 AOP 方案

**注解定义**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {
    String module();     // product / order / flash_sale / user
    String action();     // CREATE / UPDATE / DELETE / TOGGLE_STATUS / LOGIN
    String description() default "";  // SpEL: "下架商品 #{#id}"
}
```

**切面实现**：

```java
@Aspect
@Component
public class OpLogAspect {

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint jp, OpLog opLog) {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            return jp.proceed();
        } catch (Exception e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            // @Async 异步落库，独立事务 REQUIRES_NEW
            buildAndSaveLog(opLog, jp, success, errorMsg, start);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void buildAndSaveLog(...) {
        // ① 从 SecurityContextHolder 获取当前用户
        // ② 解析 SpEL 获取动态描述
        // ③ 从 RequestContextHolder 获取 IP
        // ④ 组装 OperationLog → INSERT
    }
}
```

**关键设计决策**：

| 问题 | 决策 | 原因 |
|------|------|------|
| 何时触发 | finally 中 | 成功/失败都记录，失败的操作同样有排查价值 |
| 事务策略 | REQUIRES_NEW | 主事务回滚时日志不丢失 |
| 执行方式 | @Async 异步 | 不阻塞接口响应，性能影响趋零 |
| 降级策略 | 线程池满 → 内存队列 | 日志不丢失，批量刷盘 |
| 升级路径 | 量大时切 MQ | 初期 @Async 够用 |

#### 3.6.3 数据看板

```
┌─────────────────────────────────────────────────────┐
│                    数据看板                           │
│  ┌──────────────┬──────────────┬──────────────┐     │
│  │  总订单数     │  总销售额     │  今日订单数    │     │
│  │  12,345      │  ¥567,890    │  234          │     │
│  └──────────────┴──────────────┴──────────────┘     │
│  ┌──────────────────────────────────────────────┐   │
│  │  近 7 天订单趋势 (折线图)                       │   │
│  │  📈  ECharts Line Chart                      │   │
│  └──────────────────────────────────────────────┘   │
│  ┌──────────────────┬──────────────────────────┐    │
│  │  商品销量 Top10   │  秒杀活动数据              │    │
│  │  🏆 Rank List    │  📊 Stats Cards           │    │
│  └──────────────────┴──────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

**缓存刷新机制**：

| Key | TTL | 刷新 |
|-----|-----|------|
| `dashboard:sales_overview` | 5 min | `@Scheduled(fixedRate=300000)` + 分布式锁 |
| `dashboard:product_ranking` | 10 min | 同上 |
| `dashboard:order_trend_7d` | 30 min | 同上 |
| `dashboard:order_trend_30d` | 60 min | 同上 |

---

## 四、数据库设计

### 4.1 ER 图

```
                         user
                      ┌──────────────┐
                      │ id (PK)      │
                      │ username     │
                      │ password     │
                      │ role         │
                      │ status       │
                      └──────┬───────┘
               ┌─────────────┼─────────────────────┐
               │ 1:N         │ 1:N                 │ 1:N
               ▼             ▼                     ▼
        user_address     cart_item              orders
  ┌─────────────────┐ ┌──────────────┐ ┌──────────────────────┐
  │ id (PK)         │ │ id (PK)      │ │ id (PK)              │
  │ user_id (FK)    │ │ user_id (FK) │ │ order_no (UK)        │
  │ receiver_name   │ │ product_id   │ │ user_id (FK)         │
  │ receiver_phone  │ │ sku_id       │ │ total_amount         │
  │ province/city   │ │ quantity     │ │ status               │
  │ district/detail │ │ selected     │ │ paid_at / refunded_at│
  │ is_default      │ └──────┬───────┘ │ address_snapshot(JSON)│
  │ status          │        │         └──────────┬───────────┘
  └─────────────────┘        │                    │ 1:N
                             │                    ▼
  ┌──────────────────┐      │              order_item
  │ flash_sale_event │      │         ┌──────────────────────┐
  │ id (PK)          │      │         │ id (PK)              │
  │ name             │      │         │ order_id (FK)        │
  │ start/end_time   │      │         │ product_id           │
  │ status           │      │         │ product_name (快照)   │
  └────────┬─────────┘      │         │ sku_id / sku_name    │
           │ 1:N            │         │ price / quantity     │
           ▼                │         └──────────────────────┘
  ┌──────────────────┐      │
  │ flash_sale_item  │      │          category
  │ id (PK)          │      │    ┌──────────────────┐
  │ event_id (FK)    │      │    │ id (PK)          │
  │ product_id (FK)──┼──────┼────┤ parent_id        │
  │ flash_price      │      │    │ name / level     │
  │ flash_stock      │      │    │ sort_order       │
  │ limit_per_user   │      │    └────────┬─────────┘
  │ version (乐观锁)  │      │             │ N:1
  └──────────────────┘      │             ▼
                            │      product ◄──────────── brand
                            │ ┌──────────────────┐ ┌──────────────┐
                            │ │ id (PK)          │ │ id (PK)      │
                            │ │ name / brief     │ │ name / logo  │
                            │ │ category_id (FK) │ │ description  │
                            │ │ brand_id (FK)◄───┼─┤ sort_order   │
                            │ │ has_sku / price  │ └──────────────┘
                            │ │ specs (JSON)     │
                            │ │ attrs (JSON)     │
                            │ │ total_stock/sales│
                            │ │ status           │
                            │ └────────┬─────────┘
                            │          │ 1:N
                            │          ▼
                            │   product_sku
                            │ ┌──────────────────┐
                            │ │ id (PK)          │
                            │ │ product_id (FK)  │
                            │ │ name / attrs     │
                            │ │ price / stock    │
                            │ │ code (UK)        │
                            │ │ image            │
                            │ └──────────────────┘
                            │
  operation_log            │        mq_deduplication
┌──────────────────┐      │    ┌──────────────────────┐
│ id (PK)          │      │    │ id (PK)              │
│ operator_id      │      │    │ message_key (UK)     │
│ module / action  │      │    │ message_type         │
│ description      │      │    │ status               │
│ target_id        │      │    │ retry_count          │
│ result / duration│      │    └──────────────────────┘
└──────────────────┘      │
                          │
```

### 4.2 12 张表详细说明

| # | 表名 | 用途 | 关键索引 |
|---|------|------|----------|
| 1 | `user` | 用户账户 | `uk_username`, `uk_email` |
| 2 | `user_address` | 收货地址（最多20个/用户） | `idx_user_default (user_id, is_default)` |
| 3 | `category` | 商品分类（树形结构） | `idx_parent` |
| 4 | `brand` | 品牌 | `uk_name` |
| 5 | `product` | 商品 SPU 层 | `idx_category`, `idx_brand`, `idx_status_sort`, `ft_search` (FULLTEXT) |
| 6 | `product_sku` | 商品 SKU 层 | `idx_product`, `uk_code` |
| 7 | `flash_sale_event` | 秒杀活动 | `idx_time`, `idx_status` |
| 8 | `flash_sale_item` | 秒杀商品关联 | `uk_event_product`, `version` (乐观锁) |
| 9 | `orders` | 订单 | `uk_order_no`, `idx_user_id`, `idx_status`, `idx_user_status_time` |
| 10 | `order_item` | 订单商品快照 | `idx_order`, `idx_product` |
| 11 | `cart_item` | 购物车 | `idx_user_product_sku` |
| 12 | `mq_deduplication` | MQ 消费幂等 | `uk_message_key_type` |
| 13 | `operation_log` | 管理端操作日志 | `idx_module_action`, `idx_operator`, `idx_created_at` |

### 4.3 索引设计策略

| 策略 | 说明 |
|------|------|
| **覆盖索引** | `idx_user_status_time (user_id, status, created_at)` 覆盖用户订单列表查询 |
| **前缀索引** | 未使用（所有字符串列均较短） |
| **FULLTEXT 索引** | `product.name + product.keywords` 使用 ngram parser (`token_size=2`)，支持中文分词搜索 |
| **唯一索引** | 用户名、邮箱、订单号、SKU编码、消息去重键 |
| **乐观锁** | `flash_sale_item.version` 配合 MyBatis-Plus `@Version` |

### 4.4 Flyway 版本管理

```
backend/src/main/resources/db/migration/
├── V1__init.sql              # 初始建表（全部 13 张表）
├── V2__add_address_table.sql
├── V3__add_refund_fields.sql
└── V4__add_mq_dedup_table.sql
```

**文件命名规范**：`V{version}__{description}.sql`，version 递增不可重复

**多环境差异**：

| 配置 | dev | prod |
|------|-----|------|
| `flyway.migrate-on-start` | true（自动） | false（手动） |
| `flyway.clean-on-validation-error` | true | false |

---

## 五、Redis 缓存设计

### 5.1 Redis 数据结构全景

| Key Pattern | 类型 | TTL | 说明 |
|-------------|------|-----|------|
| **秒杀** | | | |
| `flash_stock:{itemId}` | String (int) | 活动时长 | 秒杀库存实时扣减 |
| `flash_item:{itemId}` | Hash | 活动时长 | 秒杀商品信息缓存 |
| `flash_event:{eventId}` | String (JSON) | 活动时长 | 活动时间/状态 |
| `flash:users:{itemId}` | Set | 活动时长 | 已购用户集合 |
| `flash:rate_limit:{userId}:{itemId}` | String | 1s | 下单频率限制 |
| `flash:dedup:order:{orderNo}` | String | 3600s | 秒杀下单幂等 |
| `flash:result:{requestId}` | String (JSON) | 60s | 前端轮询结果 |
| **商品** | | | |
| `product:{id}` | String (JSON) | 3600s + rand(0,600) | 商品详情缓存 |
| `product:{id}:null` | String | 60s | 空值缓存（穿透防护） |
| `product:list:{categoryId}:{brandId}:{sort}:{page}` | String (JSON) | 600s + rand(0,120) | 列表缓存 |
| **缓存防护** | | | |
| `cache:mutex:{key}` | String | 10s | 热点数据加载互斥锁 |
| **登录限流** | | | |
| `rate:login:{ip}` | String (int) | 60s | IP 登录限流 |
| `rate:login:user:{username}` | String (int) | 900s | 用户名登录限流 |
| `rate:register:{ip}` | String (int) | 3600s | 注册限流 |
| `login:attempts:{username}` | String (int) | 900s | 失败次数 |
| `login:locked:{username}` | String | 900s | 锁定标记 |
| **分布式锁** | | | |
| `lock:order_timeout:{orderNo}` | String | 30s | 超时处理锁 |
| `task:lock:{taskName}` | String | 30s | 定时任务锁 |
| **数据看板** | | | |
| `dashboard:sales_overview` | String (JSON) | 300s | 销售概览 |
| `dashboard:product_ranking` | String (JSON) | 600s | 商品排行 |
| `dashboard:order_trend_7d` | String (JSON) | 1800s | 7天趋势 |
| `dashboard:order_trend_30d` | String (JSON) | 3600s | 30天趋势 |

### 5.2 缓存穿透防护

> **场景**：大量请求查询不存在的商品 ID，绕过 Redis 直接打到 DB

**双保险方案**：

```
请求:
  ① 布隆过滤器检查 key 是否可能存在
     ├─ 不存在 → 直接返回 null（不查 Redis，不查 DB）
     └─ 可能存在 → 进入步骤②

  ② Redis 缓存中查询
     ├─ 命中 → 返回
     └─ 未命中 → 进入步骤③

  ③ 查询 DB
     ├─ 存在 → SET product:{id} 3600+rand → 返回
     └─ 不存在 → SET product:{id}:null 1 EX 60 → 返回 null
                  ⚠️ TTL 60s 短于正常缓存，避免占用内存
```

**布隆过滤器初始化**：

```java
@PostConstruct
public void initBloomFilter() {
    BloomFilter<String> filter = BloomFilter.create(
        Funnels.stringFunnel(Charset.defaultCharset()),
        100000,  // 预计元素数量
        0.01     // 误判率 1%
    );
    List<Long> productIds = productMapper.selectAllIds();
    productIds.forEach(id -> filter.put("product:" + id));
    this.productBloomFilter = filter;
}
```

### 5.3 缓存击穿防护

> **场景**：热点商品缓存过期瞬间，大量请求同时打到 DB

**Redisson 分布式锁互斥加载**：

```java
public Product getProduct(Long id) {
    String cacheKey = "product:" + id;

    // ① 先读缓存
    Product cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return cached;

    // ② 加互斥锁
    RLock lock = redisson.getLock("cache:mutex:" + cacheKey);
    try {
        if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
            // ③ 双重检查（其他线程可能已写入）
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return cached;

            // ④ 查 DB 并回写缓存
            Product db = productMapper.selectById(id);
            if (db != null) {
                redisTemplate.opsForValue().set(
                    cacheKey, JSON.toJSONString(db),
                    3600 + ThreadLocalRandom.current().nextInt(600),
                    TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(cacheKey + ":null", "1", 60, TimeUnit.SECONDS);
            }
            return db;
        }
    } finally {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
    // ⑤ 获取锁失败 → 短暂 sleep 后重试读缓存
    Thread.sleep(50);
    return getProduct(id);
}
```

### 5.4 缓存雪崩防护

| 策略 | 实现 |
|------|------|
| **TTL 随机化** | 基础值 + `random(0, 10%基础值)`，避免大量 Key 同时过期 |
| **熔断降级** | Resilience4j CircuitBreaker，Redis 不可用时快速失败 |
| **限流兜底** | 令牌桶限流保护 DB |
| **多级缓存** | 考虑未来引入本地缓存 Caffeine 作为二级缓存 |

### 5.5 Cache-Aside 更新策略

```
读取路径：
  读缓存 → 命中返回 / 未命中 → 读DB → 写缓存 → 返回

写入路径：
  写DB → 删除缓存（而非更新）
  原因：更新缓存可能导致缓存与DB不一致（并发写入时）
```

| 操作 | 缓存动作 |
|------|----------|
| 创建商品 | 不写缓存（懒加载，等首次查询） |
| 更新商品 | **删除** `product:{id}` + 相关列表缓存 |
| 商品下架/删除 | **删除** 商品缓存 + 列表缓存 |
| 库存变化 | **删除** 对应商品缓存 |

---

## 六、消息队列设计

### 6.1 Topic 规划

| Topic | 类型 | 消费者 | 说明 |
|-------|------|--------|------|
| `flash-sale-order` | 普通消息 | FlashSaleOrderConsumer | 秒杀下单异步落单 |
| `order-timeout` | **延迟消息** (15min) | OrderTimeoutConsumer | 订单超时检查与取消 |

### 6.2 消息体设计

**flash-sale-order**：

```json
{
  "orderNo": "FS2024010112000012345",
  "userId": 10001,
  "itemId": 500,
  "flashPrice": "99.00",
  "quantity": 1,
  "requestId": "req_abc123",
  "timestamp": 1700000000000
}
```

**order-timeout**：

```json
{
  "orderNo": "FS2024010112000012345",
  "userId": 10001,
  "itemId": 500,
  "createdAt": 1700000000000,
  "eventType": "ORDER_TIMEOUT_CHECK"
}
```

### 6.3 消费幂等性保障

```
双重保障机制:

第一层 (快速): Redis SETNX
  SET flash:dedup:order:{orderNo} 1 NX EX 3600
  → 返回 false 表示已处理，直接 ACK

第二层 (可靠): DB 唯一约束
  INSERT INTO mq_deduplication (message_key, message_type) VALUES (?, ?)
  → DuplicateKeyException → 已处理，直接 ACK
  → 插入成功 → 首次处理，继续消费逻辑
```

### 6.4 消费失败处理

```
MQ 消费失败:
  第1次 → 重试 (10s 后)
  第2次 → 重试 (30s 后)
  第3次 → 重试 (1min 后)

全部失败 → 进入死信队列 (DLQ)
  → 监控告警
  → 人工排查后重新投递或补偿
```

### 6.5 消息堆积处理

| 策略 | 说明 |
|------|------|
| **消费并行度** | `consumeThreadMax = 20`，根据压测调整 |
| **批量消费** | 每次拉取 `maxMessageBatchSize = 32` 条 |
| **监控告警** | 消费延迟 > 1s 触发告警 |

---

## 七、安全设计

### 7.1 认证安全

| 策略 | 实现 |
|------|------|
| **密码存储** | BCrypt `cost=12` |
| **传输加密** | SHA-256（前端）+ BCrypt（后端），全程无明文 |
| **JWT** | HMAC-SHA256，密钥环境变量 `JWT_SECRET` |
| **有效期** | Access Token 2h，可选 Refresh Token 7d |
| **存储** | localStorage (V1)，通过 XSS 防护降低风险 |

### 7.2 授权安全

```java
// 方法级注解
@PreAuthorize("hasRole('ADMIN')")
public Result<Void> deleteProduct(Long id) { ... }

// 自定义注解
@RequireRole(min = Role.OPERATOR)
public Result<ProductVO> updateProduct(ProductUpdateReq req) { ... }

// URL 级控制
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "ADMIN")
    .requestMatchers("/api/flash-sale/**").authenticated()
    .anyRequest().permitAll()
);
```

### 7.3 防攻击策略

| 威胁 | 防护 |
|------|------|
| **XSS** | 富文本输入使用 OWASP Java HTML Sanitizer 过滤危险标签 |
| **SQL 注入** | MyBatis-Plus `#{}` 参数化查询，100% 预编译 |
| **暴力破解** | 登录/注册双维度限流（IP + 用户名） |
| **文件上传** | 魔数校验 + 扩展名白名单 + 大小限制 |
| **CSRF** | 前后端分离 + JWT Bearer Token，天然免疫 |
| **敏感信息泄露** | password 字段 `@JsonIgnore`，日志脱敏 |

### 7.4 全局限流方案

```
令牌桶限流（Interceptor 实现）
  默认: 100 req/s/IP

细化策略:
  登录:   IP 60s/10次 + 用户名 15min/5次失败
  注册:   IP 1h/3次
  秒杀:   用户+商品 1s/1次
  管理端: 默认规则
```

---

## 八、异常处理设计

### 8.1 后端异常体系

```
RuntimeException
  ├── BusinessException(code, message)         ← 业务异常 (400)
  │     ├── UserExistsException
  │     ├── PasswordErrorException
  │     ├── StockNotEnoughException
  │     ├── FlashSaleNotStartedException
  │     ├── FlashSaleEndedException
  │     ├── FlashSaleLimitException
  │     ├── OrderTimeoutException
  │     └── DuplicateOrderException
  ├── RateLimitException                       ← 限流异常 (429)
  └── AuthException                            ← 认证异常 (401)
```

### 8.2 GlobalExceptionHandler 映射表

| 异常类型 | HTTP | Code | Message |
|----------|------|------|---------|
| `BusinessException` | 400 | 业务 code | 业务提示信息 |
| `RateLimitException` | 429 | RATE_LIMIT | "操作太频繁，请稍后再试" |
| `AuthException` | 401 | UNAUTHORIZED | "登录已过期，请重新登录" |
| `AccessDeniedException` | 403 | FORBIDDEN | "没有权限访问" |
| `MethodArgumentNotValid` | 422 | VALIDATION_ERROR | 字段级错误列表 |
| `HttpMessageNotReadable` | 400 | PARAM_ERROR | "请求参数格式错误" |
| 未捕获异常 | 500 | SYSTEM_ERROR | "系统繁忙，请稍后重试" |

### 8.3 统一返回格式

```json
// 成功
{ "code": 200, "message": "success", "data": {}, "timestamp": 1700000000000 }

// 业务错误
{ "code": 40001, "message": "库存不足", "data": null, "timestamp": 1700000000001 }

// 校验错误 (字段级)
{ "code": 422, "message": "VALIDATION_ERROR", "data": {
  "fields": [
    { "field": "username", "message": "用户名长度应在 2-32 字符之间" }
  ]
}, "timestamp": 1700000000002 }

// 分页返回
{ "code": 200, "message": "success", "data": {
  "records": [], "total": 100, "page": 1, "pageSize": 10, "totalPages": 10
}}
```

### 8.4 前端错误边界体系

```
<App>
  └── <ErrorBoundary>                    ← 全局兜底: React 渲染崩溃
      └── <BrowserRouter>
          └── <AuthProvider>             ← 401 统一跳登录
              └── <Layout>
                  └── <ErrorBoundary>    ← 内容区隔离
                      └── <Outlet />
```

**Axios 响应拦截器处理表**：

| HTTP Status | 处理方式 |
|-------------|----------|
| 200 | 返回 `response.data` |
| 400 | Toast 展示 `data.message` |
| 401 | 清除 Token → 跳转 `/login?redirect=当前路径` |
| 403 | Toast "没有权限访问" |
| 422 | 返回字段错误，表单组件映射显示 |
| 429 | Toast "操作太频繁" |
| 500 | Toast "系统繁忙，请稍后重试" |
| Network Error | Toast "网络连接失败" + 重试按钮 |
| Timeout | "请求超时，请检查网络" |

---

## 九、部署架构设计

### 9.1 Docker Compose 编排

```
                     ┌──────────────────────────────┐
                     │       nginx (frontend)        │
                     │        port: 80               │
                     │        mem: 128m              │
                     └──────────────┬───────────────┘
                                    │ reverse proxy /api → backend:8080
                                    ▼
                     ┌──────────────────────────────┐
                     │       backend (Spring Boot)    │
                     │        port: 8080             │
                     │        mem: 512m              │
                     │        depends_on: mysql      │
                     │                    redis      │
                     │                    rocketmq   │
                     │        stop_grace: 35s        │
                     └──┬──────────┬──────────┬──────┘
                        │          │          │
              ┌─────────┘          │          └─────────┐
              ▼                    ▼                    ▼
   ┌─────────────────┐ ┌────────────────┐ ┌──────────────────────┐
   │  mysql:8.0      │ │  redis:7-alpine│ │  rocketmq:5.1.4      │
   │  port: 3306     │ │  port: 6379    │ │  namesrv: 9876       │
   │  mem: 1g        │ │  mem: 512m     │ │  broker: 10911       │
   │  healthcheck    │ │  appendonly yes│ │  mem: 256m/512m      │
   │  volume: mysql  │ │  volume: redis │ │                      │
   └─────────────────┘ └────────────────┘ └──────────────────────┘
```

### 9.2 多环境配置差异

| 配置项 | dev | prod |
|--------|-----|------|
| **数据库连接池 max** | 10 | 20 |
| **日志级别** | DEBUG | INFO |
| **Flyway 迁移** | 启动时自动 | 手动执行 |
| **限流策略** | 100 req/s | 根据压测调整 |
| **七牛云 Bucket** | `mall-dev` | `mall-prod` |
| **SQL 日志** | 打印 | 关闭 |

### 9.3 优雅关闭

```yaml
# Spring Boot
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

# Docker Compose
backend:
  stop_grace_period: 35s  # 比 Spring 多 5s 缓冲
```

**关闭流程**：
1. Docker 发送 SIGTERM
2. Spring 停止接收新请求
3. 处理完进行中的请求（最多 30s）
4. 释放数据库连接池
5. 容器退出

### 9.4 CI/CD 建议流程

```
  代码提交
    │
    ▼
  ┌─────────────────┐
  │ 1. 编译构建        │
  │    frontend: vite build
  │    backend: mvn package -DskipTests
  ├─────────────────┤
  │ 2. 单元测试        │
  │    backend: mvn test
  ├─────────────────┤    ┌──────────────────────────────┐
  │ 3. Docker 构建     │    │ dev 环境:                    │
  │    docker build   │───►│ 自动部署 → 冒烟测试 → 通知     │
  ├─────────────────┤    └──────────────────────────────┘
  │ 4. 集成测试 (可选)  │    ┌──────────────────────────────┐
  │    docker-compose │    │ prod 环境:                    │
  │    JMeter 压测    │───►│ 手动审批 → 蓝绿部署 → 监控确认   │
  └─────────────────┘    └──────────────────────────────┘
```

---

## 十、性能设计

### 10.1 数据库性能

| 策略 | 参数 |
|------|------|
| **连接池** | HikariCP, max=20(prod), minIdle=5 |
| **连接超时** | 30s |
| **空闲回收** | 10min |
| **最大生命周期** | 30min |
| **事务隔离** | READ_COMMITTED |
| **慢查询阈值** | 200ms（dev）, 500ms（prod） |

### 10.2 秒杀场景性能预估

| 指标 | 目标值 | 说明 |
|------|--------|------|
| **秒杀接口 QPS** | ≥ 5000 | Redis DECR 单机可达 10w+ QPS |
| **接口响应时间** | < 50ms | 仅 Redis + 发 MQ，无 DB 操作 |
| **MQ 消费 TPS** | ≥ 2000 | 含事务内 DB 写入 |
| **前端轮询** | 3s 间隔 | 30s 超时，降低服务器压力 |
| **JMeter 并发** | 500 用户 | 200 库存，验证无超卖 |

### 10.3 缓存性能优化

```
热点数据预热:
  - 系统启动时加载商品布隆过滤器
  - 秒杀活动开始前 5 分钟预热 Redis 库存 + 商品信息
  - 定时任务每 5 分钟刷新数据看板缓存

内存估算:
  - 商品缓存: ~2KB/条 × 10,000 商品 ≈ 20MB
  - 列表缓存: ~5KB/页 × 50 页 ≈ 250KB
  - 秒杀数据: ~1KB/活动 × 100 活动 ≈ 100KB
  - 总计: ~50MB (含 Redis 开销)
```

### 10.4 压测方案 (JMeter)

```
脚本: jmeter/flash-sale.jmx

场景 1 — 秒杀正常流量:
  线程数: 200 并发
  库存: 100
  预期: 最终订单数 = 100，无超卖

场景 2 — 同用户重复秒杀:
  线程数: 50，同一用户
  预期: 每人最多 1 单（限购生效）

场景 3 — 库存一致性:
  秒杀结束后:
    Redis flash_stock:{itemId} 值
    = DB flash_sale_item.flash_stock
    = DB 已创建订单数
```

---

## 十一、附录

### 附录 A：技术选型对照表

| 技术需求 | 候选方案 | 最终选择 | 取舍理由 |
|----------|----------|----------|----------|
| 前端框架 | React / Vue / Svelte | React 18 | 生态最成熟，shadcn/ui 专为 React 设计 |
| 构建工具 | Vite / Webpack / Rsbuild | Vite 6 | 用户指定，HMR 极速 |
| UI 组件库 | Ant Design / shadcn/ui / MUI | shadcn/ui | 用户指定，基于 Radix UI，高度可定制 |
| ORM | MyBatis-Plus / JPA / MyBatis | MyBatis-Plus | 用户指定，复杂 SQL 灵活 |
| 消息队列 | RocketMQ / RabbitMQ / Kafka | RocketMQ 5 | 用户指定，延迟消息能力原生支持 |
| 缓存客户端 | Jedis / Lettuce / Redisson | Redisson | 分布式锁 API 友好 |
| 数据库迁移 | Flyway / Liquibase | Flyway | 用户指定，SQL 文件简单直接 |
| 搜索方案 | ES / MySQL FULLTEXT / Solr | MySQL FULLTEXT | V1 阶段，MySQL 够用，预留 ES 升级 |
| API 文档 | SpringDoc / SpringFox | SpringDoc OpenAPI 2 | Spring Boot 3 兼容 |

### 附录 B：完整目录结构

```
full-stack/
├── docker-compose.yml
├── Technology-design.md            ← 本文档
├── DEMAND.md
├── PLAN.md
├── nginx/
│   └── default.conf
├── jmeter/
│   └── flash-sale.jmx
│
├── frontend/
│   ├── vite.config.ts
│   ├── package.json
│   ├── tsconfig.json
│   ├── Dockerfile
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── App.css                   # TailwindCSS 入口
│       ├── api/
│       │   ├── client.ts            # Axios 拦截器
│       │   ├── auth.ts
│       │   ├── product.ts
│       │   ├── cart.ts
│       │   ├── order.ts
│       │   ├── flash-sale.ts
│       │   ├── address.ts
│       │   └── upload.ts
│       ├── stores/
│       │   ├── authStore.ts         # Zustand 认证
│       │   └── cartStore.ts         # Zustand 购物车
│       ├── hooks/
│       │   ├── useRequest.ts
│       │   ├── useAuth.ts
│       │   ├── useCountdown.ts
│       │   └── useFlashSale.ts
│       ├── components/
│       │   ├── ui/                  # shadcn/ui 组件
│       │   ├── ErrorBoundary.tsx
│       │   ├── Layout.tsx
│       │   ├── AuthGuard.tsx
│       │   ├── AdminGuard.tsx
│       │   ├── AdminLayout.tsx
│       │   ├── Loading.tsx
│       │   ├── Empty.tsx
│       │   └── SkuSelector.tsx
│       ├── pages/
│       │   ├── Home/
│       │   ├── Login/
│       │   ├── Register/
│       │   ├── NotFound.tsx
│       │   ├── Forbidden.tsx
│       │   ├── ProductList/
│       │   ├── ProductDetail/
│       │   ├── Cart/
│       │   ├── Checkout/
│       │   ├── OrderList/
│       │   ├── OrderDetail/
│       │   ├── UserCenter/
│       │   ├── FlashSale/
│       │   │   ├── FlashSaleList.tsx
│       │   │   └── FlashSaleDetail.tsx
│       │   └── Admin/
│       │       ├── AdminProducts.tsx
│       │       ├── AdminOrders.tsx
│       │       ├── AdminFlashSales.tsx
│       │       ├── AdminUsers.tsx
│       │       ├── Dashboard.tsx
│       │       └── AdminOperationLogs.tsx
│       ├── router/
│       │   └── index.tsx
│       ├── types/
│       │   ├── api.ts
│       │   ├── auth.ts
│       │   ├── product.ts
│       │   ├── order.ts
│       │   └── flash-sale.ts
│       └── utils/
│           ├── format.ts
│           └── validators.ts
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/example/mall/
│       │   ├── MallApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── MyBatisPlusConfig.java
│       │   │   ├── RedisConfig.java
│       │   │   ├── RocketMQConfig.java
│       │   │   └── ThreadPoolConfig.java
│       │   ├── common/
│       │   │   ├── Result.java
│       │   │   ├── PageResult.java
│       │   │   ├── BusinessException.java
│       │   │   ├── BizCode.java
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── auth/
│       │   │   ├── JwtProvider.java
│       │   │   ├── JwtAuthFilter.java
│       │   │   ├── UserDetailsServiceImpl.java
│       │   │   └── annotation/
│       │   │       └── RequireRole.java
│       │   ├── module/
│       │   │   ├── user/
│       │   │   │   ├── controller/UserAuthController.java
│       │   │   │   ├── controller/UserController.java
│       │   │   │   ├── controller/AddressController.java
│       │   │   │   ├── service/UserService.java
│       │   │   │   ├── entity/User.java
│       │   │   │   ├── entity/UserAddress.java
│       │   │   │   └── mapper/UserMapper.java
│       │   │   ├── product/
│       │   │   │   ├── controller/ProductController.java
│       │   │   │   ├── controller/CategoryController.java
│       │   │   │   ├── controller/BrandController.java
│       │   │   │   ├── controller/AdminProductController.java
│       │   │   │   ├── service/ProductService.java
│       │   │   │   ├── entity/Product.java
│       │   │   │   ├── entity/ProductSku.java
│       │   │   │   ├── entity/Category.java
│       │   │   │   ├── entity/Brand.java
│       │   │   │   └── mapper/ProductMapper.java
│       │   │   ├── cart/
│       │   │   │   ├── controller/CartController.java
│       │   │   │   ├── service/CartService.java
│       │   │   │   ├── entity/CartItem.java
│       │   │   │   └── mapper/CartMapper.java
│       │   │   ├── order/
│       │   │   │   ├── controller/OrderController.java
│       │   │   │   ├── controller/AdminOrderController.java
│       │   │   │   ├── service/OrderService.java
│       │   │   │   ├── entity/Orders.java
│       │   │   │   ├── entity/OrderItem.java
│       │   │   │   └── mapper/OrderMapper.java
│       │   │   ├── flashsale/
│       │   │   │   ├── controller/FlashSaleController.java
│       │   │   │   ├── controller/AdminFlashSaleController.java
│       │   │   │   ├── consumer/FlashSaleOrderConsumer.java
│       │   │   │   ├── consumer/OrderTimeoutConsumer.java
│       │   │   │   ├── service/FlashSaleService.java
│       │   │   │   ├── entity/FlashSaleEvent.java
│       │   │   │   ├── entity/FlashSaleItem.java
│       │   │   │   └── mapper/FlashSaleMapper.java
│       │   │   └── operation/
│       │   │       ├── OpLog.java          # 注解
│       │   │       ├── OpLogAspect.java    # AOP 切面
│       │   │       ├── controller/OperationLogController.java
│       │   │       └── entity/OperationLog.java
│       │   ├── interceptor/
│       │   │   └── RateLimitInterceptor.java
│       │   └── scheduled/
│       │       ├── FlashPreheatTask.java
│       │       └── DashboardCacheTask.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           ├── application-prod.yml
│           └── db/migration/
│               ├── V1__init.sql
│               ├── V2__add_address_table.sql
│               ├── V3__add_refund_fields.sql
│               └── V4__add_mq_dedup_table.sql
```

### 附录 C：术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| SPU | Standard Product Unit | 商品标准化单元，如 iPhone 15 |
| SKU | Stock Keeping Unit | 库存量单位，如 iPhone 15 银色 128G |
| 秒杀 | Flash Sale | 限时限量的特价促销活动 |
| 削峰 | Peak Clipping | 用 MQ 队列平滑处理瞬时高并发请求 |
| 乐观锁 | Optimistic Lock | 基于 version 字段的并发控制，先更新后检查 |
| 幂等 | Idempotent | 同一操作执行多次的结果与执行一次相同 |
| 熔断 | Circuit Breaker | 故障快速失败机制，防止级联故障 |
| 穿透 | Cache Penetration | 查询不存在的数据绕过缓存直接打 DB |
| 击穿 | Cache Breakdown | 热点 Key 过期瞬间大量请求打到 DB |
| 雪崩 | Cache Avalanche | 大量 Key 同时过期导致缓存层整体失效 |
| 死信 | Dead Letter | MQ 消费多次失败后进入的专用队列 |

---

> **文档维护说明**：本文档基于 `DEMAND.md` v1.0 编写，两者应保持同步更新。当需求变更时，优先更新 DEMAND.md，再同步本设计文档的技术方案部分。
