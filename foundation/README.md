# 智慧党建第二阶段技术底座

单体应用：Spring Boot 3.3 + Spring Security + MyBatis-Plus + Vue 3 + Element Plus + MySQL 8。

不包含旧仓库的登录逻辑，也不包含微服务、工作流、商城等无关模块。移动端不在本目录。

## 本地启动

数据库口令和 JWT 密钥只通过环境变量注入，示例见 `.env.example`。

```bash
docker run -d --name party-foundation-mysql \
  -e MYSQL_ROOT_PASSWORD="$DB_PASSWORD" \
  -e MYSQL_DATABASE=party_foundation \
  -p 127.0.0.1:13309:3306 mysql:8.0

cd foundation/server
DB_PASSWORD=... JWT_SECRET=... mvn spring-boot:run
# 服务监听 18080，避开本机可能已经占用的 8080。

cd foundation/web
npm install
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。种子账号的本地演示口令见 `server/src/main/resources/db/migration/V1__init.sql` 文件头，仅用于本机验证。

## 已实现

- `POST /api/auth/login`、`GET /api/auth/me`：服务端校验口令并签发 JWT，后续请求重新加载用户与角色。
- `GET /api/orgs/tree`：党委、党总支、党支部树，按当前用户的组织子树过滤。
- 党员档案的新增和查询，党员只能挂在党支部。
- 角色写权限与组织数据范围同时生效。只读角色不能新增；总支管理员看不到其他总支的党员。
