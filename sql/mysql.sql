-- TOYOU MySQL Schema

CREATE TABLE IF NOT EXISTS `user`
(
    uid        BIGINT AUTO_INCREMENT              NOT NULL COMMENT 'uid' PRIMARY KEY,
    username   VARCHAR(16)                        NOT NULL COMMENT '用户名',
    password   VARCHAR(256)                       NOT NULL COMMENT '密码',
    email      VARCHAR(32)                        NOT NULL COMMENT '邮箱',
    emailRaw   VARCHAR(32)                        NOT NULL COMMENT '邮箱原始地址',
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
    uuid       VARCHAR(36)                        NOT NULL COMMENT '下载uuid',
    uid        BIGINT                             NOT NULL COMMENT 'uid',
    pid        VARCHAR(32)                        NOT NULL COMMENT 'pid-md5',
    fileName   VARCHAR(128)                       NOT NULL COMMENT '文件名',
    downloads  BIGINT   DEFAULT 0                 NOT NULL COMMENT '下载次数',
    isPublic   BOOL     DEFAULT FALSE             NOT NULL COMMENT '是否公开',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    available  BOOL     DEFAULT TRUE              NOT NULL COMMENT '是否可用'
);