# VibeCampus CI/CD 部署与环境隔离指南

本文汇总本项目在多轮联调中确认过的部署方案、环境隔离策略、GitHub Actions 配置、Docker 运行方式与常见问题排查路径，作为后续团队统一参考文档。

---

## 1. 目标与范围

- 使用 GitHub Actions 自动化部署测试环境与生产环境。
- `develop` 分支变更自动部署到测试环境，`main` 分支变更自动部署到生产环境。
- 测试与生产部署在同一台云服务器，但通过 Docker Compose project 隔离容器、网络、数据卷。
- 后端使用 Spring Profile 区分环境（`test` / `prod`）。
- 敏感配置不入库，统一使用 GitHub Secrets 注入。

---

## 2. 当前采用的部署方案（方案 A）

当前已回到方案 A（服务器端构建）：

1. GitHub Actions 检测变更并上传源码到服务器。
2. 服务器执行 `docker compose ... up -d --build`。
3. Docker 在服务器构建并启动对应服务（`backend` / `web` / `mysql`）。

说明：

- 仅重建有变更的服务（`backend` 或 `web`）。
- MySQL 服务由 compose 管理，无需手动单独部署 MySQL 容器。

---

## 3. 分支与触发策略

工作流文件：`.github/workflows/cicd-deploy.yml`

- `develop`：
  - `push` / `pull_request` 触发测试环境部署。
- `main`：
  - `push` / `pull_request` 触发生产环境部署。
- 触发路径限制：
  - `VibeCampus-Backend/**`
  - `VibeCampus-Frontend/**`
  - `deploy/**`
  - `.github/workflows/cicd-deploy.yml`

---

## 4. 环境隔离设计（同机部署）

### 4.1 Compose project 隔离

- 测试：`vibecampus-test`
- 生产：`vibecampus-prod`

影响：

- 容器名隔离
- 网络隔离
- volume 隔离

即使测试与生产的库名都叫 `vibecampus`，也不是同一个 MySQL 实例数据。

### 4.2 端口隔离

- 测试前端端口建议使用 `TEST_WEB_PORT=8081`（或其他非 80）
- 生产前端端口一般使用 `PROD_WEB_PORT=80`

---

## 5. 配置文件与目录约定

- 测试 compose：`deploy/test/docker-compose.yml`
- 生产 compose：`deploy/prod/docker-compose.yml`
- 测试环境变量模板：`deploy/test/.env.example`
- 生产环境变量模板：`deploy/prod/.env.example`

后端 profile：

- 测试环境：`SPRING_PROFILES_ACTIVE=test`
- 生产环境：`SPRING_PROFILES_ACTIVE=prod`

后端配置文件：

- `VibeCampus-Backend/vibecampus-bootstrap/src/main/resources/application-test.yml`
- `VibeCampus-Backend/vibecampus-bootstrap/src/main/resources/application-prod.yml`

---

## 6. GitHub Secrets 配置清单

在仓库 `Settings -> Secrets and variables -> Actions -> Repository secrets` 中配置：

### 6.1 公共

- `SERVER_HOST`
- `SERVER_PORT`（SSH 端口，通常 22）
- `SERVER_USER`
- `SERVER_SSH_KEY`（私钥全文，非 `.pub`）
- `SERVER_BASE_DIR`（如 `/opt/vibecampus`）

### 6.2 测试环境

- `TEST_MYSQL_ROOT_PASSWORD`
- `TEST_MYSQL_DATABASE`
- `TEST_MYSQL_USER`
- `TEST_MYSQL_PASSWORD`
- `TEST_WEB_PORT`

### 6.3 生产环境

- `PROD_MYSQL_ROOT_PASSWORD`
- `PROD_MYSQL_DATABASE`
- `PROD_MYSQL_USER`
- `PROD_MYSQL_PASSWORD`
- `PROD_WEB_PORT`

---

## 7. MySQL 密码与应用连接关系

两个密码角色不同：

- `MYSQL_ROOT_PASSWORD`：MySQL root 管理员密码。
- `MYSQL_PASSWORD`：业务用户（`MYSQL_USER`）密码，后端通常用这个连接数据库。

后端连接由以下环境变量注入：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

注意：

- 反复 CI/CD 不会自动清空数据（数据在 volume 中持久化）。
- 只有删除 volume（如 `docker compose down -v`）才会重置数据库。

---

## 8. 部署验证方法

### 8.1 前端验证

访问：

- 测试：`http://<SERVER_HOST>:<TEST_WEB_PORT>`
- 生产：`http://<SERVER_HOST>:<PROD_WEB_PORT>`（若为 80 可省略端口）

当前已加入前端可见部署探针：

- 首页显示：`部署验证: cicd-check-20260421`

### 8.2 后端验证

访问 `/api/ping`，返回中包含部署探针状态：

- `OK-cicd-check-20260421`

---

## 9. 已遇到的典型问题与结论

### 9.1 `scp-action` 认证失败

报错特征：

- `ssh.ParsePrivateKey: ssh: no key found`
- `unable to authenticate`

结论：

- `SERVER_SSH_KEY` 内容不正确或格式损坏。
- 必须使用完整私钥内容（多行，含 BEGIN/END），并确保服务器 `authorized_keys` 正确。

### 9.2 服务器拉 Docker Hub 超时

报错特征：

- `dial tcp ... registry-1.docker.io:443: i/o timeout`

结论：

- 非 workflow 逻辑问题，是服务器出网链路问题（DNS 正常但 443 不通）。
- 可通过 Docker `registry-mirrors` 绕过直连 Docker Hub。

### 9.3 “另一台服务器可以拉镜像”

结论：

- 可用机并非直连 Docker Hub，而是 Docker daemon 配置了镜像加速源并生效。
- 关键配置在 `/etc/docker/daemon.json`（`registry-mirrors` + `dns`）。

---

## 10. 服务器出网排查最小路径

```bash
getent hosts registry-1.docker.io
nc -vz registry-1.docker.io 443
curl -Iv --connect-timeout 10 https://registry-1.docker.io/v2/
docker pull maven:3.9.9-eclipse-temurin-21
```

判定：

- DNS 通但 443 超时：云网络出网策略/NAT/路由问题。
- `curl` 失败但 `docker pull` 成功：通常是镜像加速源在工作。

---

## 11. Docker 镜像加速建议配置（示例）

`/etc/docker/daemon.json` 示例：

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

应用配置：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
docker info | sed -n '/Registry Mirrors/,+8p'
```

---

## 12. 关于方案 B（构建推镜像仓库）历史说明

我们曾切换到方案 B（CI 构建镜像并上传/拉取镜像）以规避 Docker Hub 出网问题；后续在服务器侧镜像源可用后，已切回方案 A。

保留结论：

- 若后续再次出现服务器无法稳定拉基础镜像，可快速切回方案 B 或接入国内私有镜像仓库（ACR/TCR/Harbor）。

---

## 13. 当前团队操作建议

1. 先保持方案 A，简化维护成本。
2. 保留两套服务器（可用镜像源配置作为基线）。
3. 每次改动前检查：
   - Secrets 是否完整
   - 测试/生产端口是否冲突
   - `/api/ping` 与前端探针是否可见
4. 不在仓库提交任何真实密钥、密码、`.env` 明文。

---

## 14. 快速检查清单（Checklist）

- [ ] `develop` 推送可触发测试部署
- [ ] `main` 推送可触发生产部署
- [ ] 测试与生产容器可同机共存
- [ ] 测试与生产数据库数据隔离
- [ ] 测试前端可访问并看到部署探针
- [ ] `/api/ping` 返回部署探针状态
- [ ] CI/CD 不包含明文敏感信息

