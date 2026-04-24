# 全栈商城系统 — AI 并行开发任务计划

> **设计依据**: [Technology-design.md](./Technology-design.md) | [DEMAND.md](./DEMAND.md)
> **任务粒度**: 每个任务约 50-300 行代码，适合单个 AI Agent 一次性完成
> **并行策略**: 同组内所有任务无依赖，可同时启动多个 Agent 并行执行

---

## 任务依赖总览

```
Phase 1 (基础) ──── 全部完成后方可进入 Phase 2+
    │
    ├─► Phase 2 (认证) ──── 完成后解锁 Phase 5-9
    │       │
    │       └─► Phase 3 (商品后端+分类/品牌)
    │       └─► Phase 4 (商品前端) ── 依赖 Phase 3
    │       └─► Phase 5 (地址+购物车) ── 依赖 Phase 2
    │       └─► Phase 6 (订单) ── 依赖 Phase 5
    │       └─► Phase 7 (秒杀核心) ── 依赖 Phase 2+3
    │       └─► Phase 8 (管理后台) ── 依赖 Phase 2+3+6+7
    │
    └─► Phase 9 (部署+测试) ── 依赖全部
```

---

## Phase 1: 工程初始化（基础层）

> **串行**: 需按编号顺序执行，后续所有 Phase 依赖此 Phase 完成
> **策略**: T1.1 → T1.2 + T1.3 并行 → T1.4 等待数据库就绪 → T1.5 + T1.6 并行

### T1.1 — Docker Compose 基础设施搭建

**依赖**: 无
**产出文件**:
- `docker-compose.yml`（MySQL 8.0 + Redis 7 + RocketMQ NameServer + Broker + Nginx + backend + frontend）
- `.env` 或 `.env.example`（环境变量模板）

**任务说明**:
1. 编写 `docker-compose.yml`，包含 7 个服务：mysql、redis、rocketmq-namesrv、rocketmq-broker、backend、frontend、nginx
2. 配置健康检查（mysql 用 mysqladmin ping）、内存限制、端口映射、volumes
3. 配置 backend 的 `depends_on` 条件等待（mysql health + redis started + rocketmq started）
4. 环境变量引用 `${MYSQL_ROOT_PASSWORD}`, `${JWT_SECRET}`, `${QINIU_ACCESS_KEY}`, `${QINIU_SECRET_KEY}`, `${SPRING_PROFILES_ACTIVE}`
5. RocketMQ broker 需配置 `NAMESRV_ADDR` 环境变量指向 namesrv

**参考**: Technology-design.md §9.1, DEMAND.md §1.3

---

### T1.2 — 后端项目脚手架

**依赖**: T1.1（仅需知道端口/服务名，可同时启动）
**产出目录**: `backend/`
**产出文件**:
- `backend/pom.xml`（Spring Boot 3.2 + JDK 21 + MyBatis-Plus 3.5 + jjwt 0.12 + Redisson + RocketMQ Starter + SpringDoc + Flyway + HikariCP + OWASP Sanitizer）
- `backend/src/main/java/com/example/mall/MallApplication.java`
- `backend/src/main/resources/application.yml`（公共配置：HikariCP、MyBatis-Plus、日志、优雅关闭）
- `backend/src/main/resources/application-dev.yml`（dev 环境：debug 日志、Flyway 自动迁移、宽松限流）
- `backend/src/main/resources/application-prod.yml`（prod 环境：info 日志、Flyway 手动迁移）

**任务说明**:
1. 初始化 Spring Boot 3.2 Maven 项目
2. 配置所有依赖（不遗漏 Redisson、RocketMQ、Flyway）
3. 三环境配置文件：application.yml（公共）+ dev + prod，按 DEMAND.md §1.4 的差异表配置
4. HikariCP 连接池参数按 Technology-design.md §10.1 配置
5. 优雅关闭配置：`server.shutdown=graceful` + `timeout-per-shutdown-phase=30s`

**参考**: Technology-design.md §1.2, DEMAND.md §1.2/§1.4/§1.5/§1.6

---

### T1.3 — 前端项目脚手架

**依赖**: T1.1（可同时启动）
**产出目录**: `frontend/`
**产出文件**:
- `frontend/package.json`（React 18 + TypeScript 5 + Vite 6 + TailwindCSS 4 + React Router v7 + Zustand + Axios + React Hook Form + Zod + shadcn/ui 初始化）
- `frontend/vite.config.ts`（代理 `/api` 到 `http://localhost:8080`）
- `frontend/tsconfig.json`
- `frontend/tailwind.config.ts`
- `frontend/src/main.tsx`
- `frontend/src/App.css`（TailwindCSS 指令）

**任务说明**:
1. 用 Vite 6 创建 React + TypeScript 项目
2. 安装所有依赖（参考 Technology-design.md §1.2 前端技术栈表）
3. 初始化 shadcn/ui（`npx shadcn-ui@latest init`）
4. 配置 Vite 代理，`/api` → `http://localhost:8080`
5. 配置 TailwindCSS 4 + CSS 入口

**参考**: Technology-design.md §2.3 前端架构, DEMAND.md §1.1

---

### T1.4 — Flyway 数据库迁移（V1 初始建表）

**依赖**: T1.1（需要 MySQL 运行），T1.2（需要 Flyway 依赖已配置）
**产出文件**: `backend/src/main/resources/db/migration/V1__init.sql`

**任务说明**:
1. 编写完整的建表 SQL（13 张表），按 DEMAND.md §9 的 DDL 精确复制
2. 包含所有索引（UNIQUE KEY、INDEX、FULLTEXT INDEX）
3. ngram parser 的 FULLTEXT 索引语句：`FULLTEXT INDEX ft_search (name, keywords) WITH PARSER ngram`
4. 所有表使用 InnoDB + utf8mb4
5. 确保 Flyway 文件命名规范：`V1__init.sql`（双下划线）

**表清单**:
| # | 表名 | 用途 |
|---|------|------|
| 1 | `user` | 用户账户 |
| 2 | `user_address` | 收货地址 |
| 3 | `category` | 商品分类 |
| 4 | `brand` | 品牌 |
| 5 | `product` | 商品 SPU |
| 6 | `product_sku` | 商品 SKU |
| 7 | `flash_sale_event` | 秒杀活动 |
| 8 | `flash_sale_item` | 秒杀商品关联 |
| 9 | `orders` | 订单 |
| 10 | `order_item` | 订单商品快照 |
| 11 | `cart_item` | 购物车 |
| 12 | `mq_deduplication` | MQ 幂等去重 |
| 13 | `operation_log` | 操作日志 |

**参考**: DEMAND.md §9, Technology-design.md §4.2/§4.4

---

### T1.5 — 后端公共基础设施

**依赖**: T1.2（项目结构已创建）
**产出目录**: `backend/src/main/java/com/example/mall/common/`
**产出文件**:
- `common/Result.java` — 统一返回体（4种格式：成功/业务错误/校验错误/分页）
- `common/PageResult.java` — 分页数据结构
- `common/BizCode.java` — 业务错误码枚举
- `common/BusinessException.java` — 业务异常基类
- `common/GlobalExceptionHandler.java` — 全局异常处理器（@RestControllerAdvice）
- `config/CorsConfig.java` — CORS 跨域配置
- `config/MyBatisPlusConfig.java` — MyBatis-Plus 配置（分页插件 + 乐观锁插件）

**任务说明**:
1. Result 类字段：`code`, `message`, `data`, `timestamp`，提供静态工厂方法 `success()`, `error()`, `page()`
2. GlobalExceptionHandler 处理以下异常映射（按 Technology-design.md §8.2 表）：
   - `BusinessException` → 400
   - `RateLimitException` → 429
   - `AuthException` → 401
   - `AccessDeniedException` → 403
   - `MethodArgumentNotValidException` → 422（字段级错误列表）
   - 未捕获异常 → 500
3. BizCode 按模块划分错误码段：用户 1xxxx、商品 2xxxx、订单 3xxxx、秒杀 4xxxx
4. CorsConfig 允许前端开发端口跨域，允许 Authorization header

**参考**: Technology-design.md §8.1-§8.3, DEMAND.md §7

---

### T1.6 — 前端公共基础设施

**依赖**: T1.3（项目结构已创建）
**产出目录**: `frontend/src/`
**产出文件**:
- `api/client.ts` — Axios 实例 + 请求拦截器（attach token）+ 响应拦截器（处理 401/403/422/429/500，按 Technology-design.md §8.4 表）
- `components/ErrorBoundary.tsx` — React Error Boundary 全局兜底
- `components/Layout.tsx` — 公共布局（Header + Outlet + Footer）
- `components/Loading.tsx` — 通用加载组件
- `components/Empty.tsx` — 通用空状态组件
- `router/index.tsx` — React Router v7 路由配置（所有路由占位，后续 Phase 填充）
- `types/api.ts` — API 通用类型定义（`ApiResponse<T>`, `PageResult<T>`, `FieldError`）

**任务说明**:
1. Axios 响应拦截器按 HTTP 状态码分类处理，401 清除 token 跳转登录
2. ErrorBoundary 捕获 React 渲染崩溃，显示"页面出错了"+ 重新加载按钮
3. Layout 包含 Header（导航+搜索入口占位）+ `<Outlet />` + Footer
4. 路由表包含所有页面占位（import 用 lazy + Suspense），AuthGuard 包裹需登录路由
5. 路由路径按 Technology-design.md §2.3 前端架构的路由树

**参考**: Technology-design.md §2.3 前端架构 + §8.4 前端错误边界

---

## Phase 2: 用户认证模块

> **解锁条件**: Phase 1 全部完成
> **并行组 P2-A**: T2.1 + T2.2 + T2.3 + T2.4 可以全部并行（都是认证模块的不同层，接口约定已知）

### T2.1 — 后端 JWT + Spring Security 配置

**依赖**: T1.2, T1.5
**产出目录**: `backend/.../auth/`, `backend/.../config/`
**产出文件**:
- `auth/JwtProvider.java` — JWT 签发/校验工具（HMAC-SHA256，2h 有效期，payload 含 sub/username/role）
- `auth/JwtAuthFilter.java` — OncePerRequestFilter，从 Authorization header 解析 token，注入 SecurityContext
- `auth/UserDetailsServiceImpl.java` — 实现 UserDetailsService，从 DB 加载用户
- `config/SecurityConfig.java` — Spring Security 6 配置（禁用 CSRF、无状态 Session、URL 权限规则、JWT Filter 注册）

**任务说明**:
1. JWT Payload：`{ sub, username, role, iat, exp }`，密钥从 `JWT_SECRET` 环境变量读取
2. JwtAuthFilter 跳过 `/api/auth/**` 路径，其他需要认证
3. SecurityConfig URL 规则：`/api/admin/**` → OPERATOR/ADMIN，`/api/flash-sale/**` → authenticated，其余 permitAll
4. JWT 校验失败返回 401

**参考**: Technology-design.md §3.1.2/§7.1/§7.2, DEMAND.md §2.1.3

---

### T2.2 — 后端用户注册/登录 API

**依赖**: T1.4（user 表）, T1.5, T2.1
**产出目录**: `backend/.../module/user/`
**产出文件**:
- `entity/User.java` — MyBatis-Plus Entity（password 字段 @JsonIgnore）
- `mapper/UserMapper.java` — MyBatis-Plus BaseMapper<User>
- `service/UserService.java` — 注册/登录/个人信息查询/密码修改 业务逻辑
- `controller/UserAuthController.java` — `POST /api/auth/register`, `POST /api/auth/login`
- `controller/UserController.java` — `GET /api/user/profile`, `PUT /api/user/profile`, `PUT /api/user/password`
- `auth/annotation/RequireRole.java` — 自定义角色注解（min 属性）

**任务说明**:
1. **注册**：校验用户名/邮箱唯一性 → BCrypt(cost=12) 加密密码 → INSERT → 返回成功
   - 异常：USER_EXISTS → 400, VALIDATION_ERROR → 422
2. **登录**：查询用户 → BCrypt 校验 → 生成 JWT → 返回 token + user 信息
   - 限流检查调用 T2.5 的 RateLimitInterceptor（可先写 TODO，后续 T2.5 补充）
   - 异常：USER_NOT_FOUND/PASSWORD_ERROR → 401, ACCOUNT_DISABLED → 403
3. **密码修改**：校验旧密码 → BCrypt 加密新密码 → UPDATE
4. **传输安全**：前端 SHA-256 → 后端 BCrypt（后端字段校验：最少 8 位，含字母+数字）
5. User 实体：`password` 字段标注 `@JsonIgnore`

**参考**: Technology-design.md §3.1.1/§3.1.2/§3.1.5, DEMAND.md §2.1

---

### T2.3 — 前端登录/注册页面

**依赖**: T1.6（路由 + Axios + 公共组件）
**产出目录**: `frontend/src/pages/Login/`, `frontend/src/pages/Register/`
**产出文件**:
- `pages/Login/index.tsx` — 登录页（React Hook Form + Zod）
- `pages/Register/index.tsx` — 注册页
- `pages/NotFound.tsx` — 404 页面
- `pages/Forbidden.tsx` — 403 页面
- `api/auth.ts` — 认证 API 函数（login, register, getProfile, updateProfile, changePassword）

**任务说明**:
1. **登录页**：用户名+密码输入框 + "登录"按钮，Zod 校验非空
   - 错误状态：用户名不存在、密码错误 Toast 提示
   - 成功后存 token 到 localStorage + 更新 authStore + 跳转首页（或 redirect 参数）
   - 密码传输前 SHA-256 哈希
2. **注册页**：用户名+密码+确认密码+邮箱，Zod 校验（密码强度、邮箱格式、用户名规则）
   - 错误状态：用户名已存在红字提示、邮箱格式错误行内提示
   - 成功后跳转登录页
3. 页面视觉使用 shadcn/ui 组件（Card, Input, Button, Label, Toast）

**参考**: Technology-design.md §3.1.1/§3.1.2 异常表, DEMAND.md §2.1.1/§2.1.2

---

### T2.4 — 前端 AuthProvider + 路由守卫

**依赖**: T1.6, T2.3
**产出文件**:
- `stores/authStore.ts` — Zustand auth store（user, token, isAuthenticated, login(), logout(), fetchProfile()）
- `hooks/useAuth.ts` — 认证 Hook（角色判断 isAdmin/isOperator）
- `components/AuthGuard.tsx` — 路由守卫组件（未登录跳 /login?redirect=当前路径）
- `components/AdminGuard.tsx` — 管理员/运营守卫（角色不足显示 Forbidden）

**任务说明**:
1. authStore 初始化时从 localStorage 读 token，若存在则调用 `/api/user/profile` 验证有效性
2. AuthGuard 检查 isAuthenticated，未登录时 `navigate('/login?redirect=' + currentPath)`
3. AdminGuard 接收 `minRole` prop，检查 `user.role >= minRole`，不通过渲染 Forbidden 页面
4. 401 响应拦截器（已在 T1.6 的 client.ts 中）清除 authStore + 跳转登录

**参考**: Technology-design.md §2.3 前端状态管理 + §3.1.3/§3.6.1

---

### T2.5 — 后端限流拦截器

**依赖**: T1.5（需要 Common 层）, T1.2（需要 RedisTemplate）
**产出目录**: `backend/.../interceptor/`
**产出文件**:
- `interceptor/RateLimitInterceptor.java` — 令牌桶全局限流 + 登录/注册双维度限流
- `common/RateLimitException.java` — 限流异常类
- `config/WebMvcConfig.java` — 注册拦截器

**任务说明**:
1. 全局限流：默认 100 req/s/IP（令牌桶算法），通过 Redis 实现
2. 登录接口限流：
   - IP 维度：`rate:login:{ip}` EX 60s，60s 内最多 10 次
   - 用户名维度：`rate:login:user:{username}` EX 900s，15min 内最多 5 次失败
   - 连续 5 次失败：`SET login:locked:{username} 1 EX 900`
3. 注册接口限流：IP 维度，`rate:register:{ip}` EX 3600s，1h 内最多 3 次
4. 秒杀接口限流（可先写占位，Phase 7 完善）：用户+商品维度 1s 间隔

**参考**: Technology-design.md §3.1.4, DEMAND.md §6.4

---

## Phase 3: 商品模块（后端）

> **解锁条件**: Phase 2 完成（需要注入用户信息 + 权限控制）
> **并行组 P3-A**: T3.1 + T3.2 + T3.5 三者互不依赖，可完全并行
> **串行**: T3.3 依赖 T3.1 + T3.2（Product 引用 Category + Brand），T3.4 依赖 T3.3

### T3.1 — 后端分类管理 CRUD

**依赖**: T1.4（category 表）, T1.5
**产出目录**: `backend/.../module/product/`
**产出文件**:
- `entity/Category.java`
- `mapper/CategoryMapper.java`
- `service/CategoryService.java`
- `controller/CategoryController.java` — `GET /api/categories`（树形结构）, `GET /api/categories/{id}`, `POST/PUT/DELETE /api/admin/categories`

**任务说明**:
1. 分类支持树形结构（`parent_id` + `level` 字段），查询时返回树形 JSON
2. `GET /api/categories` 返回启用的顶级分类及其子分类（无需认证）
3. 管理端 CRUD 接口添加 `@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")` 或 `@RequireRole`
4. 排序按 `sort_order ASC`

**参考**: DEMAND.md §2.2

---

### T3.2 — 后端品牌管理 CRUD

**依赖**: T1.4（brand 表）, T1.5
**产出文件**（同 product 模块目录）:
- `entity/Brand.java`
- `mapper/BrandMapper.java`
- `service/BrandService.java`
- `controller/BrandController.java` — `GET /api/brands`, `POST/PUT/DELETE /api/admin/brands`

**任务说明**:
1. `GET /api/brands` 返回全部启用品牌列表（无需认证）
2. 管理端 CRUD 需 OPERATOR 以上权限
3. 品牌名唯一约束，创建时处理 `uk_name` 冲突

**参考**: DEMAND.md §2.2

---

### T3.3 — 后端商品 CRUD + SKU 管理 + 搜索

**依赖**: T1.4, T3.1, T3.2
**产出文件**:
- `entity/Product.java`
- `entity/ProductSku.java`
- `mapper/ProductMapper.java` — 含自定义 SQL（FULLTEXT 搜索、FOR UPDATE 锁定）
- `mapper/ProductSkuMapper.java`
- `service/ProductService.java` — CRUD + 列表查询 + 搜索 + 缓存逻辑
- `controller/ProductController.java` — `GET /api/products`, `GET /api/products/{id}`
- `controller/AdminProductController.java` — `POST/PUT/DELETE/PATCH /api/admin/products`

**任务说明**:
1. **商品列表** `GET /api/products`：
   - 支持分页（page, pageSize, 默认 20）
   - 支持筛选（categoryId, brandId, minPrice, maxPrice）
   - 支持排序（price_asc/desc, sales_desc, new_desc）
   - 排序字段使用**白名单映射**防注入（`price_asc → ORDER BY price ASC`）
   - 关键词搜索：使用 FULLTEXT `MATCH(name, keywords) AGAINST(#{keyword} IN BOOLEAN MODE)`
   - 只返回 `status=1` 的商品
2. **商品详情** `GET /api/products/{id}`：
   - 返回商品 + SKU 列表 + 分类名 + 品牌名
   - 多规格商品：`has_sku=1`，返回 specs + skuList
3. **管理端 CRUD**：
   - 创建商品支持多规格（同时创建 SKU）+ 单规格
   - 更新商品支持更新 SKU 列表（新增/修改/标记删除）
   - 上架/下架：`PATCH /api/admin/products/{id}/status`
   - 软删除：`DELETE` → `status=0`
4. **SKU 校验**：多规格商品 sku_id 必填，单规格 SKU 可空；各 SKU 库存之和 = product.total_stock

**参考**: Technology-design.md §3.2.1/§3.2.2/§3.2.4, DEMAND.md §2.2

---

### T3.4 — 商品缓存策略实现

**依赖**: T3.3, T1.2（RedisTemplate + Redisson）
**产出文件**（修改已有 Service）:
- `service/ProductService.java` — 添加缓存逻辑
- `config/RedisConfig.java` — 完善 RedisTemplate 序列化配置
- `common/ProductBloomFilter.java` — 布隆过滤器初始化

**任务说明**:
1. **商品详情缓存** `product:{id}`：TTL 3600s + random(0,600)，首次查询后写入
2. **商品列表缓存** `product:list:{category}:{brand}:{sort}:{page}`：TTL 600s + random(0,120)
   - 仅缓存前 5 页
   - 关键词搜索不缓存（直接查 DB）
3. **缓存更新策略（Cache-Aside）**：商品更新/下架/删除 → 删除商品缓存 + 相关列表缓存
4. **布隆过滤器**：`@PostConstruct` 初始化所有商品 ID，查询前先检查（穿透防护）
5. **空值缓存**：`product:{id}:null` EX 60s
6. **互斥加载**：Redisson 分布式锁 `cache:mutex:{key}` EX 10s 防止击穿

**参考**: Technology-design.md §5.2-§5.5, §3.2.3, DEMAND.md §4

---

### T3.5 — 后端七牛云上传 Token 签发

**依赖**: T1.5
**产出文件**:
- `config/QiniuConfig.java` — 七牛云配置类（accessKey, secretKey, bucket, domain）
- `controller/UploadController.java` — `GET /api/upload/token?type=product|avatar`

**任务说明**:
1. 生成七牛云 uploadToken（有效期 10 分钟）
2. 上传策略限制：MIME 类型 `image/jpeg,image/png,image/webp`，文件大小商品 5MB/头像 2MB
3. 返回 `{ token, key, domain }`
4. 配置从环境变量读取：`QINIU_ACCESS_KEY`, `QINIU_SECRET_KEY`

**参考**: DEMAND.md §1.7, Technology-design.md §7.3 文件上传安全

---

## Phase 4: 商品模块（前端）

> **解锁条件**: Phase 2（Auth 前端）+ Phase 3（商品后端 API 可用）
> **并行组 P4-A**: T4.1 + T4.2 + T4.3 可并发（页面间无依赖，共享组件和 API 层）

### T4.1 — 前端商品列表页

**依赖**: T1.6, T3.3, T3.4
**产出文件**:
- `api/product.ts` — 商品 API 函数（getProducts, getProductDetail, searchProducts）
- `pages/ProductList/index.tsx` — 商品列表页
- `components/SkuSelector.tsx` — SKU 规格选择器（商品详情也复用）

**任务说明**:
1. **筛选区域**：分类下拉 + 品牌下拉 + 价格区间 + 排序选择
2. **搜索**：顶部搜索框输入 → `/products?keyword=xxx`
3. **商品网格**：每行 4 列卡片（主图 + 名称 + 品牌 + 价格 + 已售数）
4. **多规格价格显示**：`minPrice !== maxPrice` 时显示 "¥99.00 - ¥199.00"
5. **分页**：页码或"加载更多"按钮
6. **状态覆盖**：
   - 加载中 → 4 列 Skeleton 骨架屏
   - 空数据 → Empty + "该分类暂无商品"
   - 搜索无结果 → Empty + "没有找到相关商品"
   - 网络错误 → 错误提示 + "重新加载"按钮

**参考**: Technology-design.md §3.2.5 前端状态覆盖, DEMAND.md §2.2.1

---

### T4.2 — 前端商品详情页

**依赖**: T1.6, T3.3, T4.1（SkuSelector 组件）
**产出文件**:
- `pages/ProductDetail/index.tsx` — 商品详情页

**任务说明**:
1. **商品信息展示**：主图/多图切换 + 视频播放 + 品牌 + 标签 + 价格 + 已售
2. **SKU 选择器**（复用 T4.1 的 SkuSelector）：
   - 单规格商品（has_sku=0）：直接显示价格和库存，加购按钮可用
   - 多规格商品（has_sku=1）：渲染规格选择器，选择属性组合后匹配 SKU
   - 匹配成功 → 更新价格/库存/图片，"加入购物车"可点击
   - 匹配失败 → "该组合暂无库存"，按钮禁用
3. **参数属性表**：结构化展示 attrs 数组
4. **加购按钮**：未登录弹引导窗 + 库存不足禁用 + 多规格未选 SKU 提示
5. **状态覆盖**：加载中全页 Skeleton、商品下架 404 页面、库存为 0 按钮置灰

**参考**: Technology-design.md §3.2.4 SKU 选择器交互 + §3.2.5 状态覆盖

---

### T4.3 — 前端首页

**依赖**: T1.6, T3.1, T3.3
**产出文件**:
- `pages/Home/index.tsx` — 首页

**任务说明**:
1. **Banner 区域**：轮播图或静态 Hero（V1 简化）
2. **分类入口**：展示顶级分类（从 `/api/categories` 获取），点击跳转 `/products?categoryId=X`
3. **热门商品**：调用商品列表 API（按销量排序，取前 8 个）
4. **秒杀入口**：跳转 `/flash-sale` 的醒目入口卡片
5. **状态覆盖**：加载骨架屏、分类为空降级

**参考**: Technology-design.md §2.3 路由 + OPT.md §1.1

---

## Phase 5: 地址 + 购物车模块

> **解锁条件**: Phase 2（认证） + Phase 3（商品后端）
> **并行组 P5-A**: T5.1 + T5.2 后端可并行；T5.3 + T5.4 + T5.5 前端可并行（等待 T5.1+T5.2 完成后）

### T5.1 — 后端收货地址 CRUD

**依赖**: T1.4（user_address 表）, T2.1（需认证）
**产出文件**:
- `entity/UserAddress.java`
- `mapper/UserAddressMapper.java`
- `service/AddressService.java`
- `controller/AddressController.java` — `GET/POST/PUT/DELETE /api/addresses`, `PUT /api/addresses/{id}/default`

**任务说明**:
1. 每个用户最多 20 个地址（创建前校验）
2. 新增第一个地址时自动设为默认；设置新默认时取消原默认
3. 删除为软删除（`status=0`）
4. 查询只返回当前用户的地址（从 SecurityContext 获取 userId）

**参考**: DEMAND.md §2.6, Technology-design.md §4.2 表说明

---

### T5.2 — 后端购物车 CRUD

**依赖**: T1.4（cart_item 表）, T2.1, T3.3（需校验商品状态和库存）
**产出文件**:
- `entity/CartItem.java`
- `mapper/CartMapper.java`
- `service/CartService.java`
- `controller/CartController.java` — `GET /api/cart`, `POST /api/cart/add`, `PUT /api/cart/{id}`, `DELETE /api/cart/{id}`

**任务说明**:
1. **加购流程**（按 Technology-design.md §3.3.1 流程）：
   - 必须登录（Controller 层 @PreAuthorize）
   - 校验商品上架状态
   - 多规格商品必须有 sku_id
   - SKU 库存 > 0
   - 已有数量 >= 99 → 拒绝
   - 已存在相同 (user_id, product_id, sku_id) → UPDATE quantity，否则 INSERT
2. **购物车列表**：关联查询商品名/图片/价格/SKU 名，检查商品是否下架/库存不足
3. **修改数量**：校验库存 + 99 上限
4. **删除**：DELETE 单条或批量（`?ids=1,2,3`）

**参考**: Technology-design.md §3.3, DEMAND.md §2.3

---

### T5.3 — 前端购物车页面

**依赖**: T1.6, T2.4（AuthGuard）, T5.2
**产出文件**:
- `api/cart.ts` — 购物车 API 函数
- `stores/cartStore.ts` — Zustand 购物车状态（items, totalCount, 全选/取消全选, addItem, updateQty, removeItem）
- `pages/Cart/index.tsx` — 购物车页面

**任务说明**:
1. **状态覆盖**（按 Technology-design.md §3.3.3）：
   - 加载中 → Skeleton 列表
   - 空购物车 → Empty + "购物车是空的" + "去逛逛"按钮
   - 商品已下架 → 行标灰 + "该商品已下架"，数量选择器禁用
   - 库存不足 → "仅剩 X 件"，数量自动限制
   - 网络错误 → Toast + 失败回滚
2. **功能**：选择/取消选择商品、修改数量（减/加/输入）、删除商品（二次确认弹窗）、全选
3. **底部结算栏**：显示选中商品总数 + 总金额 + "去结算"按钮（跳转 /checkout）

**参考**: Technology-design.md §3.3.3, DEMAND.md §2.3 前端状态覆盖

---

### T5.4 — 前端收货地址管理

**依赖**: T1.6, T5.1
**产出文件**:
- `api/address.ts` — 地址 API 函数
- `pages/UserCenter/AddressManager.tsx` — 地址管理组件（嵌入 UserCenter 或独立）

**任务说明**:
1. 地址列表展示（收货人/手机/地址/默认标识）
2. 新增/编辑地址表单（省市区 + 详细地址 + 收货人 + 手机号 + 是否默认）
3. 手机号格式校验（Zod）
4. 设置默认地址：异步切换
5. 删除地址：二次确认弹窗
6. 状态覆盖：空状态、加载中、网络错误

**参考**: DEMAND.md §2.6

---

### T5.5 — 前端用户中心

**依赖**: T1.6, T2.4, T5.4
**产出文件**:
- `pages/UserCenter/index.tsx` — 用户中心主页
- `pages/UserCenter/Profile.tsx` — 个人信息编辑
- `pages/UserCenter/ChangePassword.tsx` — 密码修改

**任务说明**:
1. 个人信息：头像上传（七牛云直传）+ 昵称/手机号/邮箱编辑
2. 密码修改：旧密码 → 新密码 → 确认新密码（Zod 校验强度）
   - 旧密码错误 → Toast
   - 新密码与旧密码相同 → Toast
3. 嵌套路由：`/user/profile`, `/user/password`, `/user/addresses`

**参考**: DEMAND.md §2.7

---

## Phase 6: 订单模块

> **解锁条件**: Phase 2 + Phase 5（需要购物车+地址）
> **并行组 P6-A**: T6.1 + T6.2 后端可部分并行；T6.3 + T6.4 前端可并发（等待后端完成）

### T6.1 — 后端订单创建 + 事务

**依赖**: T1.4, T2.1, T3.3, T5.2
**产出文件**:
- `entity/Orders.java`
- `entity/OrderItem.java`
- `mapper/OrderMapper.java`
- `service/OrderService.java` — 核心下单逻辑
- `controller/OrderController.java` — `POST /api/orders`（创建订单）

**任务说明**:
1. **下单事务**（按 Technology-design.md §3.4.2 流程）：
   ```
   ① 幂等校验：Redis SETNX flash:dedup:{userId}:{requestId} EX 60
   ② 校验商品上架 + 库存充足
   ③ SELECT ... FOR UPDATE 锁定 product + product_sku 行
   ④ 扣减库存（事务内）
   ⑤ 创建订单 + 生成订单号（雪花算法或 Redis 自增）
   ⑥ INSERT order_item（快照：商品名/图片/SKU/价格）
   ⑦ 清空购物车已下单商品
   ⑧ 事务 COMMIT
   ```
2. 订单号生成：建议 `yyyyMMddHHmmss + 6位序列号`
3. 订单状态初始为 0（待支付）
4. order_item 保存的是快照（product_name, product_image, sku_name, price 均从当前数据复制）
5. 创建地址快照到 `address_snapshot` JSON 字段

**参考**: Technology-design.md §3.4.2, DEMAND.md §2.4.1/§5.2

---

### T6.2 — 后端订单管理 + 支付 + 取消

**依赖**: T6.1
**产出文件**:
- `service/OrderService.java` — 补充：列表查询、详情、支付、取消
- `controller/OrderController.java` — 补充：`GET /api/orders`, `GET /api/orders/{id}`, `POST /api/orders/{id}/pay`, `POST /api/orders/{id}/cancel`
- `controller/AdminOrderController.java` — `GET /api/admin/orders`, `PUT /api/admin/orders/{id}/ship`

**任务说明**:
1. **订单列表** `GET /api/orders`：分页、按状态筛选、按时间倒序；使用覆盖索引 `idx_user_status_time`
2. **订单详情** `GET /api/orders/{id}`：订单 + 商品快照 + 地址快照
3. **模拟支付** `POST /api/orders/{id}/pay`：
   - UPDATE orders SET status=1, paid_at=NOW(), payment_method='SIMULATED'
   - 秒杀订单无特殊处理，统一流程
4. **取消订单** `POST /api/orders/{id}/cancel`（按 Technology-design.md §3.4.3 流程）：
   - 校验订单属于当前用户
   - 校验 30 分钟内（`created_at + 30min > now`）
   - 待支付 → 归还普通商品库存 + 秒杀库存（Redis INCR + DB）
   - 已支付 → 同上 + 记录退款（refund_amount, refunded_at）
   - 状态变更为 4（已取消）
5. **管理员发货** `PUT /api/admin/orders/{id}/ship`：UPDATE status=2, shipped_at=NOW()

**参考**: Technology-design.md §3.4.1 状态机/§3.4.3/§3.4.4, DEMAND.md §2.4.2/§2.4.3/§2.8

---

### T6.3 — 前端结算页

**依赖**: T1.6, T5.3, T5.4, T6.1
**产出文件**:
- `api/order.ts` — 订单 API 函数
- `pages/Checkout/index.tsx` — 结算确认页

**任务说明**:
1. 展示选中商品清单 + 单价 + 数量 + 小计 + 总金额
2. 选择/确认收货地址（若无地址提示添加）
3. "提交订单"按钮（防重复点击：点击后 loading 禁用）
4. 下单成功 → 跳转支付页；下单失败 → Toast 提示具体原因
5. 异常：库存不足定位商品、商品下架提示、重复提交幂等处理

**参考**: DEMAND.md §2.4.1

---

### T6.4 — 前端订单列表 + 详情 + 支付页

**依赖**: T1.6, T6.2
**产出文件**:
- `pages/OrderList/index.tsx` — 订单列表
- `pages/OrderDetail/index.tsx` — 订单详情
- `pages/Payment/index.tsx` — 支付页（或嵌入 OrderDetail）

**任务说明**:
1. **订单列表**：分页 + 状态 Tab 筛选（全部/待支付/已支付/已发货/已完成/已取消）
   - 每个订单卡片：订单号 + 商品缩略 + 金额 + 状态 + 时间
   - 状态覆盖：空状态、加载中、网络错误
2. **订单详情**：商品清单 + 收货地址 + 金额明细 + 订单时间线 + 状态操作按钮
   - 待支付 → "去支付"按钮 + "取消订单"按钮（30 分钟内）
   - 已发货 → "确认收货"按钮
3. **支付页**：展示订单金额 + 倒计时（秒杀 15 分钟）+ "立即支付"按钮 → 调用模拟支付
4. **确认收货**：`PUT /api/orders/{id}/complete`

**参考**: DEMAND.md §2.4 前端状态覆盖 + §2.4.2

---

## Phase 7: 秒杀核心

> **解锁条件**: Phase 2（认证+限流）+ Phase 3（商品）+ Phase 6（订单模型）
> **注意**: 这是系统最核心的模块，依赖关系复杂
> **串行**: T7.1（管理端 CRUD）→ T7.2 + T7.3 + T7.4 + T7.5 可部分并行 → T7.6 + T7.7（前端）

### T7.1 — 后端秒杀管理 CRUD + 活动自动启停

**依赖**: T1.4, T2.1, T3.3
**产出文件**:
- `entity/FlashSaleEvent.java`
- `entity/FlashSaleItem.java`
- `mapper/FlashSaleMapper.java`
- `service/FlashSaleService.java` — 活动 CRUD + 商品关联
- `controller/AdminFlashSaleController.java` — `POST/PUT/DELETE /api/admin/flash-sales`, `POST /api/admin/flash-sales/{eventId}/items`, `PUT /api/admin/flash-sales/{eventId}/status`

**任务说明**:
1. 活动 CRUD：名称、开始时间、结束时间、状态
2. 关联商品：选择商品 → 设置秒杀价、秒杀库存、每人限购数
3. 校验：结束时间不能早于开始时间；同一商品不能同时出现在多个进行中的活动
4. 活动自动启停（@Scheduled + Redisson 分布式锁）：
   - 到达 start_time → status 更新为 1（进行中）
   - 到达 end_time → status 更新为 2（已结束）
5. `FlashSaleItem` 使用 `version` 字段做乐观锁（MyBatis-Plus @Version）

**参考**: DEMAND.md §3.1 运营端, Technology-design.md §3.5.1

---

### T7.2 — 后端秒杀核心接口

**依赖**: T2.1, T2.5, T7.1（需要 FlashSaleItem 和活动配置）, Redis, RocketMQ
**产出文件**:
- `service/FlashSaleService.java` — 补充：`flashOrder()` 核心方法
- `controller/FlashSaleController.java` — `POST /api/flash-sale/order`, `GET /api/flash-sale/order/status`
- `dto/FlashOrderMessage.java` — MQ 消息体

**任务说明**:
1. **秒杀下单** `POST /api/flash-sale/order`（按 Technology-design.md §3.5.2 流程）：
   ```
   ① JWT 鉴权
   ② 限流检查：SET flash:rate:{userId}:{itemId} NX EX 1（1s 间隔）
   ③ 活动时间校验（从 Redis flash_event:{eventId} 获取）
   ④ 限购校验：SISMEMBER flash:users:{itemId} {userId}
   ⑤ Redis 预扣：DECR flash_stock:{itemId}
      ├─ >0 → 继续
      └─ ≤0 → INCR 回滚 + 返回 "已售罄"
   ⑥ 构造 FlashOrderMessage → 发送到 RocketMQ Topic: flash-sale-order
   ⑦ 返回 202 { status: "pending", requestId }
   ```
2. **轮询结果** `GET /api/flash-sale/order/status?requestId=xxx`：
   - 查询 `flash:result:{requestId}` JSON
   - pending → 继续等 / success+orderNo → 跳支付 / failed → 重试

**参考**: Technology-design.md §3.5.1/§3.5.2, DEMAND.md §3.2

---

### T7.3 — 后端 RocketMQ 消费者（秒杀落单）

**依赖**: T7.1, T7.2, T6.1（OrderService）
**产出文件**:
- `consumer/FlashSaleOrderConsumer.java` — 实现 `RocketMQListener<FlashOrderMessage>`
- `service/MqDeduplicationService.java` — 消息去重服务

**任务说明**:
1. **消费逻辑**（按 Technology-design.md §3.5.3）：
   ```
   ① 幂等校验（双重）：
      a) Redis SETNX flash:dedup:order:{orderNo} EX 3600
      b) DB INSERT mq_deduplication (message_key, message_type)
         UNIQUE KEY uk_message_key_type → DuplicateKeyException → 已处理
   ② 乐观锁扣库存（最多 5 次重试，指数退避）：
      UPDATE flash_sale_item
      SET flash_stock = flash_stock - 1, version = version + 1
      WHERE id = #{itemId} AND version = #{version} AND flash_stock > 0
      重试间隔：50ms → 200ms → 500ms → 1s → 2s
   ③ 创建订单（同一事务内）：
      INSERT INTO orders (is_flash_sale=1, status=0)
      INSERT INTO order_item（快照）
      INSERT INTO mq_deduplication
   ④ 事务 COMMIT 后：
      发送 RocketMQ 延迟消息（15min）到 Topic: order-timeout
      更新 Redis flash:result:{requestId} = {status:"success", orderNo:"xxx"}
      SADD flash:users:{itemId} {userId}
   ```
2. @RocketMQMessageListener 配置：topic = "flash-sale-order", consumerGroup = "flash-sale-order-consumer"
3. @Transactional 确保 ②③④ 在同一事务中
4. 消息体 `FlashOrderMessage`：orderNo, userId, itemId, flashPrice, quantity, requestId, timestamp

**参考**: Technology-design.md §3.5.3/§3.5.5/§6.2-6.5, DEMAND.md §5.4

---

### T7.4 — 后端超时未支付消费者（延迟消息）

**依赖**: T7.3（需要 db 订单数据 + Redis 库存 key）, T6.2
**产出文件**:
- `consumer/OrderTimeoutConsumer.java` — 实现 `RocketMQListener<Message>`

**任务说明**:
1. **消费延迟消息**（按 Technology-design.md §3.5.4）：
   ```
   ① 查询订单状态
      ├─ 已支付 (status=1) → 直接 ACK
      └─ 待支付 (status=0) → 继续
   ② 分布式锁：RLock lock = redisson.getLock("lock:order_timeout:" + orderNo)
   ③ @Transactional 取消订单：
      ├─ UPDATE orders SET status=4, cancel_reason='支付超时'
      ├─ Redis INCR flash_stock:{itemId} → 归还秒杀库存
      └─ DB UPDATE flash_sale_item SET flash_stock = flash_stock + 1,
          version = version + 1 WHERE id = ? AND version = ? → 乐观锁归还
   ④ 释放分布式锁
   ```
2. @RocketMQMessageListener：topic = "order-timeout", consumerGroup = "order-timeout-consumer"
3. 幂等通过 mq_deduplication 表保障（message_key = orderNo + ":timeout"）

**参考**: Technology-design.md §3.5.4, DEMAND.md §3.3

---

### T7.5 — 后端秒杀预热定时任务 + Redis 初始化

**依赖**: T1.2, T7.1
**产出文件**:
- `scheduled/FlashPreheatTask.java` — @Scheduled 预热任务

**任务说明**:
1. 每 30 秒检查即将开始（≤5 分钟）且未预热的秒杀活动
2. 使用 Redisson 分布式锁 `task:lock:flash_preheat` 防止多实例重复执行
3. 预热内容：
   - `SET flash_stock:{itemId} {stock}` — 秒杀库存
   - `HSET flash_item:{itemId} ...` — 秒杀商品信息
   - `SET flash_event:{eventId} '{json}'` — 活动时间信息
4. 活动结束后清理 Redis key（可选，避免内存堆积）

**参考**: Technology-design.md §3.5.6, DEMAND.md §3.1

---

### T7.6 — 前端秒杀专区页面

**依赖**: T1.6, T2.4, T7.1, T7.2
**产出文件**:
- `api/flash-sale.ts` — 秒杀 API 函数
- `pages/FlashSale/FlashSaleList.tsx` — 秒杀活动列表
- `pages/FlashSale/FlashSaleDetail.tsx` — 秒杀商品详情 + 秒杀按钮

**任务说明**:
1. **秒杀列表**：
   - 进行中活动 + 即将开始活动（分别展示）
   - 每个活动显示：名称、时间、倒计时、商品缩略
   - 倒计时组件（useCountdown hook）
2. **秒杀详情页**（核心交互）：
   - 商品信息 + 秒杀价 + 原价对比 + 库存进度条
   - **秒杀按钮状态机**（按 Technology-design.md §3.5.7）：
     - 即将开始 → 灰色 + 倒计时，不可点击
     - 立即秒杀 → 红色/橙色高亮按钮
     - 正在抢购 → 按钮禁用 + 旋转动画 + "排队中..."
     - 已售罄 → 灰色 "已售罄"
     - 已结束 → 灰色 "已结束"
   - 点击秒杀 → API 调用 → 202 pending → 轮询（3s 间隔，最多 30s）
   - 轮询成功 → 跳转支付页（15 分钟倒计时）
   - 轮询失败 → Toast "抢购失败"

**参考**: Technology-design.md §3.5.7, DEMAND.md §3.2 用户端

---

### T7.7 — 前端秒杀 Hooks

**依赖**: T1.6
**产出文件**:
- `hooks/useCountdown.ts` — 倒计时 Hook
- `hooks/useFlashSale.ts` — 秒杀轮询 Hook

**任务说明**:
1. **useCountdown(targetTime, onEnd)**：
   - 传入目标时间戳，返回剩余秒数、格式化时间
   - 支持显示格式：`HH:mm:ss` 或 `mm:ss`
   - 到达 0 触发 onEnd 回调
   - 用途：秒杀开始倒计时、支付超时倒计时
2. **useFlashSale(requestId)**：
   - 3 秒间隔轮询 `/api/flash-sale/order/status?requestId=xxx`
   - 最多轮询 30 秒（10 次）
   - 返回 `{ status: 'pending' | 'success' | 'failed' | 'timeout', orderNo }`
   - 成功后自动停止轮询

**参考**: Technology-design.md §3.5.7 倒计时组件

---

## Phase 8: 管理后台

> **解锁条件**: Phase 3（商品后端）+ Phase 6（订单）+ Phase 7（秒杀）
> **并行组 P8-A**: T8.1 + T8.2 后端可并行；T8.3 完成后 T8.4-8.9 前端可全部并行

### T8.1 — 后端数据统计接口

**依赖**: T1.4, T1.2（Redis）, T6.2
**产出文件**:
- `controller/StatisticsController.java` — 数据看板 API
- `service/StatisticsService.java`
- `scheduled/DashboardCacheTask.java` — 定时刷新看板缓存

**任务说明**:
1. **API 接口** `GET /api/admin/statistics/overview`, `GET /api/admin/statistics/product-ranking`, `GET /api/admin/statistics/order-trend?days=7`：
   - 销售概览：总订单数、总销售额、今日订单数、今日销售额
   - 秒杀数据：各活动参与人数、成交率
   - 商品排行：按销量 Top 10
   - 订单趋势：近 7/30 天每日订单量
2. **缓存策略**（按 Technology-design.md §3.6.3 表）：
   - `dashboard:sales_overview` EX 300s
   - `dashboard:product_ranking` EX 600s
   - `dashboard:order_trend_7d` EX 1800s
   - `dashboard:order_trend_30d` EX 3600s
3. 定时任务每 5 分钟刷新，使用 Redisson 分布式锁

**参考**: Technology-design.md §3.6.3, DEMAND.md §2.9.5

---

### T8.2 — 后端操作日志 AOP

**依赖**: T1.5, T2.1（需 SecurityContext 获取用户）
**产出文件**:
- `module/operation/OpLog.java` — @OpLog 注解定义
- `module/operation/OpLogAspect.java` — AOP 切面（@Around + @Async + REQUIRES_NEW）
- `module/operation/entity/OperationLog.java`
- `module/operation/controller/OperationLogController.java` — `GET /api/admin/operation-logs`

**任务说明**:
1. **@OpLog 注解**（按 Technology-design.md §3.6.2）：
   - `module()`: product/order/flash_sale/user
   - `action()`: CREATE/UPDATE/DELETE/TOGGLE_STATUS/LOGIN
   - `description()`: 支持 SpEL 动态描述
2. **AOP 切面**：
   - @Around 拦截 @OpLog 方法
   - finally 块中触发日志记录（成功/失败都记录）
   - @Async 异步执行（不阻塞主业务响应）
   - @Transactional(REQUIRES_NEW) 独立事务（主事务回滚日志不丢失）
3. **日志查询**：分页 + 按模块/操作类型/操作人/时间筛选，仅管理员可查看

**参考**: Technology-design.md §3.6.2, DEMAND.md §2.10

---

### T8.3 — 前端 AdminLayout + AdminGuard

**依赖**: T1.6, T2.4
**产出文件**:
- `components/AdminLayout.tsx` — 后台布局（侧边栏 + 顶部 + 内容区）
- 修改 `router/index.tsx` — 添加 `/admin/*` 路由组

**任务说明**:
1. **侧边栏菜单**（根据角色动态显示）：
   - 运营(2)：商品管理 / 秒杀管理 / 数据看板
   - 管理员(3)：全部菜单（+ 订单管理 + 用户管理 + 操作日志）
2. **面包屑导航** + 内容区 `<Outlet />`
3. 路由守卫：`<AdminGuard minRole={Role.OPERATOR}>`
4. 普通用户访问 `/admin/*` → 403 Forbidden

**参考**: Technology-design.md §3.6.1, DEMAND.md §2.9.1

---

### T8.4 — 前端商品管理页

**依赖**: T3.3, T8.3
**产出文件**:
- `pages/Admin/AdminProducts.tsx` — 商品管理列表 + 创建/编辑表单

**任务说明**:
1. **商品列表**：分页 + 搜索 + 快速筛选（分类/状态）
2. **创建/编辑商品表单**：
   - 基本信息：名称、品牌、分类、简介、描述（富文本）、主图、多图、视频 URL
   - 规格定义：specs JSON 编辑（规格名 + 可选值列表）
   - SKU 管理：批量生成 SKU（规格组合）+ 单独编辑价格/库存/图片/编码
   - 属性参数：attrs 键值对编辑
   - 关键词、排序值
3. **上架/下架**：开关按钮，异步操作
4. **软删除**：二次确认弹窗
5. **表单校验** + 提交失败 Toast

**参考**: DEMAND.md §2.9.2

---

### T8.5 — 前端订单管理页

**依赖**: T6.2, T8.3
**产出文件**:
- `pages/Admin/AdminOrders.tsx` — 订单管理列表 + 详情

**任务说明**:
1. 订单列表：分页 + 状态筛选 + 搜索（订单号/用户名）
2. 订单详情弹窗/页面：商品清单 + 用户信息 + 地址 + 时间线
3. **发货操作**：点击"发货"按钮 → 确认 → 状态更新为"已发货"
4. 仅管理员可见

**参考**: DEMAND.md §2.9.3

---

### T8.6 — 前端秒杀管理页

**依赖**: T7.1, T8.3
**产出文件**:
- `pages/Admin/AdminFlashSales.tsx` — 秒杀活动管理

**任务说明**:
1. **活动列表**：状态筛选（待开始/进行中/已结束）
2. **新建活动**：表单（名称 + 开始/结束时间 + 备注）
   - 校验：结束时间 > 开始时间
3. **关联商品**：选择商品 → 设置秒杀价 + 秒杀库存 + 限购数
   - 校验：同一商品不能在多个进行中活动
4. **启停操作**：开始/暂停/结束按钮

**参考**: DEMAND.md §2.9.4

---

### T8.7 — 前端数据看板

**依赖**: T8.1, T8.3
**产出文件**:
- `pages/Admin/Dashboard.tsx` — 数据看板

**任务说明**:
1. 统计卡片行：总订单数、总销售额、今日订单数、今日销售额（数字滚动动画）
2. 近 7/30 天订单趋势折线图（使用 recharts 或 chart.js）
3. 商品销量 Top 10 排名列表
4. 秒杀活动数据（各活动参与人数、成交率）
5. 加载骨架屏 + 网络错误重试

**参考**: Technology-design.md §3.6.3 看板布局, DEMAND.md §2.9.5

---

### T8.8 — 前端用户管理页

**依赖**: T2.2, T8.3
**产出文件**:
- `pages/Admin/AdminUsers.tsx` — 用户管理列表
- `api/admin.ts` — 管理端 API 函数（统一定义）

**任务说明**:
1. 用户列表：分页 + 搜索（用户名/邮箱）+ 角色筛选
2. 修改角色：下拉选择（普通用户/运营/管理员）
3. 禁用/启用：开关按钮
4. 仅管理员可见

**参考**: DEMAND.md §2.9.1 权限表, OPT.md §6.3

---

### T8.9 — 前端操作日志页

**依赖**: T8.2, T8.3
**产出文件**:
- `pages/Admin/AdminOperationLogs.tsx` — 操作日志表格

**任务说明**:
1. 表格列：时间、操作人、模块、操作类型、描述、目标ID、IP、结果（成功/失败）、耗时
2. 筛选：按模块、操作类型、操作人、时间范围
3. 分页查询
4. 仅管理员可见

**参考**: DEMAND.md §2.10.6

---

## Phase 9: 部署 + 测试

> **解锁条件**: Phase 1-8 全部完成
> **并行组 P9-A**: T9.1 + T9.2 + T9.3 + T9.4 可并行启动

### T9.1 — 后端 Dockerfile

**依赖**: T1.2（完整项目）
**产出文件**: `backend/Dockerfile`

**任务说明**:
1. 多阶段构建：Maven 构建阶段 + JDK 21 运行阶段
2. 运行阶段使用 `eclipse-temurin:21-jre-alpine`
3. 复制 JAR + 设置 JVM 参数（-Xms256m -Xmx512m）
4. 配置健康检查

**参考**: Technology-design.md §9.1

---

### T9.2 — 前端 Dockerfile + Nginx 配置

**依赖**: T1.3（完整项目）
**产出文件**:
- `frontend/Dockerfile` — 多阶段构建（Node 构建 + Nginx 运行）
- `nginx/default.conf` — Nginx 反向代理配置

**任务说明**:
1. 前端构建：`yarn build` → 产出 `dist/`
2. Nginx 配置：
   - `/` → SPA（index.html fallback）
   - `/api/*` → `proxy_pass http://backend:8080`
   - gzip 压缩 + 静态资源缓存
3. Nginx 运行阶段使用 `nginx:alpine`

**参考**: Technology-design.md §9.1/§2.1

---

### T9.3 — docker-compose 最终编排调试

**依赖**: T1.1, T9.1, T9.2
**产出文件**: `docker-compose.yml`（最终版，更新 T1.1 的初始版本）

**任务说明**:
1. 确保所有服务正确编排
2. 环境变量完整（.env 文件或 docker-compose.yml 中定义）
3. backend 的 depends_on 包含 mysql（healthcheck）+ redis + rocketmq
4. RocketMQ broker 配置 `NAMESRV_ADDR` 正确
5. 验证 `docker compose up` 全部服务启动正常

**参考**: DEMAND.md §1.3, Technology-design.md §9.1

---

### T9.4 — JMeter 秒杀压测脚本

**依赖**: Phase 7（秒杀功能可用）
**产出文件**: `jmeter/flash-sale.jmx`

**任务说明**:
1. **场景 1 — 秒杀正常流量**：
   - 200 并发用户，库存 100
   - 验证无超卖（最终订单数 = 100）
2. **场景 2 — 同用户重复秒杀**：
   - 同一用户 50 并发请求
   - 验证限购生效（每人最多 1 单）
3. **场景 3 — 库存一致性验证**：
   - 秒杀结束后检查 Redis flash_stock = DB flash_stock = 已创建订单数
4. 包含 HTTP Header Manager（JWT Token）+ CSV 用户数据

**参考**: Technology-design.md §10.4, DEMAND.md §10 验证清单

---

### T9.5 — 端到端功能联调

**依赖**: Phase 1-8 全部完成（T9.1-T9.4 完成后再做）
**产出**: 验收报告

**任务说明**:
1. 启动完整 docker compose 环境
2. 按 DEMAND.md §11 验证清单逐项测试：
   - 注册 → 登录 → 浏览商品 → 搜索 → 加购 → 下单 → 支付 → 取消
   - 秒杀完整链路：创建活动 → 预热 → 秒杀 → 轮询 → 支付 → 超时释放
   - 管理后台全部功能
   - 各类异常场景覆盖
3. 记录发现的问题

**参考**: DEMAND.md §11 验证清单

---

## 并行执行总览表

| Phase | 任务数 | 关键并行组 | 可同时启动 Agent 数 |
|-------|--------|-----------|---------------------|
| Phase 1 | 6 | T1.2+T1.3 并行, T1.5+T1.6 并行 | 最多 3 个 |
| Phase 2 | 5 | T2.1~T2.5 全部并行 | 5 个 |
| Phase 3 | 5 | T3.1+T3.2+T3.5 并行, T3.3→T3.4 串行 | 最多 3 个 |
| Phase 4 | 3 | T4.1+T4.2+T4.3 全部并行 | 3 个 |
| Phase 5 | 5 | T5.1+T5.2 并行, T5.3+T5.4+T5.5 并行 | 最多 3 个 |
| Phase 6 | 4 | T6.1 先完成, T6.2+T6.3+T6.4 并行 | 最多 3 个 |
| Phase 7 | 7 | T7.6+T7.7 可并行, T7.3+T7.4+T7.5 可部分并行 | 最多 3 个 |
| Phase 8 | 9 | T8.1+T8.2 并行, T8.4~T8.9 全部并行 | 最多 6 个 |
| Phase 9 | 5 | T9.1~T9.4 全部并行 | 4 个 |

---

## 每个任务的标准交付物清单

每完成一个任务，Agent 应产出：

1. ✅ **代码文件**：所有产出的 .java/.tsx/.ts/.sql/.yml 文件
2. ✅ **编译通过**：后端 `mvn compile` 通过 / 前端 `yarn build` 或 `tsc --noEmit` 通过
3. ✅ **功能自检**：任务描述的每个功能点均已实现
4. ✅ **代码规范**：符合 Technology-design.md 的设计约束（如参数化查询、BCrypt cost=12、@JsonIgnore 等）

---

## 关键技术约束（每个 Agent 必须遵守）

| 约束 | 说明 |
|------|------|
| **SQL 注入防护** | 所有 SQL 使用 MyBatis-Plus `#{}` 参数化，排序字段白名单映射 |
| **密码安全** | BCrypt cost=12，前端 SHA-256 传输，password 字段 @JsonIgnore |
| **事务边界** | 下单/取消/秒杀消费 使用 @Transactional，操作日志 REQUIRES_NEW |
| **缓存策略** | Cache-Aside 模式，更新/删除时删除缓存而非更新 |
| **幂等性** | 所有写操作有去重机制，MQ 消费双重幂等（Redis SETNX + DB unique key）|
| **统一返回** | 全部接口使用 Result<T> 包装，异常由 GlobalExceptionHandler 统一处理 |
| **认证检查** | 管理端接口添加 @PreAuthorize 或 @RequireRole，前端路由添加 AuthGuard/AdminGuard |
