# 松和堂 · 线上购药系统 前端

计2302 20235874 于贺

本工程是《线上购药系统》后端(`../medshop`)配套的 Web 前端,采用 **Vue 3 多文件组件化** 架构,
对应后端已实现的三个模块:基础购药(M1)、用户与权限(P3)、智能药师咨询(M2 / AI)。

## 设计

界面走「现代药铺 / 临床编辑风」(modern apothecary),刻意避开千篇一律的 AI 模板审美:
- **配色**:暖米白纸感底色 + 深松绿主色 + 陶土橙点缀,而非白底紫渐变
- **字体**:标题用衬线 Fraunces + 思源宋体,正文用 Hanken Grotesk + 思源黑体,避开 Inter/Roboto
- **动效**:页面加载错落上浮、路由过渡、AI 置信度弧形仪表生长动画等高光时刻
- 全套设计 token 收敛在 `src/styles/tokens.css`,改一处即换肤

## 技术栈

- Vue 3(`<script setup>` 组合式 API)
- Vite 5(构建 / 开发服务器,内置 `/api` 代理到后端)
- Vue Router 4(路由,每个页面独立懒加载)
- Pinia 2(状态管理:登录态、购物车、轻提示)
- Axios(HTTP,统一拦截 token 与 `{code,message,data}` 解包)

## 如何运行

环境要求:Node 18+(已在 Node 22 验证)。

```bash
# 1. 先启动后端(另一个终端)
cd ../medshop
mvn spring-boot:run        # 监听 http://localhost:8080

# 2. 启动前端
cd ../medshop-web
npm install
npm run dev                # 打开 http://localhost:5173
```

开发服务器把 `/api` 请求代理到 `http://localhost:8080`,无需关心跨域。
生产构建:`npm run build`,产物在 `dist/`。

## 演示账号

| 手机号 | 密码 | 说明 |
|--------|------|------|
| 13800000000 | 123456 | 消费者(李阿姨,预置收货地址) |

登录页点「使用演示账号体验」可一键填入。

## 页面一览(细分多文件,各司其职)

| 路由 | 文件 | 说明 |
|------|------|------|
| `/login` | `views/LoginView.vue` | 登录 / 注册,左侧品牌叙事 + 右侧表单 |
| `/home` | `views/HomeView.vue` | 首页:Hero 搜索、三大保障、药师精选、AI 引导 |
| `/drugs` | `views/DrugListView.vue` | 药品搜索结果,OTC/Rx 筛选 + 分页 |
| `/drugs/:id` | `views/DrugDetailView.vue` | 药品详情,用法用量/禁忌、处方药提示、加购 |
| `/cart` | `views/CartView.vue` | 购物车 + 结算下单(含处方药提醒) |
| `/orders/:id` | `views/OrderDetailView.vue` | 订单详情,状态时间线 + 明细 |
| `/consult` | `views/ConsultView.vue` | **智能药师对话**(AI 亮点):知识来源、置信度仪表、一键转人工 |
| `/privacy` | `views/PrivacyView.vue` | 隐私授权开关,可随时撤回(约束 C-3) |

## 工程结构

```
src/
├── main.js                 入口
├── App.vue                 根组件(页头/页脚/路由过渡/Toast)
├── router/index.js         路由 + 登录守卫
├── styles/
│   ├── tokens.css          设计 token(配色/字体/间距/动效)
│   └── base.css            reset + 全局基础样式 + 入场动画
├── api/
│   ├── http.js             axios 实例(拦截器:token、解包、401)
│   ├── auth.js             登录/注册/隐私授权
│   ├── shop.js             药品/订单
│   └── consult.js          智能药师咨询
├── stores/
│   ├── auth.js             登录态(持久化)
│   ├── cart.js             购物车(持久化)
│   └── toast.js            全局轻提示
├── components/
│   ├── DrugCard.vue        药品卡片(首页/列表复用)
│   ├── layout/             AppHeader、AppFooter
│   └── ui/                 BaseButton、BaseInput、RxBadge、
│                           ConfidenceMeter(置信度仪表)、ToastHost
└── views/                  8 个页面组件(见上表)
```

## 与后端接口对应

前端调用的接口全部来自后端已实现并冒烟通过的 API:

- `POST /api/v1/auth/login`、`/register` — 登录注册
- `GET /api/v1/drugs?keyword=&page=&size=`、`GET /api/v1/drugs/{id}` — 药品
- `POST /api/v1/orders`、`GET /api/v1/orders/{id}` — 订单(处方药后端拦截)
- `POST /api/v1/consultations`、`/{id}/messages`、`/{id}/handoff` — AI 咨询与转人工
- `PUT /api/v1/privacy/authorization` — 隐私授权

> 上传打包时请剔除 `node_modules/` 与 `dist/`(已在 `.gitignore` 中排除),
> 依赖通过 `npm install` 还原。
