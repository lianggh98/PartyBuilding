# 智慧党建平台：PartyBuilding 可行性验证报告

> 验证日期：2026-10-15  
> 分支：`docs/partybuilding-feasibility-validation`（基于 `af1d2ab`）  
> 验证方式：本机静态阅读 + 本地隔离环境运行。未连接配置文件中的公网数据库或公网接口，未访问学校系统。  
> 结论先说：现有仓库能在本地编译并启动，但是一套 PC 端单体 CRUD 原型。对照招标参数 18 项，没有一项达到「A 已有」。服务端没有身份认证。直接在现有代码上补业务，盖不住认证、组织树、发展党员、积分和校园对接这些缺口。

## 1. 运行环境与执行记录

### 1.1 版本与兼容性

| 组件 | 声明 / 锁定 | 本机实际 | 兼容性 |
| --- | --- | --- | --- |
| JDK | `BackEnd/pom.xml` 中 `java.version` 17 | OpenJDK 17.0.18（Homebrew，aarch64） | 匹配 |
| Maven | 未锁定 | Apache Maven 3.9.15 | 可用。`BackEnd/mvnw` 无执行权限，本次用系统 `mvn` |
| Spring Boot | 3.0.5（Spring Framework 6.0.7，嵌入 Tomcat 10.1.7） | 启动日志一致 | 能启动。3.0 线已停止维护，后续升级需单独评估 |
| MyBatis | `mybatis-spring-boot-starter` 2.3.0，拉到 MyBatis 3.5.11、mybatis-spring 2.1.0 | 在 Boot 3.0.5 上启动成功 | 当前可用。Boot 3.2+ 与 mybatis-spring 2.x 已知不兼容，升级 Boot 时必须把 starter 升到 3.x |
| Lombok | 显式 1.18.20 | 源码未使用 Lombok 注解，编译通过 | 不阻塞。版本低于 Boot 3.0.5 BOM 管理的版本 |
| MySQL 驱动 | `mysql:mysql-connector-java` 8.0.23（旧坐标） | 连上本地 MySQL 8.0.46 | 本次可用。坐标已废弃，建议以后改为 `com.mysql:mysql-connector-j` |
| MySQL 脚本 | `SQL/partybuilding.sql` 头注明来源 8.0.32，导出日期 2023-04-30 | 导入到本地 8.0.46 成功 | 脚本可执行。库内含人员资料与口令散列，见风险 R1 |
| Node | 未声明 engines | v22.22.2 / npm 10.9.7 | Vite 4 未声明支持 Node 22，本次构建通过 |
| Vue / 构建 | Vue ^3.2.47、Vite ^4.2.0、vue-router ^4.1.6、Element Plus ^2.3.3 | 安装解析为 Vite 4.2.2 | 见下方构建记录 |
| 安全框架 | 无 `spring-boot-starter-security`，无拦截器 | 全接口匿名可调用 | 见风险 R2 |

架构是单个 Spring Boot 进程加 Vue 单页。仓库内没有微服务拆分，也没有引入微服务的业务必要。

### 1.2 后端编译与启动（已运行）

```text
cd BackEnd
mvn -DskipTests compile
```

结果：`BUILD SUCCESS`，编译 85 个源文件（2026-10-15 11:41，约 25 秒，依赖来自华为云 Maven 镜像）。

启动时用命令行覆盖数据源和上传目录，指向本机 Docker，不使用仓库里的公网地址和口令：

```text
docker run -d --name pb-feasibility-mysql \
  -e MYSQL_ROOT_PASSWORD=<本地临时口令> \
  -e MYSQL_DATABASE=partybuilding \
  -p 127.0.0.1:13309:3306 mysql:8.0

cd BackEnd
mvn -DskipTests spring-boot:run -Dspring-boot.run.arguments="\
  --spring.datasource.url=jdbc:mysql://127.0.0.1:13309/partybuilding?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai \
  --spring.datasource.username=root \
  --spring.datasource.password=<本地临时口令> \
  --file.upload-dir=/tmp/pb-feasibility-uploads"
```

结果：日志 `Started SpringBoot3Application in 0.69 seconds`，Tomcat 监听 `1994`。验证结束后已停止进程，并删除该容器。

### 1.3 数据库导入（已运行，仅本机）

`partybuilding.sql` 导入成功。`information_schema` 统计 14 张表：`Interview`、`activity`、`commend`、`cost`、`examination`、`expand`、`help`、`life`、`role`、`study`、`test`、`transfer`、`user`、`volunteer`。

没有组织机构表、任务表、积分表、签到表、消息表或同步日志表。

### 1.4 前端安装与构建

`FrontEnd/vite.config.js` 只有 Vue 插件，没有 `server.proxy`。接口根地址写死在 `FrontEnd/src/request/index.js` 的 `baseURL`，考试页和学习页的上传 `action` 也写死同一公网主机。路由守卫只检查 `localStorage.token` 是否非空（`FrontEnd/src/router/index.js`）。请求拦截器里的 token 头是注释掉的。

第一次干净安装后的构建失败：

```text
cd FrontEnd && npm install && npm run build
```

失败点：`Failed to resolve entry for package "@vue-office/pdf"`。`@vue-office/docx`、`excel`、`pdf` 的 1.0.0 包把入口放在 `postinstall` 里，按本机 Vue 主版本把 `lib/v3/index.js` 复制为 `lib/index.js`。依赖自己的 `postinstall` 执行时 Vue 尚未就绪，复制没有发生。

最小修复：在 `FrontEnd/package.json` 增加项目级 `postinstall`，于依赖安装完成后再执行这三个包自带的切换脚本。修复后重做干净安装：

```text
cd FrontEnd && rm -rf node_modules dist && npm install && npm run build
```

结果：`vite v4.2.2`，1548 个模块，`built in 3.92s`。产物约 6.9 MB 单 chunk，Rollup 给出体积警告，不影响本次构建成功。

### 1.5 连接前后端的实测与未测

以下针对本机 `127.0.0.1:1994`，用新建的测试账号，不用脚本里的历史账号。

| 流程 | 结果 | 证据 |
| --- | --- | --- |
| 注册、登录 | 实测通过 | `POST /user/register`、`POST /user/login` 返回 `code: 200`。登录响应里的 `password` 为 null |
| 用户列表 | 实测通过，同时暴露敏感字段 | `POST /user/getList` 返回 `size: 15`。15 行都带非空 `password` 与 `identitycard`。具体值不记录 |
| 新增与查询 | 实测通过，列表丢长文本 | `POST /life/insert` 返回「新增成功」。库内该行 `lifeContext` 长度为 2。`POST /life/getList` 的 `lifecontext` 为 null，因为列表 SQL 用了不含 BLOB 列的 `Base_Column_List`（`LifeMapper.xml`） |
| 文件上传与下载 | 实测通过 | `POST /file/upload` 返回 `errno: 0`。对应 `GET /file/download?filename=...` 返回 HTTP 200 |
| 无登录访问 | 实测现象 | `GET /hello` 不带任何凭证返回 HTTP 200 与 `hello world` |
| 浏览器里点页面 | 未运行验证 | 前端默认请求公网地址。按任务约束不把页面指到该主机，也未改写已提交的地址做浏览器联调 |

## 2. 十八项功能判定

判定：A 已有完整闭环；B 有代码或页面但必须改造；C 要新做；D 依赖学校接口或部署条件，可与 B/C 并列。

组织在现有模型里只是各表上的 `belongTo` / `belongto` 字符串。角色表是人员身份：超级管理员、分团委老师、基层支部书记、党员、发展党员、学生、市民（`SQL/partybuilding.sql` 的 `role`，以及 `FrontEnd/src/components/menu.vue`）。名称是团组织口径，没有党委、党总支、党支部的层级类型。

| 编号 | 判定 | 源码位置 | 依据 |
| --- | --- | --- | --- |
| 1.1 多层级组织树与组织维护 | C | `BackEnd/src/main/java/com/ru/springboot3/model/User.java` 的 `belongto`；`SQL/partybuilding.sql` 的 `user` | 只有党员记录上的支部名称字符串。没有组织表、上下级、组织信息维护页 |
| 1.2 任期换届提醒、层级和类型统计 | C | 同上；`role` 表只表示人员角色 | 没有任期、换届日期、组织类型字段，也没有统计接口 |
| 1.3 党员与积极分子电子档案、授权导出 | B | `UserController` `/user/getList`、`/updateAndInsert`；`FrontEnd/src/views/accountlist.vue`；角色值「发展党员」 | 有人员字段和用户管理页。没有档案材料、授权导出。列表把口令散列和证件号一并返回 |
| 1.4 组织关系转接申请、审核、结果与统计 | B | `TransferController` `/transfer/insert`、`/start`、`/pendingApproval`、`/approval`；`transfer` 表；`FrontEnd/src/views/transfer.vue` | 有支部名称之间的发起、待审批、通过、拒绝。菜单只给「分团委老师」。通过后把人员 `belongto` 写成当前登录人的支部，而不是单据上的接收支部。没有校外组织、没有统计。后端不校验调用者身份 |
| 2.1 三会一课、主题党日的创建、发布与通知 | B | `LifeController` 与 `life` 表；`ActivityController` 与 `activity` 表；`FrontEnd/src/views/life.vue`、`activity.vue` | 组织生活是标题、简介、正文。党群活动是时间、地点、标题和一段「活动人员」文本。没有会议类型，没有发布状态，没有通知 |
| 2.2 移动端签到、时间与参与记录 | C | `activity.activityMumber` | 参与者是一段文本。没有签到接口、签到时间、移动端页面 |
| 2.3 会议纪要、附件、档案、报表导出 | B | `StudyController` 与 `study` 表；`ExaminationController`；`FileController`；`FrontEnd/src/components/file/*` | 学习和考试可以挂文件并预览 word、excel、pdf。没有纪要实体，没有报表导出接口 |
| 2.4 学习任务定向发布、进度、学时、完成率 | B | `study` 表字段：`title`、`path`、`createTime`、`belongTo`；页面 `FrontEnd/src/views/index.vue` | 可以按支部名称挂一份资料。学时规则：代码里没有学时字段、没有累计、没有完成状态。不能把资料列表当成学时已完成 |
| 3.1 党建任务创建、分配、发布 | C | 无对应 Controller / 表 / 页面 | 志愿服务、党群活动不是带分配和发布状态的任务 |
| 3.2 任务进度、到期提醒、逾期督办 | C | 无 | 无到期日、提醒或督办 |
| 3.3 发展党员五阶段、材料、审核、档案 | B | `expand` 表（`from`、`to`、`createTime`）；`ExpandController`；`UserMapper.toBeDeveloped`、`partyMember`；`FrontEnd/src/views/developers.vue` | 只有培养人与被培养人的一条关系，外加角色名「发展党员」。五个固定阶段、材料、审核、档案都不存在。按任务约定，此处不要求通用工作流引擎，但五个阶段本身仍要新建 |
| 3.4 培养谈话、培训考核、转正等时间节点提醒 | C | `expand.createTime` 仅为关系创建时间 | 没有节点日历或提醒 |
| 4.1 移动端荣誉申报与证明材料 | B | `CommendController`；`commend` 表 | 有表彰登记的增删改查接口。没有前端页面，没有移动端，没有证明材料字段。申报流程要新建 |
| 4.2 荣誉审核、退回、按字段组合查重 | C | `commend` 表无状态、无审核人 | 没有审核流，也没有按标题、人员、时间等字段查重 |
| 4.3 积分规则、自动累加、明细与排名 | C | 全仓库无积分模型 | 无规则、无明细、无排名 |
| 5.1 学校 SSO、统一门户、企业微信扫码与认证策略 | C + D | 登录只有 `UserController.login` 比对本库口令 | 无 CAS、OAuth、OIDC、企业微信。学校不提供协议、测试账号和字段前，无法联调。本次没有假装联调通过 |
| 5.2 企业微信消息通知 | C + D | 无消息模块 | 需要企业微信企业 ID、应用 Secret、接收人账号映射和消息模板。学校未提供 |
| 5.3 教师、学生及党建基础数据同步 | C + D | 用户靠页面录入，`UserServiceImp.register` | 无同步作业、无外部主键、无增量水位 |

仓库里还有招标表未单列、但已经有页面的功能，只能当作原型参考：党费（`CostController`、`cost.vue`）、走访（`InterviewController`、`interview.vue`）、结对帮扶（`HelpController`、`help.vue`）、志愿服务（`VolunteerController`、`volunteer.vue`）、考试附件（`exam.vue`）。这些同样没有服务端权限。

## 3. 关键风险

| ID | 严重程度 | 现象 | 影响 | 最小修复 | 是否阻塞上线 |
| --- | --- | --- | --- | --- | --- |
| R1 | 高 | `BackEnd/src/main/resources/application.properties` 写入公网库地址、账号和口令；`file.upload-dir` 写死服务器路径。`SQL/partybuilding.sql` 含姓名、证件号、手机、邮箱和口令散列。前端 `baseURL` 与上传地址写死公网主机 | 仓库即凭证。历史人员资料进入版本库 | 把连接改成环境变量或本地忽略文件；轮换已出现的口令；SQL 改为无真实人员的结构脚本。本报告不摘录具体值 | 阻塞任何共享或部署 |
| R2 | 高 | 无服务端会话。路由守卫只看本地是否存过用户 JSON。列表和详情按请求体里的 `role`、`belongto` 过滤。`@CrossOrigin(origins = "*")` 加在各 Controller 上 | 调用方可以自行指定身份，读改全部业务数据 | 先加服务端登录态，接口从会话取用户，去掉信任请求体角色 | 阻塞 |
| R3 | 高 | 登录把整份用户 JSON 放进 `localStorage`（`login.vue`）。`/user/getList`、`/user/getById` 返回口令散列和证件号。口令由前端 `crypto-js` 做 MD5 后存储（`login.vue`、`accountlist.vue`），无盐 | 管理端列表即可收集证件号和可重放的口令摘要 | 接口去掉敏感字段；口令改为服务端慢哈希；登录态改为服务端签发的随机令牌 | 阻塞 |
| R4 | 高 | `FileController` 的下载按查询参数拼上传目录，`normalize` 之后没有强制结果仍在上传根目录内。上传与下载均无登录校验。下载响应的媒体类型固定写成 jpeg | 匿名访问者可能读写上传目录及其父路径上的文件 | 登录后下载；拒绝越出根目录的路径；按扩展名设置类型。本次只做静态阅读，没有做越权读取试验 | 阻塞 |
| R5 | 中 | 组织、转接、活动都用自由文本支部名。转接「通过」写错支部（`transfer.vue` 的 `passClick`）。`/transfer/insert` 失败分支仍返回成功码 | 组织数据无法逐级统计，转接结果不可信 | 先建组织表，转接成功时在服务端更新党员所属组织 | 阻塞招标项 1.x、1.4，不阻塞「程序能跑」 |
| R6 | 中 | 干净 `npm install` 后生产构建失败，原因是 `@vue-office/*` 入口依赖安装脚本 | 新环境无法产出前端包 | 已在 `FrontEnd/package.json` 加上项目级 `postinstall`，干净安装后构建通过 | 已用最小改动消除 |
| R7 | 低 | MyBatis starter 2.3.0 钉在 Boot 3.0.5 上能启动；Boot 3.0 已不维护 | 以后升级框架会在启动期失败 | 升级 Boot 时同步升级 MyBatis starter 到 3.x，并替换 MySQL 驱动坐标 | 不阻塞当前运行 |
| R8 | 低 | 列表查询省略长文本列；分页在内存 `subList`，页码越界会抛异常 | 组织生活列表正文为空；错误页码变成 500 | 列表与详情分开映射；分页下推到 SQL | 不阻塞可行性判断 |

没有操作审计。异常多被吞掉后返回「失败」或误返回「成功」。日志会打印上传文件名和部分业务字段。

## 4. PC 后台与党员移动端

PC 管理后台继续用 Vue 3 + Element Plus 这一类单页即可。现有 `FrontEnd/` 是管理端原型，菜单覆盖党务、党群、教育和用户管理，没有独立党员移动布局。

| | 企业微信 H5 | 微信小程序 |
| --- | --- | --- |
| 与现有代码的关系 | 可在同一 Vue 工程里做移动页面，放在企业微信内置浏览器 | 需要另一套运行时（原生或 UniApp），现仓库没有 |
| 登录 | 企业微信 OAuth，和 5.1 是同一条身份链路 | 微信登录；师生若只在企业微信里，还要再打通一次身份 |
| 消息 | 企业微信应用消息，直接对应 5.2 | 订阅消息，审核和模板都另算，覆盖不到「只在企业微信里的师生」 |
| 签到 | H5 扫码或地理签到可以完成 2.2 | 扫码体验更好，但要多维护一个端 |
| 发布 | 配企业微信可信域名即可 | 要走微信审核，学校主体和类目要单独确认 |

推荐：PC 用管理后台；党员端先做企业微信 H5。招标项把企业微信扫码登录和消息通知写在一起，H5 能跟这两项共用身份和消息。小程序留到确认存在一批不进企业微信的党员之后再单独立项。

本仓库没有移动端工程。任务书里的 `lianggh98/wanyue_dangjian` 匿名访问返回 404，不能当作可引用源码。公开的万岳党建仓库（`WanyueKJ/wanyue_dangjian`）声明 GPL-3.0，说明文字又限制商用、要求购买商业版。本次未复制该仓库任何代码。

## 5. 方案 A / B

| 维度 | 方案 A：继续改 PartyBuilding | 方案 B：以 RuoYi-Vue-Pro 为底座，业务按招标重做 |
| --- | --- | --- |
| 代码复用 | 能留下约 13 个 Controller 和 11 个页面。它们是扁平 CRUD，18 项里多数仍要新表、新页面 | 管理端壳、登录、RBAC、操作日志、代码生成可直接用。党务表和现有 Vue 页面要重写，复用的是字段思路（党费、转接状态、学习文件、考试附件） |
| 兼容性 | 已在 JDK 17 + Boot 3.0.5 + Vue 3 跑通。Boot 3.0 过旧，MyBatis 2.x 限制后续升级 | 上游当前线同样是 JDK 17 与 Spring Boot 3，技术栈同代。需要按单体模块引入，不引入它的可选微服务形态 |
| 安全 | R2、R3、R4 都要在这个项目里从零补上，等于自研一套权限和审计 | 上游以 Spring Security、RBAC、数据权限和操作日志为基本能力，认证缺口由框架承担 |
| 迁移量 | 页面迁移动作小，领域模型和安全改造大。转接、组织、发展党员都要推翻重做 | 现有 Java 与 Vue 几乎不能原样搬。业务代码量本身不大，重写成本低于把安全框架塞进当前结构 |
| 后续维护 | 权限、字典、日志、导入导出都要自己养 | 底座有上游。党务模块仍要自己维护。须锁定一个发行版，避免跟着商业功能走 |

许可：本仓库根目录 `LICENSE` 为 Apache-2.0 正文，附录版权人仍是占位符。在保留该许可证的前提下可以改作衍生作品。`YunaiV/ruoyi-vue-pro` 仓库声明 MIT（核对了 GitHub 上 `LICENSE` 与仓库说明，未克隆代码）。MIT 允许并入学校项目。该项目另有商业版和付费模块，本次建议只使用 MIT 仓库中的单体管理能力，不购买、不复制商业包。

推荐方案 B。PartyBuilding 证明了「JDK 17 + Boot + Vue 3 + MySQL 单体」这条路能跑，也留下了可参考的页面清单。它没有可复用的认证、组织和党员发展模型。方案 A 要先自研这些底座，再做 18 项里的 C 类功能，底座工作和业务工作叠在同一个不设防的代码上。方案 B 把底座交给 MIT 框架，党务按五个固定阶段和字段查重新建，仍然部署成一个 Spring Boot 进程，不引入微服务。

## 6. 下一阶段任务

### P0（没有这些就不能开发业务或给学校演示）

- 连接信息改环境变量，轮换已入库的数据库口令，SQL 改为不含真实人员的初始化脚本。
- 选定方案 B 的单体模块边界：一个可启动的服务，带登录、RBAC、操作日志。
- 定组织模型：党委、党总支、党支部，替换全文 `belongTo` 字符串。
- 与学校确认 5.1–5.3 的协议、测试账号、字段和接口文档。未确认前接口保持未联调。

### P1（招标闭环）

- 党员与积极分子档案、授权导出（1.3）。
- 组织关系转接在服务端完成申请、审核和所属组织变更（1.4）。
- 三会一课 / 主题党日、纪要附件、企业微信 H5 签到（2.1–2.3）。
- 学习任务与明确的学时规则：学时来自哪类活动、如何累计、完成率分母是谁（2.4）。
- 党建任务的分配、到期与逾期（3.1、3.2）。
- 发展党员五个固定阶段、材料、审核和节点提醒（3.3、3.4）。不建通用工作流引擎。
- 荣誉申报、审核退回、字段组合查重（4.1、4.2）。不做图像识别。
- 积分规则、明细和排名（4.3）。
- 企业微信 H5 登录与消息（5.1、5.2），依赖学校应用参数。

### P2（现有原型里有、招标表未单列）

- 决定党费、走访、结对帮扶、志愿服务、考试附件是迁入新模型，还是本期不做。
- 前端按路由拆包，去掉 6.9 MB 单文件。
- 升级 Spring Boot 时同步升级 MyBatis starter，并替换 MySQL 驱动坐标。

依赖学校才能做完的项：5.1、5.2、5.3，以及 2.2 若签到必须使用学校统一身份。

## 7. 未验证项与需学校确认的事项

未运行验证：

- 浏览器中的登录、菜单、表格编辑。前端默认主机是公网地址，本次没有对它发请求。
- 使用脚本中的历史账号登录。
- 文件路径穿越的实际读取。只做了静态阅读（R4）。
- `lianggh98/wanyue_dangjian` 源码。该地址匿名访问为 404。
- RuoYi-Vue-Pro 的编译与启动。只核对了公开许可证，没有把框架拉进本仓库。
- 生产部署、学校统一身份、企业微信和数据中心联调。

请学校确认：

- 统一身份的协议（CAS、OAuth 或 OIDC）、回调地址、测试账号、用户唯一键。
- 企业微信的企业 ID、应用 AgentId、可见范围，以及消息接收人与党员编号的对应字段。
- 数据中心要同步的教师、学生、组织字段，全量或增量，以及同步频率。
- 党员移动端是否全部在企业微信内。若存在校外党员，再决定要不要加微信小程序。
- 学时的计算口径，以及发展党员五个阶段各自必填材料。
- 历史 SQL 中的人员数据是否仍代表真实个人信息，是否需要从仓库历史中清除。
