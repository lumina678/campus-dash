# CampusRelay 校园跑腿平台

CampusRelay 是一个面向校园跑腿场景的全栈项目，覆盖任务发布、并发抢单、超时流转、资金托管、结算退款、信用分、通知和实时状态更新。

## 功能概览

- 任务发布、任务广场、抢单、取货、送达与确认
- Redis Lua 与数据库条件更新共同控制并发抢单
- 资金托管、结算、退款、仲裁与对账
- RocketMQ 定时消息与后台扫描处理超时任务
- Redis 缓存、布隆过滤器和缓存一致性检查
- Session/JWT 鉴权、刷新令牌与 WebSocket 通知
- React + TypeScript 前端

## 技术栈

- Java 21、Spring Boot、Maven
- MySQL 8、Redis 7、RocketMQ 5
- ShardingSphere、Sentinel
- React 18、TypeScript、Vite
- Docker Compose

## 目录结构

```text
campus-relay-backend/
  relay-domain/          领域模型与端口
  relay-application/     应用用例
  relay-infrastructure/  数据库、缓存和消息适配器
  relay-presentation/    REST API 与 WebSocket
  relay-bootstrap/       主应用与集成测试
  relay-worker/          后台任务与消息消费者
  relay-bench/           压测客户端
  docker/               本地中间件编排
campus-relay-frontend/   React 前端
```

更详细的设计说明见 [ARCHITECTURE.md](campus-relay-backend/docs/ARCHITECTURE.md)。

## 本地运行

### 1. 准备环境变量

```bash
cp .env.example .env
```

打开 `.env`，设置数据库密码和至少 32 字节的 JWT 密钥。随后载入变量：

```bash
set -a
source .env
set +a
```

### 2. 启动中间件

```bash
cd campus-relay-backend/docker
docker compose --env-file ../../.env up -d
./init-mq.sh
cd ../..
```

### 3. 启动后端

```bash
cd campus-relay-backend
mvn clean test
mvn -pl relay-bootstrap spring-boot:run
```

如需后台任务，在另一个终端启动：

```bash
cd campus-relay-backend
mvn install -DskipTests
java -jar relay-worker/target/relay-worker-1.0.0-SNAPSHOT.jar
```

### 4. 启动前端

```bash
cd campus-relay-frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。

## 主要环境变量

| 变量 | 用途 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | Docker 中的 MySQL root 密码 |
| `DB_PASSWORD` | 应用连接 MySQL 的密码 |
| `RELAY_AUTH_JWT_SECRET` | JWT 签名密钥，至少 32 字节 |
| `DB_URL` | 可选，自定义 JDBC 地址 |
| `DB_USERNAME` | 可选，默认 `root` |

## 授权

本仓库依据合法授权发布。除 [NOTICE](NOTICE.md) 明确说明外，不额外授予复制、修改或再分发许可。
