-- TOYOU MySQL Schema

CREATE TABLE IF NOT EXISTS `user`
(
    uid        BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'uid' PRIMARY KEY,
    username   VARCHAR(16)                        NOT NULL COMMENT '用户名',
    password   VARCHAR(256)                       NOT NULL COMMENT '密码',
    email      VARCHAR(32)                        NULL COMMENT '邮箱',
    emailRaw   VARCHAR(32)                        NOT NULL COMMENT '邮箱原始地址',
    phone      VARCHAR(16)                        NULL COMMENT '手机号',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

-- 定义管理员权限为*，即所有权限

CREATE TABLE IF NOT EXISTS `permission`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    permission VARCHAR(32)                        NOT NULL COMMENT '权限',
    expiry     BIGINT   DEFAULT 0                 NOT NULL COMMENT '过期时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `picture`
(
    pid        VARCHAR(32)                        NOT NULL COMMENT 'pid-md5' PRIMARY KEY,
    original   TEXT                               NOT NULL COMMENT '原图路径',
    thumbnail  TEXT                               NOT NULL COMMENT '缩略图路径',
    watermark  TEXT                               NOT NULL COMMENT '水印图路径',
    size       BIGINT                             NOT NULL COMMENT '大小(单位：字节)',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `user_picture`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    pid        VARCHAR(32)                        NOT NULL COMMENT 'pid-md5',
    fileName   VARCHAR(128)                       NOT NULL COMMENT '文件名',
    downloads  BIGINT   DEFAULT 0                 NOT NULL COMMENT '下载次数',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `share`
(
    sid        VARCHAR(36)                        NOT NULL COMMENT 'sid-uuid' PRIMARY KEY,
    id         BIGINT                             NOT NULL COMMENT 'user_picture的id',
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    password   VARCHAR(32)                                 COMMENT '密码',
    downloads  BIGINT   DEFAULT 0                 NOT NULL COMMENT '下载次数',
    shareMode  INTEGER  DEFAULT 0                 NOT NULL COMMENT '分享模式',
    expiry     BIGINT   DEFAULT 0                 NOT NULL COMMENT '过期时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `orders`
(
    outTradeNo    VARCHAR(128)                       NOT NULL COMMENT '订单号' PRIMARY KEY,
    uid           BIGINT                             NOT NULL COMMENT 'uid',
    subject       VARCHAR(128)                       NOT NULL COMMENT '订单标题',
    tradeNo       VARCHAR(128)                       NULL COMMENT '系统交易号',
    totalAmount   BIGINT                             NOT NULL COMMENT '订单金额(单位：分)',
    receiptAmount BIGINT                             NULL COMMENT '实收金额(单位：分)',
    payPlatform   INTEGER  DEFAULT 0                 NOT NULL COMMENT '支付平台',
    tradeStatus   INTEGER  DEFAULT 0                 NOT NULL COMMENT '交易状态',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available     BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

CREATE TABLE IF NOT EXISTS `oauth`
(
    id         BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'id' PRIMARY KEY,
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    platform   INTEGER                            NOT NULL COMMENT '平台',
    openId     VARCHAR(64)                        NOT NULL COMMENT 'openId',
    token      VARCHAR(64)                        NOT NULL COMMENT 'token',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);

-- 实名认证数据表
CREATE TABLE IF NOT EXISTS `real_name`
(
    uid        BIGINT                             NOT NULL COMMENT 'uid' PRIMARY KEY,
    name       VARCHAR(32)                        NOT NULL COMMENT '姓名',
    idCard     VARCHAR(32)                        NOT NULL COMMENT '身份证号',
    pass       BOOL     DEFAULT FALSE             NOT NULL COMMENT '是否通过',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);