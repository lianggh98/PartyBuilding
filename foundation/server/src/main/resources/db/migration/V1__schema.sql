-- 正式结构。不包含账号、口令或人员种子。
-- 角色和权限由应用根据 sys_user.role_code 计算，不另建可被请求参数改写的权限表。

CREATE TABLE party_org (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    org_type VARCHAR(32) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_org_parent FOREIGN KEY (parent_id) REFERENCES party_org (id)
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    org_id BIGINT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT fk_sys_user_org FOREIGN KEY (org_id) REFERENCES party_org (id)
);

CREATE TABLE party_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(16) NULL,
    phone VARCHAR(32) NULL,
    member_no VARCHAR(64) NULL,
    org_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_party_member_org FOREIGN KEY (org_id) REFERENCES party_org (id)
);

CREATE INDEX idx_party_member_org ON party_member (org_id);
