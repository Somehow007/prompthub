-- ============================================================
-- 智享AI提示词平台 — 完整建表 DDL
-- 数据库: prompthub
-- 设计依据: 3NF 范式 + 实际查询性能权衡
-- ============================================================

CREATE DATABASE IF NOT EXISTS prompthub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE prompthub;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`      VARCHAR(50)  NOT NULL COMMENT '用户名，唯一',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希（bcrypt）',
  `email`         VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar_url`    VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `creator_level` TINYINT      NOT NULL DEFAULT 0 COMMENT '创作者等级（0-5，系统自动计算）',
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
-- 2. 标签表（支持层级关系）
-- ============================================================
CREATE TABLE IF NOT EXISTS `tag` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name`       VARCHAR(50) NOT NULL COMMENT '标签名称',
  `parent_id`  BIGINT      DEFAULT NULL COMMENT '父标签ID，NULL表示顶级',
  `level`      TINYINT     NOT NULL DEFAULT 1 COMMENT '层级',
  `sort_order` INT         NOT NULL DEFAULT 0 COMMENT '排序序号',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  CONSTRAINT `fk_tag_parent` FOREIGN KEY (`parent_id`) REFERENCES `tag`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表（支持层级关系）';

-- ============================================================
-- 3. 模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS `template` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `creator_id`      BIGINT        NOT NULL COMMENT '创作者用户ID',
  `title`           VARCHAR(200)  NOT NULL COMMENT '模板标题',
  `description`     VARCHAR(2000) DEFAULT NULL COMMENT '适用场景描述',
  `cover_url`       VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
  `current_version` INT           NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `price`           DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格（0=免费）',
  `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1-上架 0-下架 2-审核中',
  `use_count`       BIGINT        NOT NULL DEFAULT 0 COMMENT '总使用次数',
  `use_count_7d`    INT           NOT NULL DEFAULT 0 COMMENT '近7天使用次数',
  `favorite_count`  INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `review_count`    INT           NOT NULL DEFAULT 0 COMMENT '评价数',
  `avg_rating`      DECIMAL(2,1)  NOT NULL DEFAULT 0.0 COMMENT '平均评分',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
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
-- 4. 模板版本表
-- ============================================================
CREATE TABLE IF NOT EXISTS `template_version` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `template_id`     BIGINT        NOT NULL COMMENT '模板ID',
  `version_number`  INT           NOT NULL COMMENT '版本号',
  `prompt_content`  TEXT          NOT NULL COMMENT 'Prompt内容',
  `change_note`     VARCHAR(1000) DEFAULT NULL COMMENT '变更说明',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_version` (`template_id`, `version_number`),
  KEY `idx_template_id` (`template_id`),
  CONSTRAINT `fk_version_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本表';

-- ============================================================
-- 5. 模板-标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `template_tag` (
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
-- 6. 评分评价表
-- ============================================================
CREATE TABLE IF NOT EXISTS `review` (
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
-- 7. 订单表
-- ============================================================
CREATE TABLE IF NOT EXISTS `order` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`    VARCHAR(32)    NOT NULL COMMENT '订单编号',
  `user_id`     BIGINT         NOT NULL COMMENT '用户ID',
  `template_id` BIGINT         NOT NULL COMMENT '模板ID',
  `amount`      DECIMAL(10,2)  NOT NULL COMMENT '交易金额',
  `status`      TINYINT        NOT NULL DEFAULT 0 COMMENT '0-待支付 1-已完成 2-已取消 3-已退款',
  `pay_time`    DATETIME       DEFAULT NULL COMMENT '支付时间',
  `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  CONSTRAINT `fk_order_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 8. 使用日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `usage_log` (
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
-- 9. 活动模板表
-- ============================================================
CREATE TABLE IF NOT EXISTS `activity_template` (
  `id`              BIGINT    NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  `template_id`     BIGINT    NOT NULL COMMENT '模板ID',
  `start_time`      DATETIME  NOT NULL COMMENT '开始时间',
  `end_time`        DATETIME  NOT NULL COMMENT '结束时间',
  `total_quota`     INT       NOT NULL COMMENT '限量总份数',
  `remaining_quota` INT       NOT NULL COMMENT '剩余份数',
  `status`          TINYINT   NOT NULL DEFAULT 1 COMMENT '1-进行中 0-已结束 2-未开始',
  `created_at`      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`),
  CONSTRAINT `fk_activity_template` FOREIGN KEY (`template_id`) REFERENCES `template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时活动模板表';

-- ============================================================
-- 10. 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `favorite` (
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
-- 11. 收入明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS `income_record` (
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

-- ============================================================
-- 种子数据：初始化标签体系
-- ============================================================
INSERT INTO `tag` (`name`, `parent_id`, `level`, `sort_order`) VALUES
('写作',   NULL, 1, 1),
('翻译',   NULL, 1, 2),
('编程',   NULL, 1, 3),
('设计',   NULL, 1, 4),
('教育',   NULL, 1, 5),
('商业',   NULL, 1, 6),
('Python',  3, 2, 1),
('Java',    3, 2, 2),
('JavaScript', 3, 2, 3),
('Go',      3, 2, 4),
('文案写作', 1, 2, 1),
('小说创作', 1, 2, 2),
('论文润色', 1, 2, 3),
('中译英',   2, 2, 1),
('英译中',   2, 2, 2),
('UI设计',   4, 2, 1),
('Logo设计', 4, 2, 2),
('教案设计', 5, 2, 1),
('题目生成', 5, 2, 2),
('商业计划', 6, 2, 1),
('营销文案', 6, 2, 2);
