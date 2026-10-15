# 智慧党建第二阶段技术底座

单体应用：Spring Boot 3.3 + Spring Security + MyBatis-Plus + Vue 3 + Element Plus + MySQL 8.4。

不包含旧仓库的登录逻辑，也不包含微服务、工作流、商城等无关模块。移动端不在本目录。Redis 由根目录 Compose 提供，本模块当前不连接它。

## 本地启动

中间件、端口和初始化方式见仓库根目录的 `docker/README.md`。

```bash
cp .env.example .env
# 填写本机口令。正式环境不要使用 dev profile。
docker compose up -d

cd foundation/server
set -a && source ../../.env && set +a
mvn -DskipTests spring-boot:run

cd foundation/web
npm install
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。开发账号由 `DEV_ADMIN_PASSWORD` 在首次启动时创建，口令不在源码里。

## 已实现

- `POST /api/auth/login`、`GET /api/auth/me`：服务端校验口令并签发 JWT，后续请求重新加载用户与角色。
- `GET /api/orgs/tree`：党委、党总支、党支部树，按当前用户的组织子树过滤。
- 党员档案的新增和查询，党员只能挂在党支部。
- 角色写权限与组织数据范围同时生效。只读角色不能新增；总支管理员看不到其他总支的党员。
