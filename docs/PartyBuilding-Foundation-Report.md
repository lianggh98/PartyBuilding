# 智慧党建平台：第二阶段技术底座验证报告

> 日期：2026-10-15  
> 分支：`foundation/party-base-validation`  
> 前提：第一阶段报告建议方案 B。本阶段只建立可运行的单体底座，不开发其余 18 项业务，不开发移动端，不合并 `main`。

## 1. 实际运行

本机 JDK 17.0.18。后端 `foundation/server` 使用 Spring Boot 3.3.6、Tomcat 10.1.33、MyBatis-Plus 3.5.9、Flyway。前端 `foundation/web` 使用 Vite 6.4.4、Vue 3.5、Element Plus 2.9。数据库是本机 Docker 中的 MySQL 8.0，库名 `party_foundation`，只监听 `127.0.0.1:13309`。

数据库口令和 JWT 密钥通过环境变量 `DB_PASSWORD`、`JWT_SECRET` 注入，没有写进仓库。当时的种子口令写在 `V1__init.sql`。整合 Docker 时该文件已删除，正式迁移不再包含账号或口令。见第 7 节。

服务端口用 `18080`。本机 `127.0.0.1:8080` 已被其他进程占用，若沿用 8080，请求到不了这个应用。

```text
cd foundation/server && mvn -DskipTests package
DB_PASSWORD=... JWT_SECRET=... java -jar target/foundation-server-0.1.0.jar
```

结果：Flyway 将空库迁移到 version 1。日志 `Started FoundationApplication in 1.485 seconds`，Tomcat 监听 `18080`。关闭了 Spring Boot 默认的内存用户自动配置，启动日志里没有再生成一把临时口令。

```text
cd foundation/web && npm install && npm run dev
```

Vite 在 `http://127.0.0.1:5173` 启动，`/api` 代理到 `127.0.0.1:18080`。`npm run build` 通过，产物约 1.1 MB。

接口脚本 `foundation/scripts/verify.sh` 的结果：

| 检查 | 结果 |
| --- | --- |
| 未登录访问 `GET /api/members` | HTTP 401，`未登录` |
| `admin` 登录 | 角色 `SYSTEM_ADMIN`，`canWrite=true` |
| 组织树 | 7 个节点：学校党委，其下两个党总支、四个党支部 |
| 新增党员并按姓名查询 | 新增「验证新增」，挂到「第二党总支第一党支部」，按姓名查到 1 条 |
| `general_admin`（第一党总支） | 只能看到张一、李二。读取王三和向第二党总支新增党员都是 HTTP 403 |
| `branch_viewer` | 新增党员 HTTP 403「没有操作权限」。列表只有张一 |
| `branch_admin` | 向兄弟支部新增党员 HTTP 403 |

浏览器使用本机 Chrome 无头模式打开 `http://127.0.0.1:5173`，走完页面：

- `admin` 登录后组织树出现「学校党委」和「第一党总支第一党支部」，党员列表同时有「验证新增」和「王三」。
- `general_admin` 的组织树没有「第二党总支」，党员表没有「王三」和「验证新增」，有「张一」。
- `branch_viewer` 的党员页没有「新增党员」按钮。

旧仓库 `BackEnd/`、`FrontEnd/` 的登录和权限代码没有被迁移，也没有被这次底座调用。

## 2. 代码结构

```text
foundation/
  server/          Spring Boot 单体
    src/main/java/com/partybuilding/foundation/
      config/SecurityConfig.java
      security/    JWT 签发与过滤器，每次请求按用户 id 重载角色
      service/     登录、组织、党员、数据范围
      web/         /api/auth、/api/orgs、/api/members
    src/main/resources/db/migration/V1__schema.sql
    src/main/resources/db/dev/V100__dev_seed.sql   仅 dev profile
    src/main/java/.../seed/DevUserSeeder.java      仅 dev profile
  web/             Vue 3 管理端：登录、组织树、党员档案
  scripts/verify.sh
```

这是参照 RuoYi-Vue-Pro（`YunaiV/ruoyi-vue-pro`，MIT）的单体路线做的瘦底座：Spring Security、JWT、RBAC 权限字、按组织子树限制数据。没有克隆该仓库。上游还包含工作流、支付、商城、CRM、ERP、物联网等模块，以及可选的微服务形态；按本阶段要求，这些都没有引入。党组织用独立的 `party_org`，没有把学校组织硬塞进通用部门表后再补类型约束。

数据范围对应上游的「全部数据」和「本组织及以下」：

- `SYSTEM_ADMIN` 不绑定组织，看到整棵树，并且只有这个角色可以建立党委。
- `COMMITTEE_ADMIN`、`GENERAL_ADMIN`、`BRANCH_ADMIN` 只能访问自己组织及其下级。
- `BRANCH_VIEWER` 范围与所在党支部相同，但没有 `org:write`、`member:write`。

角色和所属组织类型在登录时以及每次解析 JWT 时核对。党委管理员必须绑党委，党总支管理员必须绑党总支，党支部角色必须绑党支部。客户端请求体里的角色不会被信任。

## 3. 已完成内容

- 管理员登录和服务端权限校验。口令使用 BCrypt。令牌只保存用户 id。
- 学校党委、党总支、党支部三级组织。党支部不能再挂下级，党总支的上级必须是党委。
- 党员基础档案：姓名、性别、手机、党员编号、入党日期、状态。新增和按姓名、组织查询。
- 党员必须关联一个党支部。
- 不同角色、不同党组织的数据隔离，接口和页面都做了验证。

未做：发展党员阶段、组织生活、任务、荣誉积分、统一认证、企业微信、用户管理界面、操作审计。

## 4. 移动端选型

本阶段没有移动端代码。微信小程序和企业微信 H5 是两种交付物，不能合成一个安装包或一个工程就算同时交付。

| | 微信小程序 | 企业微信 H5 |
| --- | --- | --- |
| 运行环境 | 微信客户端里的小程序运行时 | 企业微信内置浏览器里的网页 |
| 与当前底座的关系 | 需要另一套页面工程，调用同一套 `/api` | 可以在现有 Vue 工程中增加移动页面，仍调用同一套 `/api` |
| 登录 | 微信登录，还要再映射到学校身份 | 企业微信 OAuth，和统一身份、应用消息是同一条链路 |
| 发布 | 微信审核、主体和类目 | 配置企业微信可信域名 |

建议下一阶段党员端单独做企业微信 H5。小程序只有在确认存在不进入企业微信的党员之后，再单独立项。两个端可以共用现在的 HTTP API，不能共用一套界面工程就算交付完成。

## 5. 存在的问题

- 底座是按 RuoYi-Vue-Pro 的技术选择新写的，不是上游仓库的裁剪分支。后续若要直接跟上游升级，需要再评估是否迁入 `yudao-module-system`。
- 角色是枚举，还没有菜单和权限的管理界面。新增角色要改代码和种子数据。
- 前端路由只检查本地是否有令牌。过期和越权由服务端返回 401 或 403。页面上的按钮隐藏不能代替服务端校验，服务端已经另做了校验。
- 组织删除和党员修改接口已有，页面还没有删除和编辑。
- 没有操作审计。登录失败和越权访问没有单独的审计表。
- 开发账号只在 `dev` profile 下按 `DEV_ADMIN_PASSWORD` 生成。正式环境不要启用 `dev`，否则会创建固定的开发账号。短于 12 位或 `admin123` 一类弱口令会拒绝启动。
- Flyway 10.10.0 提示 MySQL 8.4 新于它声明测试过的 8.1。本次迁移已经成功执行。
- 前端生产包仍是单文件，约 1.1 MB。

## 6. 下一阶段建议

1. 在这套权限上做党组织维护页面的编辑、删除，以及党员档案编辑。补操作审计。
2. 把组织关系转接做成服务端状态流转，转接成功时由服务端修改党员所属党支部。
3. 发展党员按五个固定阶段建表，不引入工作流引擎。
4. 向学校确认统一身份和企业微信参数之前，不把外部登录写成已完成。
5. 企业微信 H5 单独开工程或单独开路由组，不与小程序混在同一个交付物里。

## 7. Docker 环境整合验证

日期同样是 2026-10-15。在 `foundation/party-base-validation` 上以 `--no-ff` 合并 `origin/chore/local-middleware-compose`，没有冲突，没有合并 `main`，也没有改 PR #1。

本机 `25481` 和 `26381` 空闲，继续使用这两个端口，并且只绑定 `127.0.0.1`。镜像是 `mysql:8.4`（实际 `8.4.11`）和 `redis:latest`。数据目录是 `docker/mysql/data/`、`docker/redis/data/`，已写入 `.gitignore`。口令只在本机 `.env`，没有进入提交。

旧的 `01_partybuilding_schema.sql` 已删除。空数据目录第一次启动时，MySQL 只执行 `00_foundation_placeholder.sql`（`SELECT 1`），并创建空库 `party_foundation` 和用户 `partyapp`。表由后端 Flyway 创建。

后端以 `SPRING_PROFILES_ACTIVE=dev` 连接 `127.0.0.1:25481`，日志为：

```text
Migrating schema `party_foundation` to version "1 - schema"
Migrating schema `party_foundation` to version "100 - dev seed"
Successfully applied 2 migrations to schema `party_foundation`, now at version v100
Started FoundationApplication in 1.095 seconds
```

库里只有 `flyway_schema_history`、`party_org`、`party_member`、`sys_user`。没有旧系统的 `user`、`activity` 等 14 张表，也没有导入旧人员。`party_org` 7 行，开发用户 5 个。口令哈希不是原来的 `admin123` 哈希。

`foundation/scripts/verify.sh` 对本机 18080 的结果：

| 检查 | 结果 |
| --- | --- |
| 未登录访问党员列表 | HTTP 401，`未登录` |
| `admin` 登录 | `SYSTEM_ADMIN`，`canWrite=true` |
| 组织树 | 7 个节点，名称和类型与种子一致 |
| 新增并查询党员 | 新增 id 5「验证新增」，按姓名查到 1 条 |
| `general_admin` | 只能看到张一、李二；读王三和越权新增都是 HTTP 403 |
| `branch_viewer` | 新增 HTTP 403「没有操作权限」，列表只有张一 |
| `branch_admin` | 向兄弟支部新增 HTTP 403 |

Redis `PING` 返回 `PONG`。业务代码没有改成使用 Redis。

随后执行 `docker compose down` 再 `docker compose up -d`，没有删除数据目录。新容器日志里没有再次执行 `docker-entrypoint-initdb.d`。重启后仍是 7 个组织、5 个用户、5 名党员（含「验证新增」），Flyway 仍是 version 1 和 100。Redis 再次 `PONG`。原后端进程重连后，`admin` 登录 HTTP 200，角色仍是 `SYSTEM_ADMIN`。

这次没有重新打开浏览器。页面代码没有改，接口就是页面调用的同一组 `/api`。
