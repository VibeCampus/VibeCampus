# VibeCampus

**核心价值：为同学们提供交友服务**——在同一校园里，帮助大家更容易认识新朋友、发起互动、建立信任与归属感。产品以 **校园墙 / 校园 BBS** 为载体：信息流、分区发帖、点赞与评论等能力，都是围绕「让人与人连起来」而设计；管理后台则保障社区氛围与安全边界。

**在线仓库：** [github.com/VibeCampus/VibeCampus](https://github.com/VibeCampus/VibeCampus)

---

## 项目是做什么的？

| 问题           | 说明                                                                          |
| ------------ | --------------------------------------------------------------------------- |
| **解决什么**     | 把「想认识同校同学、想被看见、想有话题可聊」的需求，落到可浏览、可回复、可持续互动的线上空间里；表白墙等分区既是校园日常话题入口，也是自然破冰的场景。 |
| **谁在用**      | 学生是交友与内容的主角；管理员维护秩序，避免骚扰与违规内容伤害体验。                                          |
| **和通用论坛的区别** | 场景与身份都锚定**本校**：更利于同圈层匹配与线下延伸；技术上演进上可支持实名/匿名、统一身份认证等（见版本规划）。                 |

一句话判断：**这是面向校园同学的交友与社交产品，配套多端与后台治理能力；后端以统一 REST 支撑各端。**

---

## 主要功能（怎么理解这个产品）

以下为**产品能力**视角的归纳，侧重「如何支撑交友与互动」；实现进度以仓库代码与 [《校园 bbs 开发技术文档》](docs/校园bbs开发技术文档.md) 中的版本规划为准。

**MVP 阶段目标（发帖—浏览—互动闭环）**

- **账号与个人**：学号 + 密码注册登录（先做本地账号）；个人主页展示昵称、头像、发帖数等。
- **内容与浏览**：发布文字 + 最多 9 张图；帖子分类（如表白墙、二手、失物招领等——既服务交友话题，也覆盖校园生活信息）；首页信息流按时间倒序；帖子详情与点赞。
- **互动**：帖子下的单级评论；点赞仅展示数量（无点赞列表）——降低互动门槛，方便表达好感或参与话题。
- **管理**：后台删除违规帖子、封禁账号等基础治理能力。

**后续大版本方向（节选）**

- 校园统一身份认证、前台匿名发帖与后台实名、视频与二级评论、敏感词与待审核流、举报、搜索与 Redis 缓存、热度榜等；长期还可扩展私信、通知等更贴近「一对一交友与关系维护」的能力（详见技术文档 **1.0 / 2.0** 章节）。

---

## 多端形态

| 端            | 目录                       | 说明                                           |
| ------------ | ------------------------ | -------------------------------------------- |
| **PC Web**   | `VibeCampus-Frontend`    | Vue 3 技术栈，适合浏览器内完整浏览与发帖。                     |
| **小程序 / H5** | `VibeCampus-MiniProgram` | UniApp，一套代码多端发布，与 PC 共用同一套 REST。             |
| **管理后台**     | `VibeCampus-AdminPanel`  | 面向管理员的内容与账号管理界面。                             |
| **后端 API**   | `VibeCampus-Backend`     | Spring Boot 聚合多模块，对外统一 REST（建议前缀 `/api/v1`）。 |

---

## 技术栈一览

| 方向 | 选型 |
| ---- | ---- |
| 后端 | Java 21，Spring Boot 4.x，MyBatis，Spring Security，MySQL |
| PC 端 | Vue 3，Vite，Element Plus，Pinia |
| 移动端 | UniApp（微信小程序、H5 等） |

更完整的选型表、中间件与演进路线见技术文档中的 **「技术栈选用」**。

---

## 仓库里有什么？（结构速览）

```
VibeCampus/
├── VibeCampus-Backend/      # 后端：Maven 多模块（如 user / post / comment / admin），启动入口 vibecampus-bootstrap
├── VibeCampus-Frontend/     # PC 端 Web
├── VibeCampus-MiniProgram/  # 小程序与移动端
├── VibeCampus-AdminPanel/   # 管理端
└── docs/                    # 需求、设计、技术与协作说明
```

后端业务包根名为 `cn.ayeez.vibecampus`；各业务模块内按 **controller → service → mapper** 分层，便于阅读与协作。

---

## 文档与协作

| 文档                                            | 适合谁读         | 内容                                           |
| --------------------------------------------- | ------------ | -------------------------------------------- |
| [《项目开发指南》](<docs/开发环境搭建及团队开发流程说明(项目开发指南).md>) | 参与者          | 本地环境、后端 `local` / `test` / `prod` 配置、Git 流程等 |
| [《校园 bbs 开发技术文档》](docs/校园bbs开发技术文档.md)        | 产品 / 开发 / 架构 | 版本规划、难点、技术选型与演进                              |

环境搭建与分支约定**不在本 README 展开**，请直接打开《项目开发指南》。

---

## GitHub Actions CI/CD（测试 / 生产）

仓库已提供自动部署工作流：`.github/workflows/cicd-deploy.yml`。

- `develop` 分支发生 `push` / `pull_request` 时：自动部署到**测试环境**
- `main` 分支发生 `push` / `pull_request` 时：自动部署到**生产环境**
- 仅当 `VibeCampus-Backend` / `VibeCampus-Frontend` / `deploy` 有变更时触发部署，并按变更范围只重建对应服务
- 测试与生产可在同一台云服务器上运行，使用不同 Compose project（`vibecampus-test`、`vibecampus-prod`）隔离容器与数据卷

需在 GitHub 仓库 Secrets 中配置：

- 公共：`SERVER_HOST`、`SERVER_PORT`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_BASE_DIR`
- 测试：`TEST_MYSQL_ROOT_PASSWORD`、`TEST_MYSQL_DATABASE`、`TEST_MYSQL_USER`、`TEST_MYSQL_PASSWORD`、`TEST_WEB_PORT`
- 生产：`PROD_MYSQL_ROOT_PASSWORD`、`PROD_MYSQL_DATABASE`、`PROD_MYSQL_USER`、`PROD_MYSQL_PASSWORD`、`PROD_WEB_PORT`

---

## 本地快速开始（后端 + MySQL）

后端默认使用 `local` Profile（见 `VibeCampus-Backend/vibecampus-bootstrap/src/main/resources/application.yml`），本地运行前需要准备 MySQL 与本地配置文件。

- **初始化数据库（推荐脚本一键完成建库 + 建表）**：
  - 执行：`schema.mysql.sql`
  - 该脚本会创建数据库 `vibecampus` 并创建所有业务表。
- **本地数据源配置**：
  - 位置：`VibeCampus-Backend/vibecampus-bootstrap/src/main/resources/`
  - 复制：`application-local.example.yml` → `application-local.yml`
  - 填写本机 MySQL 的 `username/password`（`application-local.yml` 已被 `.gitignore` 忽略，**不要提交**）
- **启动后端**：
  - 运行 `vibecampus-bootstrap` 模块中的 `cn.ayeez.vibecampus.VibeCampusApplication`

更完整步骤与三环境（local/test/prod）配置说明见：[《项目开发指南》](<docs/开发环境搭建及团队开发流程说明(项目开发指南).md>)。

## 参与贡献

欢迎通过 Issue / Pull Request 参与。合并前请阅读 [《项目开发指南》](<docs/开发环境搭建及团队开发流程说明(项目开发指南).md>) 中的分支与提交约定，并**勿提交**数据库密码、`application-local.yml` 等敏感文件。

---

## 许可证

仓库当前未附带 `LICENSE`；若计划开源或对外分发，需由维护者明确许可条款。
