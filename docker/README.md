# Local Docker middleware (MySQL 8.4 + Redis latest)

## What this repository actually uses

- **Original PartyBuilding** (Spring Boot + Vue) needs **MySQL**. The original `BackEnd/pom.xml` contains a MySQL driver but no Redis starter.
- **Redis** is provisioned for the next-phase foundation work and is **not connected automatically** to the original backend.
- PostgreSQL, Kafka, Mailpit etc. visible in Docker Desktop are not dependencies proven by this repository.

This Compose stack is strictly for **local development**, and does not start the Java or Vue application.

## Start

From the **repository root**:

```bash
cp .env.example .env
# Edit .env and replace all three placeholder passwords with your own strong local passwords.
# Check host ports if another Docker project is already running:
# lsof -nP -iTCP:25481 -sTCP:LISTEN
# lsof -nP -iTCP:26381 -sTCP:LISTEN
docker compose up -d
docker compose ps
```

The defaults bind **only to the local machine**:

| Service | Image | Host | Container port | Persistence |
| --- | --- | --- | --- | --- |
| MySQL | `mysql:8.4` | `127.0.0.1:25481` | `3306` | `docker/mysql/data/` |
| Redis | `redis:latest` | `127.0.0.1:26381` | `6379` | `docker/redis/data/` |

If either host port is occupied, edit `MYSQL_PORT` or `REDIS_PORT` in `.env` and run `docker compose up -d` again. **The internal container ports stay 3306 and 6379.**

## MySQL auto-initialization

- At **first start, with an empty `docker/mysql/data/`**, MySQL creates the database and user from `.env`, then executes every `*.sql` inside `docker/mysql/init/`.
- `docker/mysql/init/01_partybuilding_schema.sql` includes **14 original PartyBuilding table definitions only**, extracted from `SQL/partybuilding.sql`. It intentionally excludes historical `INSERT` rows, which contain old user information and password hashes.
- Therefore the tables start **empty**, with **no predefined user or login credentials**.
- This is **not** the database schema for RuoYi-Vue-Pro. The new foundation branch may require its own migration / official initializer; validate its expected schema before switching backend to this database.
- Initialization scripts are **not re-run** when a persisted data directory already exists. Use migrations for later schema changes; back up data before any reset.

Optional checks:

```bash
docker compose exec mysql sh -lc 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -h 127.0.0.1 -u "$MYSQL_USER" "$MYSQL_DATABASE" -e "SHOW TABLES;"'
docker compose exec redis redis-cli ping
```

Expected: 14 MySQL table names and Redis `PONG`.

## Connect a local Spring Boot backend

For the **original** PartyBuilding app, override the unsafe committed datasource settings using environment variables. For example, after loading the locally edited `.env`, configure:

- JDBC URL: `jdbc:mysql://127.0.0.1:25481/partybuilding?characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`
- Username/password: the `MYSQL_USER` and `MYSQL_PASSWORD` values in `.env`.
- Redis (if using the new foundation): `127.0.0.1:26381`, using `REDIS_PASSWORD`.

Never connect to the public MySQL URL in the original application properties; rotate any old exposed credentials.

## Stop

```bash
docker compose down
```

This keeps the bind-mounted MySQL and Redis data under `docker/`. Do not delete those directories unless you have backups and intentionally want an empty database.
