-- ============================================================
-- AI Resume Agent 数据库建表脚本（初稿）
-- 数据库：MySQL 8.x，字符集 utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_resume_agent`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ai_resume_agent`;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username`        VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`        VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`          VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `school`          VARCHAR(100) DEFAULT NULL COMMENT '学校',
    `major`           VARCHAR(100) DEFAULT NULL COMMENT '专业',
    `education`       VARCHAR(20)  DEFAULT NULL COMMENT '学历（本科/硕士/博士）',
    `graduation_year` INT          DEFAULT NULL COMMENT '毕业年份',
    `job_intention`   VARCHAR(255) DEFAULT NULL COMMENT '求职意向',
    `role`            TINYINT      NOT NULL DEFAULT 0 COMMENT '0=学生，1=管理员',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '0=禁用，1=正常',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 简历表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `resume` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '简历 ID',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户',
    `file_name`      VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_url`       VARCHAR(255) NOT NULL COMMENT '存储路径',
    `file_type`      VARCHAR(20)  DEFAULT NULL COMMENT '文件类型（pdf/doc/docx/txt）',
    `file_size`      BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    `parsed_content` LONGTEXT     DEFAULT NULL COMMENT '解析出的简历文本',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待解析，1=解析中，2=已完成，3=失败',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '简历表';

-- ------------------------------------------------------------
-- 简历分析报告表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `resume_analysis` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '分析 ID',
    `resume_id`        BIGINT   NOT NULL COMMENT '关联简历',
    `total_score`      INT      DEFAULT NULL COMMENT '综合评分（0~100）',
    `dimension_scores` JSON     DEFAULT NULL COMMENT '各维度评分',
    `strengths`        TEXT     DEFAULT NULL COMMENT '优点',
    `weaknesses`       TEXT     DEFAULT NULL COMMENT '不足',
    `suggestions`      TEXT     DEFAULT NULL COMMENT '改进建议',
    `raw_ai_response`  LONGTEXT DEFAULT NULL COMMENT 'AI 原始返回',
    `status`           TINYINT  NOT NULL DEFAULT 1 COMMENT '1=成功，0=失败',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_resume_id` (`resume_id`)
) ENGINE = InnoDB COMMENT = '简历分析报告表';

-- ------------------------------------------------------------
-- 岗位表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `job` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '岗位 ID',
    `title`                 VARCHAR(100) NOT NULL COMMENT '岗位名称',
    `company`               VARCHAR(100) NOT NULL COMMENT '公司名称',
    `industry`              VARCHAR(50)  DEFAULT NULL COMMENT '所属行业',
    `location`              VARCHAR(100) DEFAULT NULL COMMENT '工作地点',
    `salary_min`            INT          DEFAULT NULL COMMENT '薪资下限（K）',
    `salary_max`            INT          DEFAULT NULL COMMENT '薪资上限（K）',
    `education_requirement` VARCHAR(20)  DEFAULT NULL COMMENT '学历要求',
    `experience_requirement` VARCHAR(20) DEFAULT NULL COMMENT '经验要求',
    `skills`                JSON         DEFAULT NULL COMMENT '技能要求列表',
    `job_description`       LONGTEXT     DEFAULT NULL COMMENT '岗位描述',
    `status`                TINYINT      NOT NULL DEFAULT 1 COMMENT '0=下架，1=上架',
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_company` (`company`),
    KEY `idx_location` (`location`)
) ENGINE = InnoDB COMMENT = '岗位表';

-- ------------------------------------------------------------
-- 岗位匹配记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `job_match` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '匹配 ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户',
    `job_id`       BIGINT   NOT NULL COMMENT '岗位',
    `match_score`  INT      DEFAULT NULL COMMENT '匹配度（0~100）',
    `match_reasons` JSON    DEFAULT NULL COMMENT '匹配理由列表',
    `skill_gaps`   JSON     DEFAULT NULL COMMENT '技能差距列表',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE = InnoDB COMMENT = '岗位匹配记录表';

-- ------------------------------------------------------------
-- 面试会话表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话 ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户',
    `job_id`         BIGINT       DEFAULT NULL COMMENT '关联岗位（可选）',
    `interview_type` VARCHAR(50)  NOT NULL COMMENT '类型：general/technical/behavioral',
    `title`          VARCHAR(100) DEFAULT NULL COMMENT '会话标题',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=进行中，1=已完成，2=已中断',
    `current_round`  INT          NOT NULL DEFAULT 0 COMMENT '当前轮次',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE = InnoDB COMMENT = '面试会话表';

-- ------------------------------------------------------------
-- 面试消息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interview_message` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
    `session_id` BIGINT   NOT NULL COMMENT '所属会话',
    `role`       VARCHAR(10) NOT NULL COMMENT 'user / assistant',
    `content`    LONGTEXT NOT NULL COMMENT '消息内容',
    `round`      INT      DEFAULT NULL COMMENT '所属轮次',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB COMMENT = '面试消息表';

-- ------------------------------------------------------------
-- 面试报告表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `interview_report` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '报告 ID',
    `session_id`       BIGINT   NOT NULL COMMENT '关联会话',
    `total_score`      INT      DEFAULT NULL COMMENT '综合评分',
    `dimension_scores` JSON     DEFAULT NULL COMMENT '维度评分',
    `question_reviews` JSON     DEFAULT NULL COMMENT '逐题点评',
    `summary`          TEXT     DEFAULT NULL COMMENT '整体总结',
    `suggestions`      TEXT     DEFAULT NULL COMMENT '改进建议',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`)
) ENGINE = InnoDB COMMENT = '面试报告表';

-- ------------------------------------------------------------
-- 项目优化记录表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `project_optimization` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '优化 ID',
    `user_id`           BIGINT       NOT NULL COMMENT '用户',
    `project_name`      VARCHAR(100) DEFAULT NULL COMMENT '项目名称',
    `role`              VARCHAR(50)  DEFAULT NULL COMMENT '担任角色',
    `original_content`  TEXT         NOT NULL COMMENT '用户原始描述',
    `optimized_content` TEXT         DEFAULT NULL COMMENT 'STAR 优化后文案',
    `star_content`      JSON         DEFAULT NULL COMMENT 'STAR 四要素拆解',
    `suggestions`       TEXT         DEFAULT NULL COMMENT '量化与关键词建议',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '项目优化记录表';

-- ------------------------------------------------------------
-- AI 配置表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
    `config_key`   VARCHAR(50)  NOT NULL COMMENT '配置键',
    `config_value` TEXT         DEFAULT NULL COMMENT '配置值（敏感值加密存储）',
    `description`  VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB COMMENT = 'AI 配置表';

-- ------------------------------------------------------------
-- 初始管理员账号
-- 建议方式：先通过注册接口创建普通账号，再手动将 role 更新为 1；
-- 或使用 Spring Security 的 BCryptPasswordEncoder 生成真实哈希后手动插入。
-- 示例（勿直接使用，哈希需真实生成）：
-- INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`)
-- VALUES ('admin', '<BCRYPT_HASH>', 'admin@example.com', 1, 1);
-- ------------------------------------------------------------
