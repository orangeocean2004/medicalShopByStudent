-- ============================================================
-- 线上购药系统 建表脚本（SQLite 方言）
-- 对应《线上购药系统-数据库设计》16 张表，满足 3NF。
-- SQLite 说明：
--   * 主键用 INTEGER PRIMARY KEY AUTOINCREMENT（等价于文档的 BIGINT 自增）
--   * 金额用 DECIMAL(10,2)（SQLite 以 NUMERIC 亲和类型存储）
--   * 状态/类型字段用 INTEGER 枚举，取值见注释
--   * 时间统一用 DATETIME，默认 CURRENT_TIMESTAMP
--   * 全部 CREATE TABLE IF NOT EXISTS，脚本可重复执行（幂等）
-- ============================================================

-- ---------- P3 用户与权限 ----------

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    phone           VARCHAR(20)  NOT NULL UNIQUE,             -- 手机号（登录账号）
    password_hash   VARCHAR(128) NOT NULL,                    -- BCrypt 密码哈希
    nickname        VARCHAR(50),
    role            INTEGER      NOT NULL DEFAULT 0,           -- 0消费者 1执业药师 2客服 3运营
    health_profile  TEXT,                                     -- 健康档案 JSON（授权后填充）
    last_active_at  DATETIME,                                 -- 最近活跃时间（坐席在线判定：超时即视为离线）
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 收货地址表
CREATE TABLE IF NOT EXISTS user_address (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER      NOT NULL,
    receiver    VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    region      VARCHAR(100) NOT NULL,                        -- 省市区
    detail      VARCHAR(200) NOT NULL,                        -- 详细地址
    is_default  INTEGER      DEFAULT 0,                        -- 0否 1是
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 隐私授权表（FP-REC-04，约束 C-3）
CREATE TABLE IF NOT EXISTS privacy_authorization (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER      NOT NULL,
    scope           VARCHAR(100) NOT NULL,                    -- 授权范围：recommendation / healthProfile
    status          INTEGER      NOT NULL DEFAULT 1,          -- 1已授权 0已撤回
    authorized_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      DATETIME
);

-- ---------- M1 基础购药 ----------

-- 药品表
CREATE TABLE IF NOT EXISTS drug (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              VARCHAR(100)  NOT NULL,
    category          VARCHAR(50),                            -- 分类：感冒/慢病等
    is_rx             INTEGER       NOT NULL DEFAULT 0,        -- 0非处方 1处方
    price             DECIMAL(10,2) NOT NULL,
    stock             INTEGER       NOT NULL DEFAULT 0,
    indication        TEXT,                                   -- 适应症
    dosage            TEXT,                                   -- 用法用量
    contraindication  TEXT,                                   -- 禁忌
    image_url         VARCHAR(255),
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_drug_name ON drug(name);

-- 购物车项表
CREATE TABLE IF NOT EXISTS cart_item (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    drug_id     INTEGER NOT NULL,
    quantity    INTEGER NOT NULL DEFAULT 1,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER       NOT NULL,
    address_id    INTEGER       NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    status        INTEGER       NOT NULL DEFAULT 0,           -- 0待支付 1已支付 2配送中 3已完成 4已取消
    logistics_no  VARCHAR(50),
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_item (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id    INTEGER       NOT NULL,
    drug_id     INTEGER       NOT NULL,
    quantity    INTEGER       NOT NULL,
    unit_price  DECIMAL(10,2) NOT NULL                        -- 下单时价格快照
);

-- 支付记录表
CREATE TABLE IF NOT EXISTS payment (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id  INTEGER       NOT NULL,
    amount    DECIMAL(10,2) NOT NULL,
    channel   INTEGER       NOT NULL DEFAULT 1,               -- 1微信 2支付宝 3银行卡
    status    INTEGER       NOT NULL DEFAULT 0,               -- 0待支付 1成功 2失败
    paid_at   DATETIME
);

-- ---------- M2 智能药师咨询 ----------

-- 咨询会话表
CREATE TABLE IF NOT EXISTS consultation (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER      NOT NULL,
    symptom         VARCHAR(500),                             -- 首次症状描述
    transferred     INTEGER      DEFAULT 0,                   -- 是否转人工 0否 1是（FP-AI-04）
    pharmacist_id   INTEGER,                                  -- 接手药师
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 咨询消息表（FP-AI-05 留存）
CREATE TABLE IF NOT EXISTS consultation_message (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    consultation_id   INTEGER      NOT NULL,
    sender_type       INTEGER      NOT NULL,                  -- 0用户 1AI 2药师
    content           TEXT         NOT NULL,
    ai_source         VARCHAR(100),                           -- AI 回复的知识来源标识
    confidence        DECIMAL(4,3),                           -- AI 置信度 0~1（FP-AI-03）
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- M3 个性化推荐 ----------

-- 续方提醒表（FP-REC-01）
CREATE TABLE IF NOT EXISTS refill_reminder (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER  NOT NULL,
    drug_id           INTEGER  NOT NULL,
    next_remind_date  DATE     NOT NULL,
    cycle_days        INTEGER  NOT NULL,
    status            INTEGER  DEFAULT 1                       -- 1生效 0关闭
);

-- 推荐记录表（FP-REC-03）
CREATE TABLE IF NOT EXISTS recommendation (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER      NOT NULL,
    drug_id     INTEGER      NOT NULL,
    reason      VARCHAR(200),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- M4 处方辅助审核 ----------

-- 处方表（FP-RX-01）
CREATE TABLE IF NOT EXISTS prescription (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER      NOT NULL,
    drug_id     INTEGER,                                      -- 关联的处方药（购买该药的凭证）
    image_url   TEXT         NOT NULL,                        -- 处方图片（base64 data URL，免依赖对象存储）
    ai_result   TEXT,                                         -- AI 预审结果 JSON
    status      INTEGER      NOT NULL DEFAULT 0,              -- 0待审 1通过 2驳回
    review_comment VARCHAR(200),                              -- 药师审核备注
    reviewer_id INTEGER,                                      -- 审核药师
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME
);

-- 处方复核记录表
CREATE TABLE IF NOT EXISTS prescription_review (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    prescription_id   INTEGER      NOT NULL,
    pharmacist_id     INTEGER      NOT NULL,
    result            INTEGER      NOT NULL,                  -- 1通过 2驳回
    comment           VARCHAR(200),
    reviewed_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- M5 智能客服 / P2 人工兜底 ----------

-- 客服会话表（FP-CS-01）
CREATE TABLE IF NOT EXISTS cs_session (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER  NOT NULL,
    order_id      INTEGER,                                    -- 关联订单（可空）
    transferred   INTEGER  DEFAULT 0,                         -- 是否转人工（FP-CS-02）
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 人工兜底工单表（公共模块 P2，约束 C-4）
CREATE TABLE IF NOT EXISTS handoff_ticket (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    source_type   INTEGER      NOT NULL,                      -- 1药师咨询 2客服 3处方
    source_id     INTEGER      NOT NULL,
    user_id       INTEGER      NOT NULL,
    agent_id      INTEGER,                                    -- 接手坐席（药师/客服）
    context       TEXT,                                       -- 携带的对话上下文 JSON
    reason        VARCHAR(100),                               -- 触发原因
    status        INTEGER      DEFAULT 0,                     -- 0待接 1处理中 2已完成
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- P1 AI 中台知识库 ----------

-- 药学知识条目表（约束 C-2，RAG 检索用）
CREATE TABLE IF NOT EXISTS knowledge_item (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    drug_id     INTEGER,                                      -- 关联药品（可空）
    source      VARCHAR(200),                                 -- 权威来源（说明书/指南）
    keywords    VARCHAR(255),                                 -- 检索关键词（mock 版以此做 RAG 召回）
    vector_id   VARCHAR(64),                                  -- 向量库中的向量ID
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------- M6 智慧运营 ----------

-- 营销文案表（FP-OPS-02）
CREATE TABLE IF NOT EXISTS marketing_copy (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id   INTEGER      NOT NULL,
    prompt        VARCHAR(500),
    content       TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
