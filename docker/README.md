# 本地 Docker 中间件

MySQL 8.4 与 Redis 只给本机开发用。Compose 不启动 Java 或 Vue。Redis 已预先拉起，当前 foundation 业务代码不连接 Redis。

## 启动

在仓库根目录：

```bash
cp .env.example .env
# 编辑 .env，把所有占位口令换成至少 12 位的本机口令。DEV_ADMIN_PASSWORD 不能使用 admin123 一类弱口令。
# 保留双引号。DB_URL 里的 & 不加引号时，source .env 会失败。
docker compose config
docker compose up -d
docker compose ps
```

端口只绑定 `127.0.0.1`。当前机器上 `25481` 和 `26381` 空闲，因此继续使用这两个端口。若以后被占用，只改 `.env` 里的 `MYSQL_PORT` 或 `REDIS_PORT`，容器内部仍是 3306 和 6379。

| 服务 | 镜像 | 本机地址 | 持久化目录 |
| --- | --- | --- | --- |
| MySQL | `mysql:8.4` | `127.0.0.1:25481` | `docker/mysql/data/` |
| Redis | `redis:latest` | `127.0.0.1:26381` | `docker/redis/data/` |

## 数据库初始化

旧脚本 `01_partybuilding_schema.sql` 已删除。那是原 PartyBuilding 的 14 张表，不能用来初始化 foundation。

正式结构和开发数据分开：

| 内容 | 位置 | 何时执行 |
| --- | --- | --- |
| 表结构：`party_org`、`sys_user`、`party_member` | `foundation/server/src/main/resources/db/migration/V1__schema.sql` | 后端第一次连接空库时，Flyway 自动执行 |
| 虚构组织和党员 | `foundation/server/src/main/resources/db/dev/V100__dev_seed.sql` | 仅 `SPRING_PROFILES_ACTIVE=dev` |
| 开发账号 | `DevUserSeeder` | 仅 dev profile，口令来自 `DEV_ADMIN_PASSWORD`，启动时做 BCrypt，不写入仓库 |

`sys_user.role_code` 保存角色。权限字在服务端按角色计算，不把角色放进请求体。开发账号有 `admin`、`committee_admin`、`general_admin`、`branch_admin`、`branch_viewer`。正式环境不要启用 `dev` profile，因此不会创建这些固定账号，也不会带入弱口令。

`docker/mysql/init/` 只在 `docker/mysql/data/` 为空时运行一次，现在不做建表。已有数据目录时，重启不会重跑初始化，也不会清空库。

第一次准备好库：

```bash
cd foundation/server
set -a && source ../../.env && set +a
mvn -DskipTests spring-boot:run
```

后端监听 `18080`。

## 检查

```bash
docker compose exec mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -h 127.0.0.1 -u "$MYSQL_USER" "$MYSQL_DATABASE" -e "SHOW TABLES;"'
docker compose exec redis redis-cli ping
```

Flyway 跑完后应看到 `flyway_schema_history`、`party_org`、`party_member`、`sys_user`。Redis 返回 `PONG`。

接口回归使用本机口令，不要把口令写进命令历史以外的文件：

```bash
DEMO_PASSWORD="$DEV_ADMIN_PASSWORD" bash foundation/scripts/verify.sh
```

## 停止

```bash
docker compose down
```

这不会删除 `docker/mysql/data/` 和 `docker/redis/data/`。只有故意删掉这两个目录，才会在下次启动时变成空库。
