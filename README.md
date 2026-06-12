# 松和堂 · 线上购药系统


一个前后端分离的线上购药系统：后端 Spring Boot + SQLite，前端 Vue 3。覆盖**消费者购药、智能药师咨询、药师审核接诊、运营管理**四类角色的完整业务闭环，解压即可在本地跑起来（SQLite 文件库，无需安装数据库服务）。

---

## 一、项目结构

```
medshop/
├── src/                  后端 Spring Boot 工程（Java + MyBatis + SQLite）
├── pom.xml               后端 Maven 配置
├── medshop-web/          前端 Vue 3 工程（Vite + Pinia + Vue Router）
└── README.md             本文档
```

后端与前端是两个独立工程，分别构建、分别启动；开发时前端通过 Vite 代理把 `/api` 转发到后端 `:8080`。

---

## 二、环境要求

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 17 | 编译运行后端 |
| Maven | 3.6+ | 后端依赖管理与构建 |
| Node.js | 18+（已在 22 验证） | 前端依赖与构建 |

> 上传包已剔除 `target/`、`medshop-web/node_modules/`、`medshop-web/dist/`、`data/*.db`。
> 后端依赖在 `mvn` 构建时自动下载，前端依赖通过 `npm install` 还原，数据库每次启动按脚本重建。

---

## 三、如何运行（两个终端）

### 1. 启动后端

```bash
cd medshop
mvn spring-boot:run
```

后端监听 `http://localhost:8080`。首次启动会自动：
1. 创建 `./data/medshop.db`（SQLite 数据库文件）；
2. 执行 `db/schema.sql` 建表、`db/data.sql` 灌入演示药品与药学知识库；
3. 由 `DataInitializer` 写入演示账号（密码均为 `123456`）。

### 2. 启动前端

```bash
cd medshop/medshop-web
npm install        # 首次运行需还原依赖
npm run dev
```

前端开发服务器监听 `http://localhost:5173`，浏览器打开即可。`/api` 请求自动代理到后端，无需关心跨域。

生产构建：`npm run build`，产物在 `dist/`。前端为 **hash 路由**，`dist/` 可直接由任意静态服务器托管。

---

## 四、演示账号（密码均为 123456）

| 手机号 | 角色 | 登录后看到的首页 | 能做什么 |
|--------|------|------------------|----------|
| `13800000000` | 消费者（李阿姨） | 购药首页 | 搜药、加购、下单、AI 咨询、上传处方、管理收货地址、改昵称/密码 |
| `13800000001` | 执业药师 | 药师工作台仪表盘 | 接转人工工单并实时对话、审核处方、查看工作量统计、改昵称/密码 |
| `13800000099` | 运营管理员 | 运营管理仪表盘 | 网站统计、药品库存管理、订单流转、人员管理（新增员工）、改昵称/密码 |

> 登录页点「使用演示账号体验」可一键填入消费者账号。
> 三种角色登录后**首页与导航完全不同**：消费者是购药商城，药师是接诊工作台，运营是经营看板。运营/药师没有购药与咨询入口。

---

## 五、功能模块

### 消费者（角色 0）
- **基础购药**：药品搜索（OTC/Rx 筛选、分页）、详情、购物车、下单（事务 + 库存校验 + 价格快照）、订单状态查询。
- **智能药师咨询（AI）**：经 AI 中台 RAG 检索 + 合规过滤 + 置信度/风险判定生成用药建议；低置信或高风险时提示「一键转人工」。
- **处方药管控**：购买处方药需上传处方图片，经药师审核通过后方可下单，否则结算被拦截。
- **收货地址管理**：增删改、设默认；结算页自动选中默认地址，可切换。
- **个人中心**：改昵称、改密码、管理收货地址。
- **隐私授权**：可随时开关、撤回（约束 C-3）。

### 执业药师（角色 1）
- **工作台仪表盘**：随时间问候、待接工单 / 待审处方计数、工作量 KPI、手绘图表（工作量分布柱状图、处方通过率环形图）。
- **转人工工单**：接单后与用户实时对话（轮询），处理完标记完成；用户也可主动「返回智能药师」结束人工。
- **处方审核**：查看处方图片，通过 / 驳回（驳回需填原因）。

### 运营管理员（角色 3）
- **管理仪表盘**：营收、订单、用户、药品 4 项 KPI；订单状态分布柱状图、用户构成环形图。
- **药品库存**：上架新药品、调整库存、低库存预警。
- **订单管理**：按业务规则流转订单状态（确认支付 → 发货 → 完成 / 取消）。
- **人员管理**：查看全部用户，新增员工账号（药师 / 客服 / 运营）。

---

## 六、技术栈

**后端**
- Spring Boot 3.2 + MyBatis 3
- SQLite（文件型数据库，免安装）
- JWT 鉴权（jjwt）、BCrypt 密码哈希
- 角色鉴权拦截器，按角色控制接口访问

**前端**
- Vue 3（`<script setup>` 组合式 API）
- Vite 5（构建 / 开发服务器，内置 `/api` 代理）
- Vue Router 4（hash 路由 + 登录守卫 + 角色守卫）
- Pinia 2（登录态、购物车、轻提示）
- Axios（统一拦截 token 与 `{code,message,data}` 解包）

界面走「现代药铺 / 临床编辑风」：暖米白底 + 深松绿主色 + 陶土橙点缀，标题衬线字体，刻意避开千篇一律的 AI 模板审美。设计 token 收敛在 `src/styles/tokens.css`。

---

## 七、AI 模块说明（智能药师咨询）

AI 调用统一收敛到 **AiGatewayService**，业务层不直接调大模型，体现「AI 解耦 + 合规兜底」。支持两种 provider，在后端 `application.yml` 的 `medshop.ai.provider` 切换：

- `mock`（默认）：基于本地药学知识库做关键词 RAG 召回 + 模板生成，**离线、无需 API Key** 即可完整演示「检索 → 生成 → 置信度/风险判定 → 转人工」全流程。
- `claude`：调用 Claude 中转站（OpenAI 兼容 `/v1/chat/completions`）。配置示例：

  ```yaml
  medshop:
    ai:
      provider: claude
      base-url: https://你的中转站地址/v1
      api-key: sk-xxxx
      model: claude-sonnet-4-20250514
  ```

两种 provider 共用同一套合规过滤、置信度阈值与高风险词判定；判定为低置信 / 高风险 / 命中合规拦截时返回 `needHandoff=true`，前端据此展示「一键转人工」。

---

## 八、主要接口一览

| 模块 | 方法与路径 |
|------|-----------|
| 认证 | `POST /api/v1/auth/login`、`/register` |
| 药品 | `GET /api/v1/drugs?keyword=&page=&size=`、`GET /api/v1/drugs/{id}` |
| 订单 | `POST /api/v1/orders`、`GET /api/v1/orders/{id}`、`GET /api/v1/orders/mine` |
| 收货地址 | `GET/POST /api/v1/addresses`、`PUT/DELETE /api/v1/addresses/{id}`、`PUT /api/v1/addresses/{id}/default` |
| AI 咨询 | `POST /api/v1/consultations`、`/{id}/messages`、`/{id}/handoff`、`/{id}/resume` |
| 处方 | `POST /api/v1/prescriptions`、`GET /prescriptions/mine`、`/pending`、`PUT /prescriptions/{id}/review` |
| 药师工作台 | `GET /api/v1/handoff/tickets`、`PUT /handoff/tickets/{id}`、`GET /handoff/stats`、`/online` |
| 个人信息 | `GET/PUT /api/v1/users/me`、`PUT /users/me/password` |
| 运营后台 | `GET /api/v1/admin/stats`、`/drugs`、`/orders`、`/users`；`POST /admin/drugs`、`/users`；`PUT /admin/drugs/{id}/stock`、`/orders/{id}/status` |
| 隐私授权 | `PUT /api/v1/privacy/authorization` |

> 后端鉴权：除登录/注册外所有接口需带 `Authorization: Bearer <token>`。
> 角色接口（药师工作台、运营后台）越权访问返回 403。

---

## 九、后端工程结构

```
src/main/java/com/medshop/
├── MedshopApplication.java        启动类
├── common/                        统一返回、异常、JWT、鉴权拦截器、演示数据初始化
│   └── security/                  JwtUtil / AuthInterceptor / WebConfig
├── user/                          用户与权限、个人信息
├── shop/                          药品、订单、收货地址、运营后台
│   ├── controller/                DrugController、OrderController、AddressController、AdminController …
│   ├── service / mapper / entity / dto
└── consult/                       智能药师咨询、转人工、处方审核
    ├── ai/                        AI 中台：AiGatewayService、LlmClient(mock/claude)
    ├── service / controller / mapper / entity / dto
src/main/resources/
├── application.yml                数据源、JWT、AI provider 配置
├── db/schema.sql                  建表脚本（SQLite）
├── db/data.sql                    演示数据（药品、知识库、地址）
└── mapper/*.xml                   MyBatis SQL 映射
```

前端工程结构见 `medshop-web/README.md`。
