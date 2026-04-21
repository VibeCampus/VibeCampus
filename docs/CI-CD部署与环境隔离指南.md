# VibeCampus CI/CD 部署与环境隔离指南（完整实战版）

本文是本项目 CI/CD 联调全过程的完整沉淀，覆盖了从需求定义、方案选型、工作流设计、Secrets 管理、环境隔离、测试验证、故障定位到最终稳定落地的全部关键经验。

目标是让任何新成员仅靠本文即可完成以下事项：

- 理解当前 CI/CD 体系如何工作。
- 在不踩坑的前提下维护测试/生产双环境。
- 复用我们已经验证过的排障路径。
- 在后续网络或部署条件变化时，快速在方案 A / 方案 B 之间切换。

---

## 1. 项目部署目标与约束

### 1.1 核心目标

- 使用 GitHub Actions 自动部署。
- `develop` 负责测试环境自动部署。
- `main` 负责生产环境自动部署。
- 前端与后端按代码变更范围进行增量更新。
- 测试与生产部署在同一台云服务器，彼此隔离，互不影响。

### 1.2 现实约束

- 服务器网络可能无法稳定直连 Docker Hub。
- 同机部署必须避免端口冲突、容器名冲突、卷数据互相覆盖。
- 敏感配置不能进入仓库，必须通过 Secrets 注入。

---

## 2. 最终当前方案（已落地）

当前生效方案是 **方案 A（服务器构建）**：

1. GitHub Actions 检测变更并上传源码到服务器。
2. 服务器执行 `docker compose ... up -d --build`。
3. 由服务器本地 Docker 完成构建与启动。

为什么当前回到方案 A：

- 服务器镜像加速源配置完成后，基础镜像拉取可用。
- 方案 A 相比方案 B 维护更简单（少一层镜像打包/传输/仓库管理）。

---

## 3. CI/CD 触发规则与分支语义

工作流文件：`.github/workflows/cicd-deploy.yml`

### 3.1 分支触发

- `develop`：
  - `push` 和 `pull_request`（目标是 `develop`）触发测试部署。
- `main`：
  - `push` 和 `pull_request`（目标是 `main`）触发生产部署。

### 3.2 路径触发（减少无效部署）

仅当以下目录有变更时才触发部署：

- `VibeCampus-Backend/**`
- `VibeCampus-Frontend/**`
- `deploy/**`
- `.github/workflows/cicd-deploy.yml`

### 3.3 变更服务计算逻辑

通过 paths filter 输出三个布尔值：

- `backend` 是否变化
- `frontend` 是否变化
- `deploy` 是否变化

部署逻辑：

- 若 `deploy` 变化，视为前后端都需要重建。
- 若仅 `backend` 变化，只更新 `backend`。
- 若仅 `frontend` 变化，只更新 `web`。

---

## 4. 环境隔离架构（同机部署关键）

### 4.1 Compose project 隔离

- 测试环境 project：`vibecampus-test`
- 生产环境 project：`vibecampus-prod`

隔离效果：

- 容器名不同（例如 `vibecampus-test-web-1` / `vibecampus-prod-web-1`）
- Docker 网络不同
- 数据卷不同（MySQL 数据天然隔离）

### 4.2 配置隔离

- 测试 compose：`deploy/test/docker-compose.yml`
- 生产 compose：`deploy/prod/docker-compose.yml`
- 测试 profile：`SPRING_PROFILES_ACTIVE=test`
- 生产 profile：`SPRING_PROFILES_ACTIVE=prod`

### 4.3 端口隔离

- 测试前端端口：`TEST_WEB_PORT`（建议 8081）
- 生产前端端口：`PROD_WEB_PORT`（通常 80）

如果测试不可访问但容器正常，优先检查云安全组是否开放 `TEST_WEB_PORT`。

---

## 5. Secrets 设计与注入规范

在仓库 `Settings -> Secrets and variables -> Actions -> Repository secrets` 中配置。

### 5.1 公共 Secrets

- `SERVER_HOST`：服务器公网 IP 或域名
- `SERVER_PORT`：SSH 端口（通常 22）
- `SERVER_USER`：SSH 用户（如 `root`）
- `SERVER_SSH_KEY`：私钥全文（必须是私钥，不是 `.pub`）
- `SERVER_BASE_DIR`：服务器部署根目录（如 `/opt/vibecampus`）

### 5.2 测试 Secrets

- `TEST_MYSQL_ROOT_PASSWORD`
- `TEST_MYSQL_DATABASE`
- `TEST_MYSQL_USER`
- `TEST_MYSQL_PASSWORD`
- `TEST_WEB_PORT`

### 5.3 生产 Secrets

- `PROD_MYSQL_ROOT_PASSWORD`
- `PROD_MYSQL_DATABASE`
- `PROD_MYSQL_USER`
- `PROD_MYSQL_PASSWORD`
- `PROD_WEB_PORT`

### 5.4 常见误区

- `SERVER_PORT` 是 SSH 端口，不是业务 HTTP 端口。
- `SERVER_SSH_KEY` 必须保留多行格式，包含 `BEGIN/END`。
- `TEST_MYSQL_ROOT_PASSWORD` 与 `TEST_MYSQL_PASSWORD` 不同用途，不建议混用。

---

## 6. MySQL 用户与密码语义（必须分清）

### 6.1 两种密码

- `MYSQL_ROOT_PASSWORD`：root 管理员密码（运维/管理用）
- `MYSQL_PASSWORD`：业务账号密码（应用连接用）

### 6.2 后端实际连接

后端通过以下变量连接数据库：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`（来自 `MYSQL_USER`）
- `SPRING_DATASOURCE_PASSWORD`（来自 `MYSQL_PASSWORD`）

### 6.3 数据是否会被 CI/CD 覆盖

正常情况下不会：

- 数据由 Docker volume 持久化。
- `docker compose up -d --build` 不会清空卷。
- 仅当手动执行 `down -v` 或删除卷时，才会重置数据。

---

## 7. 工作流执行链路（当前方案 A）

### 7.1 测试环境链路

1. Actions checkout 代码。
2. 计算服务变更（backend/web）。
3. scp 上传源码到 `${SERVER_BASE_DIR}/test/source`。
4. ssh 到服务器，写入 `deploy/test/.env`。
5. 执行：
   - `docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env up -d --build $SERVICES`

### 7.2 生产环境链路

同上，路径与 compose 文件替换为 `prod` 版本，project 名为 `vibecampus-prod`。

---

## 8. 部署成功验证标准

### 8.1 容器层

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

测试期望：

- `vibecampus-test-mysql-1` 为 `healthy`
- `vibecampus-test-backend-1` 为 `Up`
- `vibecampus-test-web-1` 有 `0.0.0.0:<TEST_WEB_PORT>->80`

### 8.2 主机本地 HTTP

```bash
curl -I http://127.0.0.1:<TEST_WEB_PORT>
```

返回 `200` 表示容器与端口映射正常。

### 8.3 外网 HTTP

```bash
curl -I http://<SERVER_HOST>:<TEST_WEB_PORT>
```

若本机可访问、外网不可访问，问题在云安全组/云防火墙，不在容器。

### 8.4 应用探针（本次联调加入）

- 前端首页显示：`部署验证: cicd-check-20260421`
- `/api/ping` 返回：`OK-cicd-check-20260421`

---

## 9. 本次多轮联调时间线（决策复盘）

### 阶段 1：初版自动部署落地

- 完成 `develop -> test` 与 `main -> prod` 自动部署。
- 完成同机双环境隔离（compose project + 端口）。
- 将敏感配置从代码迁移到 Secrets。

### 阶段 2：SSH 密钥问题修复

现象：

- `ssh.ParsePrivateKey: ssh: no key found`
- `unable to authenticate`

处理：

- 重新生成密钥对。
- 公钥写入服务器 `authorized_keys`。
- 私钥完整内容写入 `SERVER_SSH_KEY`。

### 阶段 3：服务器拉 Docker Hub 超时

现象：

- `registry-1.docker.io:443 i/o timeout`

处理过程：

1. 尝试方案 A 失败（服务器构建拉不到基础镜像）。
2. 临时切到方案 B（Runner 预构建与传输镜像）。
3. 因上传大 tar 存在耗时与权限问题（`Permission denied`），补了权限修复。
4. 用户要求回归方案 A，继续排查服务器网络。

### 阶段 4：网络根因定位与修复

关键诊断结论：

- DNS 正常，但 443 直连 Docker Hub 超时。
- 可用服务器同样无法 `curl registry-1.docker.io`，但 `docker pull` 成功。
- 根因不是直连恢复，而是 Docker mirror 生效。

最终动作：

- 复制可用服务器 `/etc/docker/daemon.json`（`registry-mirrors` + `dns`）到问题机。
- 重启 Docker 后，`docker pull` 恢复。
- 工作流最终稳定回到方案 A。

### 阶段 5：测试环境不可访问（已修复）

现象：

- `docker ps` 正常，`curl 127.0.0.1:8081` 正常，外网打不开。

结论：

- 云入口策略未放通测试端口。

处理：

- 在云平台开放测试端口（如 8081）后恢复。

---

## 10. 故障排查手册（按概率排序）

### 10.1 类别 A：密钥认证失败

检查项：

1. `SERVER_SSH_KEY` 是否是私钥（不是 `.pub`）。
2. 是否保持原始多行格式。
3. 服务器 `~/.ssh/authorized_keys` 是否包含对应公钥。
4. 权限是否正确：`700 ~/.ssh`、`600 authorized_keys`。

### 10.2 类别 B：服务器构建时拉镜像超时

检查项：

```bash
getent hosts registry-1.docker.io
nc -vz registry-1.docker.io 443
curl -Iv --connect-timeout 10 https://registry-1.docker.io/v2/
docker pull maven:3.9.9-eclipse-temurin-21
```

判定：

- `curl` 超时 + `pull` 失败：出网问题未解决。
- `curl` 超时 + `pull` 成功：mirror 已生效，可继续用方案 A。

### 10.3 类别 C：测试环境访问不到

检查路径：

1. `docker ps` 是否映射 `0.0.0.0:8081->80`
2. `curl 127.0.0.1:8081` 是否 200
3. 云安全组是否开放 8081
4. 云主机防护/云防火墙是否拦截

---

## 11. Docker mirror 基线配置（已验证）

可用服务器配置如下，建议作为标准模板：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://huecker.io",
    "https://dockerhub.timeweb.cloud",
    "https://noohub.ru"
  ],
  "dns": ["223.5.5.5", "8.8.8.8"]
}
```

应用与验证：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
docker info | sed -n '/Registry Mirrors/,+8p'
docker pull maven:3.9.9-eclipse-temurin-21
```

---

## 12. 方案 B 历史与保留切换条件

### 12.1 什么是方案 B

- CI 在 GitHub Runner 构建镜像。
- 再将镜像传服务器或推私有仓库。
- 服务器仅 `docker load`/`docker pull` + `compose up`。

### 12.2 为什么保留方案 B 预案

- 当服务器镜像拉取仍不稳定时，方案 B 成功率更高。
- 适合“网络受限但 CI runner 网络好”的场景。

### 12.3 何时建议再次切换到 B

- 服务器频繁出现 Docker Hub 拉取超时。
- 方案 A 在可接受时间内无法稳定通过。
- 团队愿意维护额外的镜像仓库/传输复杂度。

---

## 13. 安全与合规要求

必须遵守：

- 任何 `.env`、私钥、密码、token 不提交仓库。
- `application-local.yml` 仅本地使用，不入库。
- Secrets 只放 GitHub Actions，不出现在日志和文档示例真实值中。

建议：

- 测试与生产使用不同数据库密码。
- 生产 root 密码定期轮换。
- 关键 Secrets 变更后做一次完整回归部署。

---

## 14. 新机器初始化流程（标准 SOP）

1. 安装 Docker / Compose。
2. 配置并验证 Docker mirror。
3. 创建部署目录（如 `/opt/vibecampus`）并赋权给部署用户。
4. 确认 SSH 密钥登录可用。
5. 云安全组开放：
   - SSH（22）
   - 生产端口（80 或 443）
   - 测试端口（如 8081）
6. 在 GitHub 配置完整 Secrets。
7. 首次推送 `develop` 验证测试环境。
8. 验证后推送 `main` 验证生产环境。

---

## 15. 运维常用命令清单

### 15.1 服务状态

```bash
docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env ps
docker compose --project-name vibecampus-prod -f deploy/prod/docker-compose.yml --env-file deploy/prod/.env ps
```

### 15.2 查看日志

```bash
docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env logs --tail=200 web
docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env logs --tail=200 backend
```

### 15.3 重启单服务

```bash
docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env up -d --build web
docker compose --project-name vibecampus-test -f deploy/test/docker-compose.yml --env-file deploy/test/.env up -d --build backend
```

### 15.4 健康探针

```bash
curl -I http://127.0.0.1:8081
curl -s http://127.0.0.1:8081/api/ping
```

---

## 16. 最终经验总结（关键结论）

1. **同机双环境可行且稳定**：通过 Compose project + 端口隔离即可。
2. **Secrets 规范是稳定基础**：尤其 `SERVER_SSH_KEY` 和数据库相关变量。
3. **网络问题需分层排查**：DNS、443 连通、docker pull、云安全组各自独立。
4. **不要只看 `curl Docker Hub` 结果**：mirror 生效时 `curl` 可失败但 `docker pull` 仍成功。
5. **部署成功不等于可外网访问**：容器正常后仍要检查云入口端口是否放通。
6. **方案 A/B 不应二选一极端化**：A 为主，B 为网络异常时的应急预案。

---

## 17. 最终验收清单（全链路）

- [ ] `develop` 触发测试部署成功
- [ ] `main` 触发生产部署成功
- [ ] 测试与生产容器同机共存
- [ ] 测试与生产数据库卷隔离
- [ ] 测试前端可公网访问
- [ ] 生产前端可公网访问
- [ ] `/api/ping` 返回部署探针状态
- [ ] 代码仓库不包含任何明文敏感信息
- [ ] 服务器 Docker mirror 配置可复现
- [ ] 团队成员可按本文独立完成部署与排障

