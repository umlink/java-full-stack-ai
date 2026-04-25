
# 全栈电商系统 + 秒杀功能

一个功能完整的全栈电商系统，包含用户认证、商品管理、购物车、订单系统、秒杀功能及管理后台。

## 技术栈

### 前端
- **框架**: React 18 + TypeScript 5
- **构建工具**: Vite 6
- **样式方案**: TailwindCSS 4
- **UI 组件**: shadcn/ui
- **状态管理**: Zustand
- **路由**: React Router v7
- **表单**: React Hook Form + Zod
- **HTTP 客户端**: Axios

### 后端
- **框架**: Spring Boot 3.2
- **Java 版本**: 17
- **ORM**: MyBatis-Plus 3.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7 + Redisson
- **消息队列**: RocketMQ 5.1
- **安全**: Spring Security 6 + JWT
- **数据库迁移**: Flyway
- **对象存储**: 七牛云 Kodo
- **API 文档**: SpringDoc OpenAPI 2

### 基础设施
- **容器化**: Docker Compose
- **反向代理**: Nginx

## 功能特性

### 用户端
- ✅ 用户注册/登录（JWT 认证）
- ✅ 商品浏览与搜索（MySQL FULLTEXT + ngram）
- ✅ 商品详情页（支持多规格 SKU）
- ✅ 购物车管理
- ✅ 订单创建与支付（模拟支付）
- ✅ 订单取消与退款
- ✅ 收货地址管理
- ✅ 用户个人中心
- ✅ 秒杀活动（高并发优化）
- ✅ 秒杀订单超时处理

### 管理后台
- ✅ 商品管理（CRUD + SKU 管理）
- ✅ 订单管理
- ✅ 秒杀活动管理
- ✅ 用户管理
- ✅ 数据看板（销售概览、商品排行、订单趋势）
- ✅ 操作日志记录

### 技术亮点
- 🚀 秒杀功能：Redis 预扣库存 + RocketMQ 异步落单 + 乐观锁
- 🔒 安全防护：SQL 注入防护、XSS 防护、暴力破解限流
- 💾 缓存策略：布隆过滤器防穿透、Redisson 分布式锁防击穿
- ⚡ 限流机制：令牌桶 + 双维度限流（IP + 用户）
- 📝 操作日志：AOP 切面自动记录管理端操作
- 🐳 Docker 一键部署

## 快速开始

### 前置要求
- Docker 20.10+
- Docker Compose 1.29+

### 环境配置
复制环境变量文件并配置：
```bash
# 使用提供的 .env.example 作为模板，已包含 .env 文件
```

配置项说明：
- `MYSQL_ROOT_PASSWORD`: MySQL root 密码
- `JWT_SECRET`: JWT 签名密钥
- `QINIU_ACCESS_KEY`: 七牛云 Access Key
- `QINIU_SECRET_KEY`: 七牛云 Secret Key
- `SPRING_PROFILES_ACTIVE`: Spring 环境配置（dev/prod）

### 启动服务
```bash
# 克隆项目（如果尚未克隆）
git clone &lt;repository-url&gt;
cd full-stack

# 使用 Docker Compose 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 访问应用
- 前端: http://localhost
- 后端 API: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html

### 初始化数据
系统启动后，Flyway 会自动执行数据库迁移脚本，初始化基础数据。

## 开发指南

### 前端开发
```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

### 后端开发
```bash
cd backend

# 使用 Maven 构建
mvn clean package -DskipTests

# 本地运行（需要先启动 MySQL、Redis、RocketMQ）
mvn spring-boot:run
```

### 本地开发环境
本地开发时，你可以只启动基础设施服务：
```bash
docker-compose up -d mysql redis rocketmq-namesrv rocketmq-broker
```

然后分别启动后端和前端的开发服务器。

## 项目结构

```
full-stack/
├── docker-compose.yml          # Docker Compose 编排
├── nginx/                      # Nginx 配置
│   └── default.conf
├── jmeter/                     # 压测脚本
│   └── flash-sale.jmx
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                # API 接口封装
│   │   ├── components/         # React 组件
│   │   ├── hooks/              # 自定义 Hooks
│   │   ├── pages/              # 页面组件
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Zustand 状态管理
│   │   └── types/              # TypeScript 类型定义
│   ├── Dockerfile
│   └── package.json
├── backend/                    # 后端项目
│   ├── src/main/
│   │   ├── java/com/example/mall/
│   │   │   ├── config/        # 配置类
│   │   │   ├── common/        # 公共类（统一返回、异常处理）
│   │   │   ├── auth/          # 认证模块
│   │   │   ├── module/        # 业务模块
│   │   │   ├── interceptor/   # 拦截器
│   │   │   └── scheduled/     # 定时任务
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/  # Flyway SQL 脚本
│   ├── Dockerfile
│   └── pom.xml
├── DEMAND.md                   # 需求文档
├── Technology-design.md       # 技术设计文档
├── OPT.md                     # 产品分析文档
└── README.md                  # 本文件
```

## 技术架构

### 秒杀核心流程

```
用户点击秒杀
    ↓
[前端] 已抢购检查
    ↓
[后端] JWT 认证 + 限流检查
    ↓
[Redis] DECR 预扣库存
    ↓
[RocketMQ] 发送下单消息
    ↓
[返回] 202 排队中
    ↓
[前端] 轮询结果（3s 间隔，最长 30s）
    ↓
[MQ 消费] 幂等检查 + 乐观锁扣 DB 库存 + 创建订单
    ↓
[Redis] 更新秒杀结果
    ↓
[前端] 轮询获取结果，跳转支付
```

### 缓存策略

| 场景 | 方案 |
|------|------|
| 缓存穿透 | 布隆过滤器 + 空值缓存 |
| 缓存击穿 | Redisson 分布式锁 + 双重检查 |
| 缓存雪崩 | TTL 随机化 + 熔断降级 |

### 消息队列使用

- **Topic**: `flash-sale-order` - 秒杀订单异步落单
- **Topic**: `order-timeout` - 订单超时检查（延迟消息 15 分钟）

## 压测

项目包含 JMeter 压测脚本，用于验证秒杀场景的性能：

```bash
# 压测脚本位置
jmeter/flash-sale.jmx
```

压测场景：
- 200 并发用户
- 100 秒杀库存
- 验证无超卖，最终订单数 ≤ 100

## 多环境配置

### 开发环境 (dev)
- 日志级别: DEBUG
- Flyway: 启动时自动迁移
- 限流策略: 宽松（100 req/s）

### 生产环境 (prod)
- 日志级别: INFO
- Flyway: 手动执行迁移
- 限流策略: 严格（根据压测调整）

## 安全说明

- **密码存储**: BCrypt 加密
- **JWT 有效期**: 2 小时
- **SQL 注入**: MyBatis-Plus 参数化查询
- **XSS 防护**: OWASP Java HTML Sanitizer 过滤富文本
- **文件上传**: 魔数校验 + 扩展名白名单 + 大小限制
- **敏感信息脱敏**: 日志自动脱敏手机号、邮箱等

## 相关文档

- [DEMAND.md](./DEMAND.md) - 完整需求文档
- [Technology-design.md](./Technology-design.md) - 详细技术设计文档
- [OPT.md](./OPT.md) - 产品形态分析

## 许可证

本项目仅供学习交流使用。

