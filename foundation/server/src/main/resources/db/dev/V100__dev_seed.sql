-- 仅 dev profile 加载。虚构组织和党员，不是旧 PartyBuilding 导出的历史人员。
-- 不在这里写登录账号或口令。开发账号由应用按环境变量 DEV_ADMIN_PASSWORD 生成。

INSERT INTO party_org (id, parent_id, name, org_type, sort_no) VALUES
    (1, NULL, '学校党委', 'COMMITTEE', 1),
    (2, 1, '第一党总支', 'GENERAL_BRANCH', 1),
    (3, 1, '第二党总支', 'GENERAL_BRANCH', 2),
    (4, 2, '第一党总支第一党支部', 'BRANCH', 1),
    (5, 2, '第一党总支第二党支部', 'BRANCH', 2),
    (6, 3, '第二党总支第一党支部', 'BRANCH', 1),
    (7, 1, '党委直属党支部', 'BRANCH', 3);

INSERT INTO party_member (name, gender, phone, member_no, org_id, status, joined_at) VALUES
    ('张一', '男', '13800000001', 'DY-004-001', 4, 'ACTIVE', '2018-07-01'),
    ('李二', '女', '13800000002', 'DY-005-001', 5, 'ACTIVE', '2019-07-01'),
    ('王三', '男', '13800000003', 'DY-006-001', 6, 'ACTIVE', '2020-07-01'),
    ('赵四', '女', '13800000004', 'DY-007-001', 7, 'ACTIVE', '2021-07-01');
