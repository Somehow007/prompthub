# 智享AI提示词平台 — 设计文档

> 创建于 2026-06-28

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术方案评估](#2-技术方案评估)
3. [系统架构设计](#3-系统架构设计)
4. [数据库设计](#4-数据库设计)
5. [功能模块设计](#5-功能模块设计)
6. [开发阶段与交付计划](#6-开发阶段与交付计划)
7. [并发控制方案](#7-并发控制方案)
8. [查询优化方案](#8-查询优化方案)
9. [测试策略](#9-测试策略)
10. [Bug 修复记录](#10-bug-修复记录)

---

## 1. 项目概述

### 1.1 项目背景

构建一套 B/S 架构的 AI 提示词模板平台，通过 Web 界面实现提示词模板的创建、版本管理、分类检索、交易使用等全流程数字化管理。

### 1.2 核心目标

| 目标 | 说明 |
|------|------|
| 数据库设计 | E-R 图、3NF 范式规范、完整建表 SQL |
| SQL 高级特性 | 多表查询、事务、存储过程 |
| 全栈实现 | Spring Boot 后端 + Vue 3 前端，前后端分离 |

### 1.3 用例概览

```
┌─────────────────────────────────────────────────────┐
│                   智享AI提示词平台                      │
│                                                       │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ 用户注册 │  │ 模板浏览  │  │  模板搜索筛选  │        │
│  └─────────┘  └──────────┘  └──────────────┘        │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ 用户登录 │  │ 模板创建  │  │  购买付费模板  │        │
│  └─────────┘  └──────────┘  └──────────────┘        │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ 个人主页 │  │ 版本管理  │  │  评分与评价   │        │
│  └─────────┘  └──────────┘  └──────────────┘        │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ 创作者等级│  │ 标签管理  │  │  数据看板     │        │
│  └─────────┘  └──────────┘  └──────────────┘        │
│  ┌─────────┐  ┌──────────┐  ┌──────────────┐        │
│  │ 限时活动 │  │ 智能推荐  │  │  平台总览     │        │
│  └─────────┘  └──────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────┘
```

---

## 2. 技术方案评估

### 2.1 后端技术选型

| 技术 | 版本 | 选型理由 |
|------|------|----------|
| **Java** | 17 | LTS 版本，生态成熟，Spring Boot 3.x 最低要求 |
| **Spring Boot** | 3.5.7 | 业界标准，自动配置，starter 生态丰富 |
| **MyBatis-Plus** | 3.5.14 | 增强 MyBatis，免写基础 CRUD，支持分页、逻辑删除 |
| **MySQL** | 8.0+ | 关系型数据库，满足事务、存储过程等课设要求 |
| **Redis** | 7.x | 缓存热点数据、排行榜、限时活动库存 |
| **Redisson** | 4.0.0 | Redis 分布式锁，保证并发安全（活动抢购、余额扣减） |
| **Sa-Token** | 1.43.0 | 轻量级权限认证框架，支持登录鉴权、角色权限 |
| **Hutool** | 5.8.37 | Java 工具类库，简化日常开发 |
| **Fastjson2** | 2.0.36 | 高性能 JSON 序列化 |

**理由**：Spring Boot + MyBatis-Plus 是 Java 后端最成熟的组合之一。Sa-Token 比 Spring Security 更轻量、API 更直观。Redisson 提供 JVM 级别的分布式锁，不需要额外运维 ZooKeeper。

### 2.2 前端技术选型

| 技术 | 选型理由 |
|------|----------|
| **Vue 3** (Composition API) | 主流前端框架，响应式系统优秀，生态完善 |
| **Vite** | 构建速度极快（相比 Webpack），Vue 官方推荐 |
| **Element Plus** | Vue 3 生态最成熟的 UI 组件库，组件丰富，文档齐全 |
| **ECharts** | 数据看板图表库，功能强大，社区活跃 |
| **Pinia** | Vue 3 官方状态管理，TypeScript 友好 |
| **Vue Router** | 官方路由方案 |
| **Axios** | HTTP 客户端，拦截器机制适合统一处理 Token |

**理由**：Vue 3 + Element Plus 是当前国内企业级后台管理系统最主流的前端方案，稳定成熟，学习成本低，中文文档完善。

> 更新于 2026-06-28：技术选型讨论结果 — 后端沿用已初始化的 Spring Boot 3.5.7 体系，前端采用 Vue 3 + Element Plus + ECharts。

---

## 3. 系统架构设计

### 3.1 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    浏览器 (Browser)                     │
│          Vue 3 + Element Plus + ECharts               │
│                  (localhost:5173)                      │
└────────────────────┬─────────────────────────────────┘
                     │ HTTP REST API (JSON)
                     │ Sa-Token 认证
┌────────────────────┴─────────────────────────────────┐
│              Spring Boot 3.5.7 (port 8099)            │
│                                                       │
│  ┌──────────┐ ┌──────────┐ ┌───────────────────┐    │
│  │ Controller│ │ Service  │ │   Mapper (MyBatis) │    │
│  │  (REST)   │ │ (业务逻辑) │ │   Plus)           │    │
│  └──────────┘ └──────────┘ └────────┬──────────┘    │
│                                      │                │
│              ┌───────────────────────┼──────┐         │
│              │                       │      │         │
│         ┌────┴────┐           ┌─────┴──┐   │         │
│         │  MySQL  │           │ Redis  │   │         │
│         │ (主存储) │           │ (缓存)  │   │         │
│         └─────────┘           └────────┘   │         │
└──────────────────────────────────────────────┘         │
```

### 3.2 后端分层结构

```
src/main/java/com/somehow/work/prompthub/
├── PrompthubApplication.java          # 启动类
├── config/                             # 配置类
│   ├── SaTokenConfig.java             # Sa-Token 配置
│   ├── MyBatisPlusConfig.java         # MyBatis-Plus 分页插件
│   ├── RedisConfig.java               # Redis 序列化配置
│   └── WebMvcConfig.java              # CORS 跨域配置
├── controller/                         # 控制器层
│   ├── UserController.java             # 用户注册/登录/信息/充值/主页
│   ├── TemplateController.java         # 模板CRUD/版本/搜索/使用记录
│   ├── OrderController.java            # 购买/订单列表
│   ├── TagController.java              # 标签树查询
│   ├── ReviewController.java           # 评价创建/列表/删除
│   ├── FavoriteController.java         # 收藏toggle/列表/检查
│   ├── ActivityController.java         # 限时活动（Phase 5）
│   └── StatisticsController.java       # 数据统计（Phase 4）
├── service/                            # 业务逻辑层
│   ├── UserService.java
│   ├── TemplateService.java
│   ├── OrderService.java
│   ├── TagService.java
│   ├── ReviewService.java
│   ├── ActivityService.java
│   ├── StatisticsService.java
│   └── RecommendService.java
├── service/impl/                       # 业务实现
│   └── ...
├── mapper/                             # 数据访问层
│   └── ...
├── entity/                             # 实体类
│   └── ...
├── dto/                                # 数据传输对象
│   └── ...
├── vo/                                 # 视图对象
│   └── ...
├── enums/                              # 枚举类
│   └── ...
├── exception/                          # 异常处理
│   ├── GlobalExceptionHandler.java
│   └── BusinessException.java
└── util/                               # 工具类
    └── ...
```

### 3.3 前端分层结构

```
prompthub-frontend/
├── index.html
├── vite.config.ts
├── package.json
├── src/
│   ├── main.ts                         # 入口
│   ├── App.vue                         # 根组件
│   ├── router/                         # 路由配置
│   │   └── index.ts
│   ├── stores/                         # Pinia 状态管理
│   │   ├── user.ts                     # 用户状态
│   │   └── template.ts                 # 模板状态
│   ├── api/                            # API 请求封装
│   │   ├── request.ts                  # Axios 实例 + 拦截器
│   │   ├── user.ts                     # 用户注册/登录/信息/充值/主页
│   │   ├── template.ts                 # 模板CRUD/版本/搜索/使用记录
│   │   ├── tag.ts                      # 标签树查询
│   │   ├── order.ts                    # 购买/订单列表
│   │   ├── review.ts                   # 评价创建/列表/删除
│   │   ├── favorite.ts                # 收藏toggle/列表/检查
│   │   └── statistics.ts              # 数据统计（Phase 4）
│   ├── views/                          # 页面组件
│   │   ├── Home.vue                    # 首页/模板广场
│   │   ├── Login.vue                   # 登录
│   │   ├── Register.vue                # 注册
│   │   ├── Profile.vue                 # 个人主页（模板/收藏/订单/已购/收入）
│   │   ├── Favorites.vue               # 我的收藏页
│   │   ├── TemplateDetail.vue          # 模板详情（含购买/收藏/评价/使用）
│   │   ├── TemplateCreate.vue          # 创建/编辑模板
│   │   ├── TemplateHistory.vue         # 版本历史
│   │   ├── Dashboard.vue               # 数据看板（Phase 4）
│   │   └── Activity.vue                # 限时活动（Phase 5）
│   ├── components/                     # 公共组件
│   │   ├── TemplateCard.vue            # 模板卡片
│   │   ├── TagSelector.vue             # 标签选择器
│   │   ├── RatingStars.vue             # 星级评分组件
│   │   └── AppLayout.vue               # 全局布局
│   └── utils/                          # 工具函数
│       └── index.ts
```

---

## 4. 数据库设计

### 4.1 E-R 图（概念模型）

```
                    ┌──────────────────────┐
                    │        User           │
                    │──────────────────────│
                    │ PK  id               │
                    │     username          │
                    │     password_hash     │
                    │     email             │
                    │     phone             │
                    │     avatar_url        │
                    │     creator_level      │
                    │     balance           │
                    │     role              │
                    │     status            │
                    │     created_at        │
                    └────┬─────┬───────┬───┘
                         │     │       │
                        1│    1│       │1
          ┌──────────────┘     │       └──────────────┐
          │                    │                      │
          │ 1                  │ 1                    │ 1
   ┌──────┴──────┐   ┌───────┴────────┐   ┌──────────┴──────┐
   │   Order     │   │  UsageLog      │   │    Review       │
   │─────────────│   │────────────────│   │─────────────────│
   │ PK id       │   │ PK id          │   │ PK id           │
   │ FK user_id  │   │ FK user_id     │   │ FK user_id      │
   │ FK template_│   │ FK template_id │   │ FK template_id   │
   │      id     │   │ input_params   │   │ rating (1-5)    │
   │ amount      │   │ created_at     │   │ content         │
   │ status      │   └────────────────┘   │ created_at      │
   │ created_at  │                        └────────┬─────────┘
   └─────────────┘                                 │
                                                   │ N
          ┌────────────────────────────────────────┘
          │
          │ N                          ┌──────────────────────┐
   ┌──────┴──────────────┐            │        Tag           │
   │     Template        │            │──────────────────────│
   │─────────────────────│            │ PK  id               │
   │ PK  id              │  N     N   │     name             │
   │ FK  creator_id──────┼────────────│     parent_id ────┐  │
   │     title           │  模板-标签   │     level        │  │
   │     description     │  关联表     │     sort_order   │  │
   │     cover_url       │            └──────────┬────────┘  │
   │     status(上/下架)  │                       │           │
   │     price           │                       │ 自引用     │
   │     use_count       │                       │ (parent_id)│
   │     favorite_count  │                       └───────────┘
   │     review_count    │
   │     avg_rating      │
   │     created_at      │
   │     updated_at      │
   └──────┬──────────────┘
          │
          │ 1
   ┌──────┴──────────────┐
   │  TemplateVersion    │
   │─────────────────────│
   │ PK  id              │
   │ FK  template_id     │
   │     version_number  │
   │     prompt_content  │
   │     change_note     │
   │     created_at      │
   └─────────────────────┘

   ┌──────────────────────────────────┐
   │     ActivityTemplate             │
   │──────────────────────────────────│
   │ PK  id                           │
   │ FK  template_id                  │
   │     start_time                   │
   │     end_time                     │
   │     total_quota                  │
   │     remaining_quota              │
   │     status                       │
   └──────────────────────────────────┘
```

> 更新于 2026-06-29：Phase 3 新增 Favorite（收藏）、IncomeRecord（收入明细）两个实体未在 E-R 图中绘制。Favorite 为 User-Template 多对多关联表；IncomeRecord 与 Order 为 1:1 关系，记录创作者销售收入。完整表结构见 4.2 节 DDL。

### 4.2 逻辑模型（表结构设计）

#### 4.2.1 范式分析

所有表满足 **第三范式（3NF）**：
- **1NF**：每个字段都是不可分割的原子值
- **2NF**：非主键字段完全函数依赖于主键（无部分依赖）
- **3NF**：非主键字段不传递依赖于主键

以 `review` 表为例：
- 评分 `rating` 和评价内容 `content` 直接依赖于评价 ID，不通过其他非主键字段传递依赖 → 满足 3NF
- 模板的 `avg_rating` 和 `review_count` 设计在 `template` 表中作为冗余字段，这是经过权衡的**反范式化设计**，避免每次查询模板列表时都要 JOIN 评价表做聚合计算 —— 这是性能优化中常见的可控冗余

#### 4.2.2 建表 DDL

```sql
-- ============================================================
-- 数据库: prompthub
-- 描述: 智享AI提示词平台核心业务表
-- 设计依据: 3NF 范式 + 实际查询性能权衡
-- ============================================================

CREATE DATABASE IF NOT EXISTS prompthub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE prompthub;

-- ============================================================
-- 1. 用户表 (user)
-- 管理用户基本信息、创作者等级、账户余额
-- ============================================================
CREATE TABLE `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`      VARCHAR(50)  NOT NULL COMMENT '用户名，唯一',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值（bcrypt）',
  `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar_url`    VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `creator_level` TINYINT      NOT NULL DEFAULT 0 COMMENT '创作者等级（0-5，由系统自动计算）',
  `balance`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 标签表 (tag)
-- 支持层级关系的标签体系
-- ============================================================
CREATE TABLE `tag` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`       VARCHAR(50)  NOT NULL COMMENT '标签名称',
  `parent_id`  BIGINT       DEFAULT NULL COMMENT '父标签ID，NULL表示顶级标签',
  `level`      TINYINT      NOT NULL DEFAULT 1 COMMENT '层级：1-一级 2-二级 3-三级',
  `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  CONSTRAINT `fk_tag_parent` FOREIGN KEY (`parent_id`) REFERENCES `tag`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表（支持层级关系）';

-- ============================================================
-- 3. 模板表 (template)
-- 核心业务表：管理提示词模板
-- ============================================================
CREATE TABLE `template` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `creator_id`     BIGINT        NOT NULL COMMENT '创作者用户ID',
  `title`          VARCHAR(200)  NOT NULL COMMENT '模板标题',
  `description`    VARCHAR(2000) DEFAULT NULL COMMENT '适用场景描述',
  `cover_url`      VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
  `current_version` INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格（0=免费）',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-上架 0-下架 2-审核中',
  `use_count`      BIGINT        NOT NULL DEFAULT 0 COMMENT '总使用次数',
  `use_count_7d`   INT           NOT NULL DEFAULT 0 COMMENT '近7天使用次数',
  `favorite_count` INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `review_count`   INT           NOT NULL DEFAULT 0 COMMENT '评价数',
  `avg_rating`     DECIMAL(2,1)  NOT NULL DEFAULT 0.0 COMMENT '平均评分（冗余，避免频繁JOIN聚合）',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status_price` (`status`, `price`),
  KEY `idx_use_count` (`use_count`),
  KEY `idx_avg_rating` (`avg_rating`),
  KEY `idx_created_at` (`created_at`),
  FULLTEXT KEY `ft_title_desc` (`title`, `description`),
  CONSTRAINT `fk_template_creator` FOREIGN KEY (`creator_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词模板表';

-- ============================================================
-- 4. 模板版本表 (template_version)
-- 每次编辑自动生成新版本，支持版本对比和回滚
-- ============================================================
CREATE TABLE `template_version` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `template_id`     BIGINT        NOT NULL COMMENT '模板ID',
  `version_number`  INT           NOT NULL COMMENT '版本号（递增）',
  `prompt_content`  TEXT          NOT NULL COMMENT 'Prompt内容',
  `change_note`     VARCHAR(1000) DEFAULT NULL COMMENT '变更说明',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version` (`template_id`, `version_number`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `fk_version_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本表（历史版本记录）';

-- ============================================================
-- 5. 模板-标签关联表 (template_tag)
-- 多对多关联
-- ============================================================
CREATE TABLE `template_tag` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `template_id` BIGINT   NOT NULL COMMENT '模板ID',
  `tag_id`      BIGINT   NOT NULL COMMENT '标签ID',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_tag` (`template_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`),
  CONSTRAINT `fk_tt_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板-标签关联表';

-- ============================================================
-- 6. 评分评价表 (review)
-- 用户使用模板后的评分和评价
-- ============================================================
CREATE TABLE `review` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `user_id`     BIGINT         NOT NULL COMMENT '用户ID',
  `template_id` BIGINT         NOT NULL COMMENT '模板ID',
  `rating`      TINYINT        NOT NULL COMMENT '评分（1-5）',
  `content`     VARCHAR(2000)  DEFAULT NULL COMMENT '评价内容',
  `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_template` (`user_id`, `template_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_rating` (`rating`),
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_review_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_rating_range` CHECK (`rating` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分评价表';

-- ============================================================
-- 7. 订单表 (order)
-- 用户购买付费模板的交易记录
-- 核心事务：下单同时扣余额、更新模板使用统计
-- ============================================================
CREATE TABLE `order` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`      VARCHAR(32)    NOT NULL COMMENT '订单编号（业务唯一）',
  `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
  `template_id`   BIGINT         NOT NULL COMMENT '模板ID',
  `amount`        DECIMAL(10,2)  NOT NULL COMMENT '交易金额',
  `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '状态：0-待支付 1-已完成 2-已取消 3-已退款',
  `pay_time`      DATETIME       DEFAULT NULL COMMENT '支付时间',
  `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_order_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表（购买交易记录）';

-- ============================================================
-- 8. 使用日志表 (usage_log)
-- 记录每次模板使用的详细日志，用于统计分析
-- ============================================================
CREATE TABLE `usage_log` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
  `template_id`   BIGINT        NOT NULL COMMENT '模板ID',
  `input_params`  VARCHAR(2000) DEFAULT NULL COMMENT '输入参数摘要',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_template_created` (`template_id`, `created_at`),
  CONSTRAINT `fk_usage_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_usage_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='使用日志表';

-- ============================================================
-- 9. 活动模板表 (activity_template)
-- 限时免费活动管理
-- ============================================================
CREATE TABLE `activity_template` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `template_id`      BIGINT       NOT NULL COMMENT '模板ID',
  `start_time`       DATETIME     NOT NULL COMMENT '活动开始时间',
  `end_time`         DATETIME     NOT NULL COMMENT '活动结束时间',
  `total_quota`      INT          NOT NULL COMMENT '限量总份数',
  `remaining_quota`  INT          NOT NULL COMMENT '剩余份数',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-进行中 0-已结束 2-未开始',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`),
  CONSTRAINT `fk_activity_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时活动模板表';

-- ============================================================
-- 10. 收藏表 (favorite)
-- 用户收藏模板的关联
-- ============================================================
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
  `template_id` BIGINT   NOT NULL COMMENT '模板ID',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_template` (`user_id`, `template_id`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `fk_fav_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_fav_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

-- ============================================================
-- 11. 收入明细表 (income_record)
-- 创作者收入明细
-- ============================================================
CREATE TABLE `income_record` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id`     BIGINT        NOT NULL COMMENT '创作者用户ID',
  `template_id` BIGINT        NOT NULL COMMENT '模板ID',
  `order_id`    BIGINT        NOT NULL COMMENT '关联订单ID',
  `amount`      DECIMAL(10,2) NOT NULL COMMENT '收入金额',
  `type`        VARCHAR(20)   NOT NULL COMMENT '类型：sale-销售 refund-退款',
  `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_income_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_income_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`),
  CONSTRAINT `fk_income_order` FOREIGN KEY (`order_id`) REFERENCES `order`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者收入明细表';
```

### 4.3 索引设计说明

| 表 | 索引 | 类型 | 覆盖场景 |
|----|------|------|----------|
| template | `idx_status_price` | 联合索引 | 模板广场按价格筛选上架模板 |
| template | `ft_title_desc` | 全文索引 | 关键字搜索标题和描述 |
| template | `idx_use_count` / `idx_avg_rating` | 单列索引 | 热门排序 |
| usage_log | `idx_template_created` | 联合索引 | 按模板统计某时间段使用趋势 |
| review | `uk_user_template` | 唯一索引 | 保证同一用户对同一模板只能评价一次 |
| template_version | `uk_template_version` | 唯一索引 | 保证同一模板版本号唯一 |
| activity_template | `idx_status_time` | 联合索引 | 查询进行中的活动 |

---

## 5. 功能模块设计

### 5.1 模块总览

```
┌──────────────────────────────────────────┐
│            智享AI提示词平台                 │
├──────────┬──────────┬──────────┬─────────┤
│  用户模块  │  模板模块  │  交易模块  │  数据模块  │
├──────────┼──────────┼──────────┼─────────┤
│ ·注册登录  │ ·模板CRUD │ ·订单创建  │ ·创作者看板 │
│ ·个人主页  │ ·版本管理  │ ·余额管理  │ ·平台总览  │
│ ·创作者等级 │ ·上下架    │ ·交易记录  │ ·使用趋势  │
│ ·收藏管理  │ ·标签体系  │ ·收入明细  │ ·热门排行  │
├──────────┼──────────┼──────────┼─────────┤
│  搜索模块  │  互动模块  │  活动模块  │  推荐模块  │
├──────────┼──────────┼──────────┼─────────┤
│ ·关键字搜索 │ ·评分评价  │ ·限时免费  │ ·协同过滤  │
│ ·多条件筛选 │ ·使用日志  │ ·限量管理  │ ·标签推荐  │
│ ·分页排序  │          │          │          │
└──────────┴──────────┴──────────┴─────────┘
```

### 5.2 核心业务流程

#### 5.2.1 购买模板流程（事务保证 + 访问控制）

```
用户发起购买
    │
    ▼
┌─────────────────────────────┐
│  1. 检查模板状态（上架？）      │
│  2. 检查用户余额是否充足        │
│  3. 检查是否已购买过           │
└──────────┬──────────────────┘
           │ 通过
           ▼
┌─────────────────────────────┐
│  @Transactional              │ ← 事务边界
│  4. 扣减用户余额              │
│  5. 生成订单记录              │
│  6. 记录创作者收入            │
│  7. 更新模板使用统计          │
└──────────┬──────────────────┘
           │ 成功
           ▼
        返回订单信息
           │
           ▼
┌─────────────────────────────┐
│  访问控制（查看/使用校验）      │
│  - getDetail()：未购买 →      │
│    隐藏 promptContent，       │
│    返回 hasPurchased=false   │
│  - recordUse()：未购买 →      │
│    抛出 "请先购买此模板"       │
│  - 创作者本人无需购买即可访问   │
│  - 免费模板无访问限制          │
└─────────────────────────────┘
```

**访问控制规则**：

| 角色 | 免费模板 | 付费模板（未购买） | 付费模板（已购买） |
|------|----------|-------------------|-------------------|
| 匿名用户 | 可查看 Prompt | ❌ Prompt 隐藏 | — |
| 登录用户（非创作者） | 可查看 Prompt | ❌ Prompt 隐藏，显示购买 CTA | ✅ 完整内容 + 记录使用 |
| 创作者本人 | 可查看 Prompt | ✅ 完整内容（无需购买） | ✅ 完整内容 |

#### 5.2.2 版本管理流程

```
编辑模板
    │
    ▼
读取当前版本号 N
    │
    ▼
插入 template_version (version_number = N+1)
    │
    ▼
更新 template.current_version = N+1
    │
    ▼
版本历史可查看/对比/回滚
    │
    ▼
回滚操作：将指定历史版本的 prompt_content
          复制为新的当前版本（N+2）
```

#### 5.2.3 限时活动领取流程（Redis 分布式锁）

```
用户点击"免费领取"
    │
    ▼
┌─────────────────────────────┐
│  Redis 分布式锁              │
│  key: activity:lock:{id}     │
└──────────┬──────────────────┘
           │ 获取锁
           ▼
┌─────────────────────────────┐
│  1. 检查活动是否在有效期内     │
│  2. 检查剩余份数 > 0         │
│  3. DECR remaining_quota    │
│  4. 生成使用记录             │
└──────────┬──────────────────┘
           │ 释放锁
           ▼
        返回结果
```

---

## 6. 开发阶段与交付计划

### 阶段总览

```
Phase 1 ───► Phase 2 ───► Phase 3 ───► Phase 4 ───► Phase 5
基础设施     核心业务      交易互动      数据统计      拓展创新
  2天         3天          2天          2天          2天
```

### Phase 1：基础设施搭建（预计 2 天）

> **目标**：完成项目骨架、数据库初始化、用户认证体系

| # | 任务 | 内容 | 后端 | 前端 | 测试验收标准 |
|---|------|------|------|------|-------------|
| 1.1 | 项目初始化 | Spring Boot 骨架完善、Vue 3 项目创建、依赖安装、目录结构建立 | ✅ | ✅ | 两个项目均能正常启动，前端能访问后端 API |
| 1.2 | 数据库初始化 | 执行全部 DDL 建表、初始化标签种子数据、配置 MyBatis-Plus | ✅ | — | 数据库表创建成功，MyBatis-Plus 连接正常 |
| 1.3 | 用户注册 | 用户名+密码+邮箱注册、bcrypt 加密、参数校验 | ✅ | ✅ | 注册成功返回 Token，重复用户名/邮箱提示错误 |
| 1.4 | 用户登录 | Sa-Token 集成、登录返回 Token、登录拦截器 | ✅ | ✅ | 登录成功跳转首页，未登录访问需认证接口返回 401 |
| 1.5 | 全局基础能力 | 统一响应体（Result）、全局异常处理、CORS 配置、前端 Axios 封装 | ✅ | ✅ | 所有 API 返回统一格式 `{code, message, data}` |

**Phase 1 交付物**：
- 可运行的后端项目（编译通过 + 测试通过）
- 可运行的前端项目（vue-tsc 无错误 + vite build 成功）
- 数据库全部表结构就位
- 用户注册/登录功能可演示

---

### Phase 2：核心业务模块（预计 3 天）

> **目标**：完成模板管理、标签体系、搜索筛选、版本管理

| # | 任务 | 内容 | 后端 | 前端 | 测试验收标准 |
|---|------|------|------|------|-------------|
| 2.1 | 标签管理 | 标签 CRUD、层级查询（递归/扁平化）、标签树组件 | ✅ | ✅ | 标签树正确展示，可按层级展开 |
| 2.2 | 模板创建 | 填写标题/描述/Prompt/价格、选择标签、自动生成 v1 版本 | ✅ | ✅ | 创建成功后在数据库 template + template_version 均有记录 |
| 2.3 | 模板列表 | 分页查询、按标签筛选、按价格/评分/热度排序、模板卡片 | ✅ | ✅ | 分页正确，筛选+排序组合生效 |
| 2.4 | 模板详情 | 查看模板完整信息、Prompt 内容、当前标签、评分评价列表 | ✅ | ✅ | 详情页信息完整展示 |
| 2.5 | 关键字搜索 | 全文索引搜索标题+描述、搜索结果高亮、分页 | ✅ | ✅ | 搜索"写作"能匹配到相关模板 |
| 2.6 | 版本管理 | 编辑模板→自动生成新版本、版本历史列表、版本内容对比、回滚 | ✅ | ✅ | 编辑3次后版本列表有3条，回滚后 current_version 递增 |
| 2.7 | 上下架管理 | 创作者对自己模板的上架/下架操作 | ✅ | ✅ | 下架后模板在广场不显示，但创作者个人页可见 |

**Phase 2 交付物**：
- 标签树可正常使用
- 模板完整生命周期可演示（创建→编辑→版本回滚→上下架）
- 搜索+筛选+分页+排序组合可用

> ✅ Phase 2 完成于 2026-06-28：后端新增 14 个文件（4 个 Entity、4 个 Mapper、3 个 DTO、4 个 VO、2 个 Service、2 个 Controller），前端新增 7 个文件（2 个 API、3 个组件、4 个页面），编译+测试+类型检查+构建全部通过。

> ✅ Phase 3 完成于 2026-06-28，Bug 修复于 2026-06-29。
>
> **初始实现**（06-28）：后端新增 15 个文件（5 个 Entity: Order/UsageLog/Review/Favorite/IncomeRecord, 5 个 Mapper, 2 个 DTO: RechargeDTO/CreateReviewDTO, 5 个 VO: OrderVO/ReviewVO/UserProfileVO/IncomeRecordVO, 3 个 Service: OrderService/ReviewService/FavoriteService, 3 个 Controller: OrderController/ReviewController/FavoriteController），更新了 UserMapper（deductBalance）、UserService/Impl（recharge/profile）、TemplateService/Impl（recordUse）、UserController（recharge/profile）、TemplateController（recordUse）。前端新增 5 个文件（3 个 API: order.ts/review.ts/favorite.ts, 1 个组件: RatingStars.vue, 2 个页面: Profile.vue/Favorites.vue）。
>
> **访问控制修复**（06-29）：修复了付费模板无需购买即可查看/使用的严重安全漏洞。OrderService 新增 `isPurchased()` 和 `getPurchasedTemplateIds()`；TemplateDetailVO 新增 `hasPurchased` 字段；`getDetail()` 对未购买的付费模板隐藏 Prompt 内容；`recordUse()` 增加购买校验。UserProfileVO 新增 `purchasedTemplates`，个人主页新增"已购模板"Tab。前端 TemplateDetail.vue 增加内容门控（锁定界面+购买CTA）和购买状态追踪（✓已购买/✓已获取按钮）。编译+测试+类型检查+构建全部通过。

---

### Phase 3：交易与互动（预计 2 天）

> **目标**：完成购买交易、账户余额、评分评价、收藏功能

| # | 任务 | 内容 | 后端 | 前端 | 测试验收标准 |
|---|------|------|------|------|-------------|
| 3.1 | 账户余额 | 余额查询、充值接口、余额变动记录 | ✅ | ✅ | 余额显示正确，充值后余额增加 |
| 3.2 | 购买模板 | 下单扣款事务、订单生成、重复购买检测、订单列表 | ✅ | ✅ | 并发购买时事务保证余额一致；重复购买提示"已购买" |
| 3.3 | 使用日志 | 使用模板时记录日志、`use_count`/`use_count_7d` 自动更新 | ✅ | ✅ | 使用1次后 use_count+1，统计查询能看到记录 |
| 3.4 | 评分评价 | 评分 1-5 星、文字评价、同一用户只能评价一次 | ✅ | ✅ | 评完分后模板 avg_rating 自动更新；再次评价提示"已评价" |
| 3.5 | 收藏管理 | 收藏/取消收藏、收藏列表、`favorite_count` 更新 | ✅ | ✅ | 收藏后计数+1，取消后-1，列表正确展示 |
| 3.6 | 个人主页 | 已发布模板、收藏列表、使用记录、余额和收入、创作者等级 | ✅ | ✅ | 个人主页数据完整、等级自动计算 |

**Phase 3 交付物**：
- 完整的购买流程可演示（浏览→购买→使用→评价）
- 事务验证：模拟并发购买，余额和订单数据一致
- 个人主页信息完整

---

### Phase 4：数据统计与看板（预计 2 天）

> **目标**：完成数据看板、热门排行、使用趋势图、平台总览。个人主页已有基础统计（模板数/收藏/使用次数/收入/已购模板），Phase 4 专注于**聚合分析**和**图表可视化**。

> 更新于 2026-06-29：Phase 3 个人主页已涵盖部分统计数据（templateCount, favoriteCount, totalUseCount, totalIncome），Phase 4 聚焦排行榜、趋势图、平台总览和创作者等级存储过程。

| # | 任务 | 内容 | 后端 | 前端 | 测试验收标准 |
|---|------|------|------|------|-------------|
| 4.1 | 热门排行 | 综合评分×使用量×收藏数计算热度分、Top N 查询、首页排行组件 | ✅ | ✅ | 排行榜展示正确，热度分公式合理 |
| 4.2 | 使用趋势 | 指定模板近30天每日使用频次 SQL、ECharts 折线图 | ✅ | ✅ | 折线图数据与 usage_log 统计一致 |
| 4.3 | 创作者看板 | 集成到个人主页：收入趋势（按月）、模板使用趋势、ECharts 图表 | ✅ | ✅ | 创作者登录后能看到自己模板的可视化数据 |
| 4.4 | 平台总览 | 总用户数、总模板数、总交易额、新增用户趋势（Dashboard 页面） | ✅ | ✅ | 首页/Dashboard 展示平台总览数据 |
| 4.5 | 创作者等级 | 根据被使用次数和评分自动计算等级的 MySQL 存储过程 + 定时调用 | ✅ | — | 存储过程执行后等级正确更新 |

**Phase 4 交付物**：
- 数据看板页面（Dashboard.vue + ECharts）
- 首页热门排行组件
- 平台总览统计数据
- 创作者等级自动更新存储过程
- 个人主页已有的统计数据作为基础，Phase 4 增加趋势图和排行

---

### Phase 5：拓展创新功能（预计 2 天）

> **目标**：限时活动、智能推荐、系统打磨。前期已完成大部分打磨工作（返回导航、加载状态、错误处理、响应式适配），Phase 5 聚焦创新功能和最终交付物。

> 更新于 2026-06-29：前期已完成：所有页面返回导航（TemplateCreate/Login/Register）、ElMessage 错误提示、loading 状态、响应式 CSS、模糊搜索（LIKE）、评价时间修复、删除评价功能。Phase 5 专注于活动和推荐。

| # | 任务 | 内容 | 后端 | 前端 | 测试验收标准 |
|---|------|------|------|------|-------------|
| 5.1 | 限时免费活动 | 创建活动、领取（Redis 分布式锁）、剩余份数管理、活动状态自动变更 | ✅ | ✅ | 并发领取不超过限量，活动到期自动结束 |
| 5.2 | 简易推荐 | 基于标签相似度推荐（协同过滤思想）：用户常用标签 → 推荐同类高分模板 | ✅ | ✅ | 推荐结果与用户历史使用标签相关 |
| 5.3 | 系统打磨 | 代码清理、前后端编译/build均无错误、响应式最终检查、文案统一 | ✅ | ✅ | 前后端零错误零警告 |
| 5.4 | 并发测试 | 模拟并发购买、并发领取活动的数据一致性测试 | ✅ | — | 并发场景下事务/分布式锁正确，数据无异常 |

**Phase 5 交付物**：
- 限时活动完整流程可演示
- 简易推荐功能可用
- 完整的项目源码、课程设计报告、汇报 PPT

---

> ✅ Phase 1 完成于 2026-06-28：项目骨架、数据库 DDL、用户注册登录、前端登录注册页面全部就绪，编译+测试通过。

### 6.1 各阶段 API 接口规划

#### Phase 1 API
```
POST   /api/user/register        # 用户注册
POST   /api/user/login           # 用户登录
GET    /api/user/info            # 获取当前用户信息
```

#### Phase 2 API
```
# 标签
GET    /api/tags                 # 获取标签树
POST   /api/admin/tags           # 创建标签（管理员）

# 模板
POST   /api/templates            # 创建模板
GET    /api/templates            # 模板列表（分页+筛选+排序）
GET    /api/templates/{id}       # 模板详情
PUT    /api/templates/{id}       # 编辑模板（生成新版本）
PUT    /api/templates/{id}/status # 上下架

# 版本
GET    /api/templates/{id}/versions        # 版本历史列表
GET    /api/versions/{id}                   # 版本详情
GET    /api/versions/compare?v1&v2          # 版本对比
POST   /api/templates/{id}/rollback/{vid}   # 回滚到指定版本

# 搜索
GET    /api/templates/search?keyword=xxx    # 关键字搜索
```

#### Phase 3 API
```
# 订单
POST   /api/orders               # 购买模板（事务：扣款→订单→收入→统计）
GET    /api/orders               # 我的订单列表

# 模板详情（含购买状态）
GET    /api/templates/{id}       # 返回 hasPurchased 字段；未购付费模板隐藏 promptContent

# 使用日志
POST   /api/templates/{id}/use   # 记录使用（付费模板需先购买，否则 400）

# 评价
POST   /api/reviews              # 评分评价
GET    /api/templates/{id}/reviews # 模板评价列表
DELETE /api/reviews/{id}         # 删除自己的评价

# 收藏
POST   /api/favorites/{templateId}    # 收藏/取消收藏（toggle）
GET    /api/favorites/check/{id}      # 检查收藏状态
GET    /api/favorites                 # 我的收藏

# 用户
GET    /api/user/profile/{id}    # 用户主页（含 purchasedTemplates 已购列表）
POST   /api/user/recharge        # 充值
```

#### Phase 4 API
```
GET    /api/statistics/ranking       # 热门排行
GET    /api/statistics/trend/{id}    # 模板使用趋势
GET    /api/statistics/creator       # 创作者数据看板
GET    /api/statistics/platform      # 平台总览
```

#### Phase 5 API
```
# 活动
POST   /api/admin/activities        # 创建活动
GET    /api/activities              # 活动列表
POST   /api/activities/{id}/claim   # 领取活动模板

# 推荐
GET    /api/recommend               # 获取推荐模板
```

---

## 7. 并发控制方案

### 7.1 购买扣款 — 数据库乐观锁

```sql
-- 扣减余额时使用版本号/余额条件防止超扣
UPDATE user SET balance = balance - #{amount}
WHERE id = #{userId} AND balance >= #{amount};
-- 影响行数=0 则表示余额不足
```

配合 `@Transactional` 注解和 MySQL InnoDB 行锁，保证下单+扣款+生成记录的原子性。

### 7.2 限时活动领取 — Redis 分布式锁

```java
// 使用 Redisson 分布式锁
RLock lock = redissonClient.getLock("activity:lock:" + activityId);
try {
    if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
        // 1. 检查剩余份数
        // 2. 检查是否已领取
        // 3. DECR Redis 库存
        // 4. 写入 MySQL 记录
    }
} finally {
    lock.unlock();
}
```

### 7.3 重复购买检测

```sql
-- 唯一约束保证不重复购买同一模板
-- uk_user_template 在 review 和 favorite 表中已使用
-- 对于订单，业务层先查询是否已有已完成订单
SELECT COUNT(*) FROM `order`
WHERE user_id = ? AND template_id = ? AND status = 1;
```

---

## 8. 查询优化方案

### 8.1 多条件组合搜索

```sql
-- 利用联合索引 idx_status_price + 全文索引 ft_title_desc
SELECT t.* FROM template t
INNER JOIN template_tag tt ON t.id = tt.template_id
WHERE t.status = 1
  AND t.price BETWEEN 0 AND 50
  AND tt.tag_id IN (1, 2, 3)          -- 标签筛选
  AND t.avg_rating >= 4.0             -- 评分筛选
  AND MATCH(t.title, t.description) AGAINST(#{keyword} IN BOOLEAN MODE)
GROUP BY t.id
ORDER BY t.use_count DESC              -- 按热度排序
LIMIT #{offset}, #{pageSize};
```

### 8.2 使用趋势查询

```sql
-- 利用 idx_template_created 联合索引
SELECT DATE(created_at) AS use_date,
       COUNT(*) AS daily_count
FROM usage_log
WHERE template_id = #{templateId}
  AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(created_at)
ORDER BY use_date;
```

### 8.3 热门排行

```sql
-- 综合热度分 = 使用量*0.5 + 评分*0.3 + 收藏数*0.2
SELECT t.id, t.title,
       (t.use_count * 0.5 + t.avg_rating * 10 * 0.3 + t.favorite_count * 0.2) AS hot_score
FROM template t
WHERE t.status = 1
ORDER BY hot_score DESC
LIMIT 20;
```

### 8.4 创作者等级更新存储过程

```sql
DELIMITER //
CREATE PROCEDURE update_creator_levels()
BEGIN
  UPDATE user u
  SET u.creator_level =
    CASE
      WHEN total_use >= 1000 AND avg_rating >= 4.5 THEN 5
      WHEN total_use >= 500  AND avg_rating >= 4.0 THEN 4
      WHEN total_use >= 200  AND avg_rating >= 3.5 THEN 3
      WHEN total_use >= 50   AND avg_rating >= 3.0 THEN 2
      WHEN total_use >= 10                     THEN 1
      ELSE 0
    END
  WHERE u.id IN (
    SELECT creator_id FROM (
      SELECT t.creator_id,
             SUM(t.use_count) AS total_use,
             AVG(t.avg_rating) AS avg_rating
      FROM template t
      GROUP BY t.creator_id
    ) AS stats
  );
END //
DELIMITER ;
```

---

## 9. 测试策略

### 9.1 测试分层

```
┌─────────────────────────────┐
│  E2E 测试（手动演示验收）      │  ← Phase 交付验收
├─────────────────────────────┤
│  集成测试（API 接口测试）      │  ← 关键业务流程
├─────────────────────────────┤
│  单元测试（Service/Mapper）   │  ← 核心逻辑
└─────────────────────────────┘
```

### 9.2 每阶段测试要点

| 阶段 | 测试重点 | 测试方法 |
|------|----------|----------|
| Phase 1 | 注册登录流程、Token 认证、数据库连接 | 单元测试 + 手动 |
| Phase 2 | 模板 CRUD、版本管理、搜索筛选正确性 | 单元测试 + 手动 |
| Phase 3 | **事务正确性**、余额一致性、并发购买 | 单元测试 + 并发模拟 |
| Phase 4 | SQL 统计查询正确性、图表数据一致性 | 数据对比验证 |
| Phase 5 | 分布式锁、推荐相关性、全链路验收 | 并发测试 + 手动演示 |

### 9.3 关键测试用例

| 场景 | 预期结果 |
|------|----------|
| 匿名用户查看付费模板详情 | promptContent 为空，hasPurchased=false，显示购买 CTA |
| 未购买用户查看付费模板 | promptContent 隐藏，显示锁定界面+购买按钮，"记录使用"不可见 |
| 购买后查看付费模板 | promptContent 完整显示，按钮变为"✓ 已购买"，"记录使用"可用 |
| 创作者查看自己的付费模板 | 完整内容，无需购买，hasPurchased=true |
| 未购买用户调用 recordUse API | 返回 400 "请先购买此模板" |
| 同时购买同一模板（并发） | 只有一个成功，其余提示"已购买"，余额只扣一次 |
| 活动限量 100 份，200 人并发领取 | 恰好 100 人成功，Redis 库存不超扣 |
| 编辑模板后查看历史版本 | 版本号递增，历史版本内容可查 |
| 回滚到 v2 | 当前版本变为新版本号，内容与 v2 一致 |
| 评分后查模板详情 | avg_rating 自动更新为正确均值 |

---

## 10. Bug 修复记录

> 本节记录开发过程中发现的关键 Bug 及修复方案，作为经验积累和后续开发参考。

| # | 发现日期 | 问题 | 严重程度 | 根因 | 修复方案 |
|---|----------|------|----------|------|----------|
| 1 | 06-29 | 评价时间显示错误 | 中 | MyBatis-Plus INSERT 后未回填 `createdAt`（DB 有 DEFAULT 但 MP 未自动查询） | `review.setCreatedAt(LocalDateTime.now())` 显式设置 |
| 2 | 06-29 | 无法删除自己的评价 | 中 | 前端未对接删除评价 API | TemplateDetail 增加删除按钮 + `deleteReview()` 调用 |
| 3 | 06-29 | 搜索不支持模糊匹配 | 高 | FULLTEXT `MATCH...AGAINST` 不支持部分关键字 | 新增 LIKE `%keyword%` 模糊搜索为主要策略，FULLTEXT 为补充 |
| 4 | 06-29 | "记录使用"点击后无反馈 | 中 | 仅 toast 提示，无后续交互 | 增加使用弹窗（显示 Prompt + 复制按钮） |
| 5 | 06-29 | 多页面缺少返回导航 | 低 | TemplateCreate/Login/Register 无返回链接 | 添加上下文感知的返回导航链接 |
| 6 | 06-29 | **付费模板无需购买即可查看/使用** | 🔴 严重 | `getDetail()` 和 `recordUse()` 无访问控制检查 | 详见下方 10.1 节 |

### 10.1 付费内容访问控制修复（Bug #6 详细）

**问题**：付费模板的 Prompt 内容对所有用户可见，"记录使用"功能无需购买即可调用，购买系统完全形同虚设。

**根因**：
- `TemplateServiceImpl.getDetail()` 无条件返回完整 `promptContent`
- `TemplateServiceImpl.recordUse()` 未校验用户是否已购买
- 前端无购买状态追踪，购买按钮购买后仍显示

**修复**：
- `TemplateDetailVO` 新增 `hasPurchased` 字段
- `getDetail(id, currentUserId)` 对未购买的付费模板清空 `promptContent`
- `recordUse()` 对未购买的付费模板抛出 `BusinessException`
- 前端 TemplateDetail 增加内容门控（锁定界面 + 购买 CTA）、购买状态按钮切换、购买后重新加载详情
- 个人主页新增"已购模板"Tab

**涉及文件**（8 个后端 + 4 个前端）：详见 Phase 3 访问控制修复记录。

---

## 附录：项目文件结构总览

```
prompthub/                              # 后端项目根目录
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/somehow/work/prompthub/
│   │   │   ├── PrompthubApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── service/ + impl/
│   │   │   ├── mapper/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── vo/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/
│   │           ├── schema.sql          # 完整建表 DDL
│   │           └── seed.sql            # 测试种子数据
│   └── test/java/
│
└── DESIGN.md                           # 本文档

prompthub-frontend/                        # 前端项目根目录
├── package.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.ts
    ├── App.vue
    ├── router/index.ts
    ├── stores/
    ├── api/
    ├── views/
    ├── components/
    └── utils/
```

---

> **当前进度**：Phase 1 ✅ → Phase 2 ✅ → Phase 3 ✅（含访问控制修复）→ **Phase 4 待开始**。
>
> 已验证：后端 62 个源文件编译通过 + 1 个测试通过，前端 1694 个模块构建成功。数据库 11 张表 + 种子数据就位。
