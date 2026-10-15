-- 本地演示种子。登录口令统一为 admin123，仅用于本机验证，不得用于部署。
-- 口令以 BCrypt 存储，接口不返回散列。

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

INSERT INTO party_org (id, parent_id, name, org_type, sort_no) VALUES
    (1, NULL, '学校党委', 'COMMITTEE', 1),
    (2, 1, '第一党总支', 'GENERAL_BRANCH', 1),
    (3, 1, '第二党总支', 'GENERAL_BRANCH', 2),
    (4, 2, '第一党总支第一党支部', 'BRANCH', 1),
    (5, 2, '第一党总支第二党支部', 'BRANCH', 2),
    (6, 3, '第二党总支第一党支部', 'BRANCH', 1),
    (7, 1, '党委直属党支部', 'BRANCH', 3);

INSERT INTO sys_user (username, password_hash, display_name, role_code, org_id) VALUES
    ('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'SYSTEM_ADMIN', NULL),
    ('committee_admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '学校党委管理员', 'COMMITTEE_ADMIN', 1),
    ('general_admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '第一党总支管理员', 'GENERAL_ADMIN', 2),
    ('branch_admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '第一党支部管理员', 'BRANCH_ADMIN', 4),
    ('branch_viewer', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '第一党支部只读', 'BRANCH_VIEWER', 4);

INSERT INTO party_member (name, gender, phone, member_no, org_id, status, joined_at) VALUES
    ('张一', '男', '13800000001', 'DY-004-001', 4, 'ACTIVE', '2018-07-01'),
    ('李二', '女', '13800000002', 'DY-005-001', 5, 'ACTIVE', '2019-07-01'),
    ('王三', '男', '13800000003', 'DY-006-001', 6, 'ACTIVE', '2020-07-01'),
    ('赵四', '女', '13800000004', 'DY-007-001', 7, 'ACTIVE', '2021-07-01');
