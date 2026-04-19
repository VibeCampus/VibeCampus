# 校园信息墙 — 后端接口说明

**文档性质**：由前端维护，给后端实现与联调使用。  
**对齐范围**：

1. HTTP 路径、方法与入参名以仓库 **`src/api/*.js`** 为准（Axios 封装层）。  
2. **页面与数据结构**以 **`src/router/index.js`**、各 **`src/views/*.vue`**、**`src/stores/*.js`**、**`src/components/PostCard.vue`** 为准（当前多数页面仍为 Mock + localStorage，联调后会改为调用上述 API）。

下文中的**固定规则**（分页起点、默认值、上限）为项目组与后端约定的契约；若后端必须采用其它规则，请书面变更并同步前端改 `src/api` 与本文档。

---

## 1. 环境与鉴权

| 项 | 固定约定 |
|----|----------|
| Base URL | `VITE_API_BASE_URL`，未配置时相对站点根路径 **`/api`**。 |
| 协议 | 生产 **HTTPS**；本地开发 HTTP 即可。 |
| 编码 | UTF-8。 |
| JSON | `Content-Type: application/json`（除 multipart 接口外）。 |
| 用户端鉴权 | 请求头 **`Authorization: Bearer <token>`**。Token 来自 **`POST /auth/login`** 或 **`POST /auth/register`** 成功响应。 |
| 管理端鉴权 | 同上；Token 来自 **`POST /admin/auth/login`**。当前前端 Axios 仍读 **同一** `localStorage.token`（用户与管理员不宜同一浏览器交替登录，或后续前端拆 `adminToken`）。 |
| 前端超时 | Axios **15s**。 |

### 1.1 成功响应（二选一，全项目必须统一）

**方案 A — 直出（与当前 Axios 拦截器一致，推荐）**  
HTTP 2xx 时，响应体即为业务 JSON，例如：

```json
{ "token": "jwt...", "user": { "id": 1, "username": "..." } }
```

列表：

```json
{ "list": [], "total": 100, "page": 1, "pageSize": 20 }
```

**方案 B — 包装**  
`{ "code": 0, "message": "ok", "data": <业务JSON> }`，**`code === 0` 表示成功**。  
注意：当前拦截器返回的是**整段**响应体，**不会**自动取 `data`。若采用方案 B，要么后端对前台路由仍用方案 A，要么前端改拦截器后再联调。

### 1.2 错误响应

| HTTP | 含义 |
|------|------|
| 400 | 参数非法、缺必填、超范围。 |
| 401 | 未登录或 Token 失效。 |
| 403 | 无权限。 |
| 404 | 资源不存在。 |
| 409 | 冲突（如手机号已注册）。 |

响应体至少包含：

```json
{ "message": "string，给人看的错误说明" }
```

前端用 `error.response.data.message` 展示。

### 1.3 前端 localStorage 与 401（联调必看）

| 键 | 用途 |
|----|------|
| `token` | 登录 Token；Axios 请求拦截器读取。 |
| `userInfo` | 当前用户 JSON 字符串；**`useUserStore`**（`src/stores/user.js`）读写。 |

**不一致处（对接时前端会修）**：`src/api/index.js` 在 401 时执行 `removeItem('user')`，而业务代码使用的是 **`userInfo`**。后端无需处理，但前后端需知：联调后前端应统一清除 **`userInfo`**。

---

## 2. 分页与条数上限（全站统一）

### 2.1 Query 参数 `page` / `pageSize`

| 参数 | 类型 | 是否必填 | 约定 |
|------|------|----------|------|
| `page` | integer | 否 | **从 1 开始**，表示第几页。未传或 `≤0` 时按 **第 1 页** 处理。 |
| `pageSize` | integer | 否 | 每页条数；**默认 20**。 |
| `pageSize` 上限 | — | — | **最大 100**。若请求 `>100`，返回 **HTTP 400** 且 `message` 说明，或静默改为 100（二选一，**全项目只能选一种**，推荐 **400**）。 |

### 2.2 列表类响应体（必须字段）

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | array | 当前页数据。 |
| `total` | integer | 符合条件的总条数。 |

以下字段**建议返回**，与 `src/api/post.js` 中 `GET /posts` 的 JSDoc 一致：

| 字段 | 类型 | 说明 |
|------|------|------|
| `page` | integer | 与本次实际查询页码一致。 |
| `pageSize` | integer | 与本次实际每页条数一致。 |

通知类列表若存在未读数，需带 **`unreadCount`: integer**。

### 2.3 `limit`（非分页列表）

用于 **`GET /posts/hot`**、**`GET /admin/dashboard/recent-posts`** 等。

| 参数 | 类型 | 默认值 | 最大值 |
|------|------|--------|--------|
| `limit` | integer | **10** | **100**（超出按 400 或截断为 100，与 `pageSize` 策略一致）。 |

---

## 3. 路由 → 页面功能 → 依赖接口

以下按 **`src/router/index.js`**。接口路径均相对于 **Base URL**。

### 3.1 用户端（前台）

| 路由 | 页面文件 | 用户可见功能 | 应对接口（`src/api`） |
|------|-----------|--------------|------------------------|
| `/` | `HomeView.vue` | 信息流、顶栏分类 Tab、社交墙子类型；帖子卡片跳转详情 | `GET /posts`（分类见 **§4**）；侧栏热门见 `GET /posts/hot` |
| `/userlogin` | `LoginView.vue` | 账号、密码、图形验证码登录 | `GET /auth/captcha`，`POST /auth/login` |
| `/register` | `RegisterView.vue` | 用户名、手机、性别、密码、确认密码、图形验证码注册 | `GET /auth/captcha`，`POST /auth/register`（**§12** 性别字段） |
| `/forgot-password` | `ForgotPasswordView.vue` | 手机 → 短信码 → 新密码 | `POST /auth/sms`，`POST /auth/sms/verify`，`POST /auth/reset-password` |
| `/c/:id` | `PostDetailView.vue` | 帖子详情、点赞/收藏 UI、一级评论与回复、关注作者 | `GET /posts/:id`，`GET /posts/:id/comments`，`POST` 评论/回复/点赞，收藏与点赞接口；**关注**见 **§11** |
| `/post/create` | `CreatePostView.vue` | 选板块、正文 ≤2000 字、匿名、多图≤9、可选视频 | `POST /posts`（multipart，见 **§9**） |
| `/User/profile` | `UserProfileView.vue` | 当前用户资料、改密、我的发布/评论/收藏/关注/粉丝 | `GET/PUT /user/me`，`PUT /user/me/password`，`POST /user/me/avatar`，`GET /user/me/posts|comments|favorites`；**关注关系**见 **§11** |
| `/users/:id` | 同上 | 他人主页：资料、TA 的发布/评论/关注/粉丝 | `GET /users/:id` 及同上列表接口；**关注**见 **§11** |
| `/message` | `MessageView.vue` | Tab：互动通知、私信会话与聊天、系统通知 | `GET/PUT /notifications*`，`GET/POST/PUT /messages/*` |
| `/search` | `SearchView.vue` | 关键词搜索、排序：最新 / 最多赞 / 最多评论 | `GET /posts/search`（`keyword`、`sort`） |

### 3.2 管理端

| 路由 | 页面文件 | 功能摘要 | 接口前缀 |
|------|-----------|----------|----------|
| `/admin/login` | `AdminLoginView.vue` | 管理员账号、密码、验证码 | `POST /admin/auth/login` |
| `/admin` | `DashboardView.vue` | 统计卡片、最近帖子 | `GET /admin/dashboard/stats`，`GET /admin/dashboard/recent-posts` |
| `/admin/content` | `ContentManageView.vue` | 帖子筛选、状态、类型、匿名、批量删除、详情 | `GET /admin/posts`，`PUT .../approve|reject`，`DELETE /admin/posts` |
| `/admin/category` | `CategoryManageView.vue` | 分类树 CRUD | `/admin/categories` 全套 |
| `/admin/comments` | `CommentManageView.vue` | 评论筛选、批量删除 | `GET/DELETE /admin/comments` |
| `/admin/users` | `UserManageView.vue` | 用户列表、状态、角色、重置密码、导入导出 | `/admin/users*` |
| `/admin/roles` | `RoleManageView.vue` | 角色与权限 | `/admin/roles` 全套 |
| `/admin/sensitive` | `SensitiveWordsView.vue` | 敏感词 CRUD | `/admin/sensitive-words` 全套 |
| `/admin/announcement` | `AnnouncementView.vue` | 公告 CRUD、发布撤回 | `/admin/announcements` 全套 |
| `/admin/logs` | `LogManageView.vue` | 操作日志、登录日志 | `GET /admin/logs/operation`，`GET /admin/logs/login` |

---

## 4. 首页分类与 `GET /posts` 的 `category` 参数

顶栏与首页逻辑见 **`AppHeader.vue`**、**`HomeView.vue`**。

| `category` Query | 含义 | 后端 `GET /posts` 建议行为 |
|------------------|------|---------------------------|
| （不传）或空字符串 | 「最新墙」：全站帖子时间序 | 不按板块过滤。 |
| `social` | 「社交墙」：捞人 + 找搭子 + 恋爱 | **`category` 为 `social_find`、`social_buddy`、`social_love` 三类的并集**（OR）。 |
| `social_find` / `social_buddy` / `social_love` | 社交墙子类 | 精确匹配该 `category`。 |
| `share` / `trade` / `general` | 分享墙 / 买卖墙 / 综合墙 | 精确匹配。 |

若后端暂不支持 `category=social` 的 OR 语义，需与前端约定改为三次请求或其它方案；**推荐后端直接支持**，与当前 UI 一致。

---

## 5. 业务枚举（与页面文案一致）

### 5.1 帖子板块 `category`（字符串，精确值）

| 值 | 页面展示名 |
|----|------------|
| `social_find` | 捞人 |
| `social_buddy` | 找搭子 |
| `social_love` | 恋爱 |
| `share` | 分享墙 |
| `trade` | 买卖墙 |
| `general` | 综合墙 |

`PostCard.vue`、`CreatePostView.vue`、`PostDetailView.vue`、管理端筛选均使用上述 key。

### 5.2 搜索排序 `sort`（`GET /posts/search`）

与 **`SearchView.vue`** 一致：

| 值 | UI 文案 |
|----|---------|
| `latest` | 最新 |
| `likes` | 最多赞 |
| `comments` | 最多评论 |

未传时默认 **`latest`**。

### 5.3 管理端帖子 `status`（`GET /admin/posts` 筛选与列表展示）

与 **`ContentManageView.vue` / `DashboardView.vue`** Mock 一致，后端应用同一套枚举（可扩展，但勿随意改名）：

| 值 | 含义 |
|----|------|
| `normal` | 正常展示 |
| `pending` | 待审核 |
| `offline` | 下架 |
| `rejected` | 拒绝 |

筛选传 `all` 或空表示「不限」时由后端约定（建议：不传该字段 = 不限）。

### 5.4 管理端帖子 `type`（内容形态）

| 值 | 含义 |
|----|------|
| `text` | 纯文字 |
| `image` | 含图 |
| `video` | 含视频 |

### 5.5 互动通知 `type`（`GET /notifications`）

与 **`MessageView.vue`** Mock 的 `like` / `comment` / `reply` 一致。

### 5.6 敏感词 `strategy`

| 值 | 含义 |
|----|------|
| `block` | 拦截 |
| `review` | 送审 |
| `log` | 仅记录 |

### 5.7 用户启用状态（管理端 / 分类等）

| `status` | 含义 |
|----------|------|
| `0` | 禁用 |
| `1` | 启用 |

### 5.8 公告发布 `action`（`PUT /admin/announcements/:id/publish`）

| 值 | 含义 |
|----|------|
| `publish` | 发布 |
| `withdraw` | 撤回 |

---

## 6. 核心数据结构（后端 JSON 须能驱动当前 UI）

### 6.1 `UserInfo`（登录、注册、`GET /user/me`、`GET /users/:id`）

前端展示用到的字段（**建议全部返回**；类型为联调约定）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | number | 是 | 用户主键。 |
| `username` | string | 是 | 昵称。 |
| `avatar` | string | 否 | 头像 URL；空字符串时组件用占位图。 |
| `phone` | string | 否 | 可脱敏如 `138****1234`。 |
| `email` | string | 否 | |
| `gender` | string | 否 | 页面选项含 **男 / 女 / 保密**（`RegisterView`、`UserProfileView`）。 |
| `bio` | string | 否 | 个人简介。 |
| `major` | string | 否 | 专业；Mock 有，个人中心可展示。 |
| `joinedAt` | string | 否 | 入学或注册时间展示用。 |

### 6.2 帖子列表/详情 `Post`（`PostCard`、`PostDetail`）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | number | 是 | |
| `category` | string | 是 | §5.1 |
| `content` | string | 是 | 正文；列表可截断展示。 |
| `anonymous` | boolean | 是 | `true` 时作者展示为「匿名用户」，不展示真实头像昵称。 |
| `authorId` | number \| null | 是 | 匿名时可为 `null`；非匿名时必有，用于关注、跳转主页。 |
| `images` | string[] | 否 | 图片 URL 列表；无图传 `[]`。 |
| `time` | string | 是 | **相对时间文案**（如「5分钟前」）或 ISO 字符串；前端当前 Mock 为中文相对时间，后端可统一用 ISO，前端再格式化。 |
| `likes` | number | 是 | 点赞数，默认 0。 |
| `comments` | number | 是 | 一级评论条数（或评论总数，**需固定语义并在接口中注释**）；用于卡片展示。 |

**列表接口可扩展**：若后端在列表中直接嵌入 `author: UserInfo`，可减少前端请求；否则前端需再 `GET /users/:id` 拼作者（当前 Mock 是本地 `users` 表拼接）。

### 6.3 热门榜 `GET /posts/hot` 单项（`HotRankSidebar.vue`）

当前侧栏使用字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | number | 帖子 ID，点击跳转 `/c/:id`。 |
| `title` | string | 卡片上用正文摘要，后端可用 **`content` 前若干字** 或单独 `title` 字段，**二选一与前端约定**；推荐直接返回 `title` 避免前端再截断。 |
| `heat` | number | 可选；用于排序权重展示，无则前端可不显示热度数值。 |

### 6.4 评论 `Comment` 与回复（`PostDetailView` / `getCommentsByPostId`）

顶级评论：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | number | 是 | 一级评论 ID，`POST .../replies` 挂在该 ID 下。 |
| `authorId` | number | 是 | |
| `content` | string | 是 | |
| `time` | string | 是 | 同帖子 `time` 规则。 |
| `likes` | number | 是 | |
| `replies` | array | 是 | 子数组；无则 `[]`。 |

回复项：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | number | 是 | |
| `authorId` | number | 是 | |
| `content` | string | 是 | |
| `time` | string | 是 | |
| `likes` | number | 否 | 默认 0。 |

前端回复接口支持 **`replyToUserId`**（`number`，可选），用于 @ 某人。

### 6.5 消息中心（与 `MessageView.vue` Mock 对齐的字段建议）

**互动通知** `Notification`：`id`, `type`（§5.5）, `userId`（触发者）, `content`（摘要文案）, `time`, `read`（boolean）。

**私信会话** `ChatSession`：`id`（会话 id 或对方 `userId`，**需统一**；前端 Mock 用会话 id + `userId`）, `userId`, `lastMsg`, `time`, `unread`（integer）。

**单条私信** `Message`：`id`, `from`（`me` \| `other` 或 `senderId`/`receiverId` 由后端定义，**联调时前端改模板绑定**）, `content`, `time`。

**系统通知** `SystemMessage`：`id`, `type`（如 `announcement`/`warning`）, `title`, `content`, `time`, `read`。

---

## 7. 认证模块

### 7.1 `GET /auth/captcha`

无参数。

**响应（HTTP 200，方案 A 直出）**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `captchaId` | string | 服务端生成的验证码会话 id。 |
| `image` | string | 图片，建议 `data:image/png;base64,...`。 |

### 7.2 `POST /auth/login`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `account` | string | 是 | 与登录页一致：用户名或手机号（`LoginView` placeholder）。 |
| `password` | string | 是 | |
| `captcha` | string | 是 | 用户输入的图形验证码内容。 |
| `captchaId` | string | **条件** | **若后端校验图形码依赖 id，则必填**；与 `GET /auth/captcha` 配对。**当前 `src/api/auth.js` 未传此字段**，联调前前端需补上。 |

**响应**：`token` string，`user` 对象结构同 **§6.1**。

### 7.3 `POST /auth/register`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | |
| `phone` | string | 是 | 11 位手机号，后端校验。 |
| `password` | string | 是 | 建议最少 8 位；前端注册页有强度提示。 |
| `captcha` | string | 是 | |
| `gender` | string | **建议必填** | 与注册页一致：**男 / 女 / 保密**。**当前 `authApi.register` 未传**，联调前前端补传。 |

### 7.4 `POST /auth/sms`

| 字段 | 类型 | 必填 |
|------|------|------|
| `phone` | string | 是 |

### 7.5 `POST /auth/sms/verify`

| 字段 | 类型 | 必填 |
|------|------|------|
| `phone` | string | 是 |
| `code` | string | 是 |

**响应**：`resetToken` string，短期有效，仅用于下一步。

### 7.6 `POST /auth/reset-password`

| 字段 | 类型 | 必填 |
|------|------|------|
| `resetToken` | string | 是 |
| `newPassword` | string | 是 |

### 7.7 `POST /auth/logout`

需 Bearer。无 body。服务端应使当前 Token 失效。

---

## 8. 用户模块

### 8.1 `GET /user/me` · `PUT /user/me`

`PUT` body 字段均为可选，至少传一项；字段同 **§6.1** 子集（`username`、`phone`、`email`、`gender`、`bio`）。

### 8.2 `PUT /user/me/password`

| 字段 | 类型 | 必填 |
|------|------|------|
| `oldPassword` | string | 是 |
| `newPassword` | string | 是 |

### 8.3 `POST /user/me/avatar`

`multipart/form-data`，字段名 **`avatar`**（单文件）。  
**响应**：`avatarUrl` string（完整可访问 URL）。

### 8.4 `GET /user/me/posts` · `GET /user/me/comments` · `GET /user/me/favorites`

Query：**§2.1**。  
响应：`list` + `total`；元素分别为 **Post**、**Comment**（含帖子上下文字段若需）、**Post**。

### 8.5 `GET /users/:id`

路径参数 **`id`**：目标用户 id，integer。  
响应：公开主页用户信息，字段不少于 **§6.1**，是否隐藏手机邮箱由后端策略决定。

---

## 9. 帖子模块

### 9.1 `GET /posts`

Query：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `category` | string | 否 | §4 |
| `page` | integer | 否 | §2.1 |
| `pageSize` | integer | 否 | §2.1 |

响应：**§2.2** + `list[]` 为 **Post**（§6.2）。

### 9.2 `GET /posts/:id`

响应：单条 **Post**（§6.2），建议含 `author` 或足够字段供详情页展示。

### 9.3 `POST /posts`（`CreatePostView`：正文最多 **2000** 字）

`multipart/form-data`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `category` | string | 是 | §5.1 |
| `content` | string | 是 | 长度上限 **2000**（与页面 `maxlength` 一致）；超出 **400**。 |
| `anonymous` | string 或 boolean | 是 | 表单常为 `"true"`/`"false"` 字符串，后端需兼容。 |
| `images` | 文件，多 part | 否 | 同字段名 **`images`** 重复追加；**最多 9 个文件**（与页面一致）。单文件建议 ≤ **5MB**；类型 `image/jpeg`、`image/png`、`image/webp`、`image/gif`（超出返回 400）。 |
| `video` | 文件 | 否 | 单文件；大小上限由后端另定（建议 ≤ 100MB）。 |

**响应**：新建 **Post** 完整对象。

### 9.4 `DELETE /posts/:id`

删除本人帖子；**403** 若非作者。

### 9.5 `POST /posts/:id/like`

**响应**：`liked` boolean，`likeCount` integer。

### 9.6 `POST /posts/:id/favorite`

**响应**：`favorited` boolean。

### 9.7 `GET /posts/search`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | string | 是 | 空串时返回 **400** 或空 `list`（**选一种写死**；推荐空 `list` + `total:0`）。 |
| `sort` | string | 否 | §5.2 |
| `page` | integer | 否 | §2.1 |
| `pageSize` | integer | 否 | §2.1 |

### 9.8 `GET /posts/hot`

Query：`limit`，**§2.3**。  
响应：`HotPost[]`，字段 **§6.3**。

---

## 10. 评论模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/posts/:postId/comments` | Query：`page`,`pageSize` §2.1；响应 `list` + `total`，元素 §6.4 |
| POST | `/posts/:postId/comments` | JSON：`content` string 必填 |
| POST | `/comments/:commentId/replies` | JSON：`content` 必填，`replyToUserId` number 可选 |
| POST | `/comments/:commentId/like` | 响应：`liked`，`likeCount` |
| DELETE | `/comments/:commentId` | 删除本人评论 |

---

## 11. 消息模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notifications` | Query：`type?`（§5.5），`page`,`pageSize`；响应含 `unreadCount` |
| PUT | `/notifications/read-all` | 全部已读 |
| GET | `/messages/chats` | 会话列表 §6.5 |
| GET | `/messages/chats/:userId` | 与某用户的私信；Query 分页 §2.1 |
| POST | `/messages/chats/:userId` | JSON：`content` string |
| PUT | `/messages/chats/:userId/read` | 会话已读 |
| GET | `/messages/system` | 系统通知列表 + `unreadCount` |
| PUT | `/messages/system/:id/read` | 单条已读 |
| GET | `/messages/unread-count` | 响应：`total`,`notification`,`chat`,`system` 均为 integer |

---

## 12. 管理端模块（`/admin/*`）

所有接口需 **管理员** Bearer。

- **`POST /admin/auth/login`**：body `account` string，`password` string，`captchaAnswer` string；响应 `token`，`admin`（对象字段与前端管理布局展示协商即可）。  
- **`GET /admin/dashboard/stats`**：KPI，`totalUsers`、`newPosts`、`pendingReviews`、`totalComments` 等为 number。  
- **`GET /admin/dashboard/recent-posts`**：`limit` §2.3。  
- **`GET /admin/posts`**：Query 含 `userId`、`category`、`keyword`、`status`（§5.3）、`type`（§5.4）、`anonymous`（boolean 或 0/1）、`page`、`pageSize`。  
- **`PUT /admin/posts/:id/approve`**、**`PUT /admin/posts/:id/reject`**：`reject` 可选 body `reason` string。  
- **`DELETE /admin/posts`**：JSON body `{ "ids": number[] }`，**非空数组**。  
- **分类** `/admin/categories`：创建 `name` 必填，`parentId` number 可选，`sort` number 可选；更新可含 `name`、`status`（0\|1）、`sort`。  
- **评论** `GET/DELETE /admin/comments`：删除 body 同 `{ ids }`。  
- **用户** `GET /admin/users`：`keyword`、`status`（0\|1）、`roleId`、`page`、`pageSize`；`PUT .../status`、`.../role`、`.../reset-password`；`POST .../import` multipart（**文件字段名由后端指定后前端固定**）；`GET .../export` 同筛选参数，响应 **文件流**，`Content-Disposition` 带文件名。  
- **角色**、**敏感词**、**公告**、**日志**：路径与 `src/api/admin.js` 一致；敏感词 `strategy` §5.6；公告 `PUT .../publish` body `action` §5.8；日志 Query 含 `startTime`/`endTime` 建议 **ISO 8601**。

**DELETE + JSON body**：依赖服务端与网关支持；若不支持，改为 POST 批量删除路径并与前端同步。

---

## 13. 页面已有、但 `src/api` 尚未定义的能力（后端需补协议）

以下逻辑在 **`UserProfileView`、`PostCard`、`PostDetailView`** 已通过 **`useSocialStore`** 本地实现，**对接真实后端时必须新增接口**（路径为建议 REST，可与后端协商后改）：

| 能力 | 建议接口 | 说明 |
|------|----------|------|
| 关注用户 | `POST /users/:id/follow` | 成功返回 `{ following: true }` |
| 取消关注 | `DELETE /users/:id/follow` 或 `POST /users/:id/unfollow` | 成功返回 `{ following: false }` |
| 是否已关注 | 在 `GET /users/:id` 或 `GET /user/me` 中带 `following: boolean` 或由前端根据列表推断 | 需约定 |

未上线前，请勿删除上述页面功能；后端与前端共同确定路径后写入 **`src/api/user.js`**（或新文件）并更新本文档。

---

## 14. 联调检查清单（前端）

1. `auth.js`：`login` 是否增加 `captchaId`；`register` 是否增加 `gender`。  
2. `index.js`：401 清除 **`userInfo`** 与 `token` 一致。  
3. 各页面、store：用 `authApi` / `postApi` 等替换 Mock。  
4. 用户与管理员 Token 是否分存储（若需要）。

---

## 附录 A：`src/api/admin.js` 路径与方法速查

| 方法 | 路径 | 请求体 / Query 要点 |
|------|------|---------------------|
| POST | `/admin/auth/login` | JSON：`account`,`password`,`captchaAnswer` |
| GET | `/admin/dashboard/stats` | — |
| GET | `/admin/dashboard/recent-posts` | Query：`limit` §2.3 |
| GET | `/admin/posts` | Query：`userId`,`category`,`keyword`,`status`,`type`,`anonymous`,`page`,`pageSize` |
| PUT | `/admin/posts/:id/approve` | — |
| PUT | `/admin/posts/:id/reject` | JSON：`reason?` |
| DELETE | `/admin/posts` | JSON body：`{ ids: number[] }` |
| GET | `/admin/categories` | — |
| POST | `/admin/categories` | JSON：`name`，`parentId?`，`sort?` |
| PUT | `/admin/categories/:id` | JSON：`name?`，`status?`(0\|1)，`sort?` |
| DELETE | `/admin/categories/:id` | — |
| GET | `/admin/comments` | Query：`keyword`,`userId`,`postId`,`page`,`pageSize` |
| DELETE | `/admin/comments` | JSON body：`{ ids: number[] }` |
| GET | `/admin/users` | Query：`keyword`,`status`(0\|1)，`roleId`,`page`,`pageSize` |
| PUT | `/admin/users/:id/status` | JSON：`status`(0\|1) |
| PUT | `/admin/users/:id/role` | JSON：`roleId` number |
| PUT | `/admin/users/:id/reset-password` | —；响应含 `newPassword` string |
| POST | `/admin/users/import` | multipart，文件字段名前后端书面约定 |
| GET | `/admin/users/export` | Query 同 users 列表；响应二进制文件流 |
| GET | `/admin/roles` | — |
| POST | `/admin/roles` | JSON：`name`，`permissions: string[]` |
| PUT | `/admin/roles/:id` | JSON：`name?`，`permissions?` |
| DELETE | `/admin/roles/:id` | — |
| GET | `/admin/sensitive-words` | Query：`keyword`,`category`,`strategy`,`page`,`pageSize` |
| POST | `/admin/sensitive-words` | JSON：`word`,`category`,`strategy` |
| PUT | `/admin/sensitive-words/:id` | JSON：部分更新字段对象 |
| DELETE | `/admin/sensitive-words/:id` | — |
| GET | `/admin/announcements` | Query：`status`,`page`,`pageSize` |
| POST | `/admin/announcements` | JSON：`title`,`content`,`type`,`displayMethod`(string[])，`status` |
| PUT | `/admin/announcements/:id` | JSON：部分更新 |
| PUT | `/admin/announcements/:id/publish` | JSON：`action`=`publish`\|`withdraw` |
| DELETE | `/admin/announcements/:id` | — |
| GET | `/admin/logs/operation` | Query：`keyword`,`module`,`type`,`startTime`,`endTime`,`page`,`pageSize` |
| GET | `/admin/logs/login` | Query：`keyword`,`status`,`startTime`,`endTime`,`page`,`pageSize` |

---

**文档版本**：与仓库 `src/api`、`src/router`、`src/views`、`src/stores` 同步维护；接口或页面变更时请更新对应章节。
