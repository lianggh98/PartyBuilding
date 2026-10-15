-- 这个目录只在数据目录为空时由 MySQL 官方镜像执行一次。
-- foundation 的表结构不放在这里。旧 PartyBuilding 的 14 张表脚本已移除。
-- 表、开发组织和开发账号由后端第一次启动时的 Flyway 与 dev profile 创建。
SELECT 1;
