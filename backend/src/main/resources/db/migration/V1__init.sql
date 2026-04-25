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
  FULLTEXT INDEX `ft_search` (`name`, `keywords`) WITH PARSER ngram
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
