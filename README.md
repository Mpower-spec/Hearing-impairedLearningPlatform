# 声学无碍学习平台

面向听障学生的无障碍学习平台，围绕“字幕优先、课堂同步、课后复习”提供教师端、学生端、实时课堂、视频学习、AI 学习资料、课程问答、手语数字人和学习社区。

本 README 以**可复现**为首要目标：说明如何从一台干净机器完成依赖安装、环境配置、数据库初始化、服务启动和结果验收。

## 目录

- [复现性声明](#复现性声明)
- [环境与依赖清单](#环境与依赖清单)
- [环境变量模板](#环境变量模板)
- [数据库初始化](#数据库初始化)
- [极速复现指南](#极速复现指南)
- [复现验收清单](#复现验收清单)
- [常见问题排查](#常见问题排查)
- [已知边界与安全要求](#已知边界与安全要求)
- [项目结构](#项目结构)
- [维护约定](#维护约定)

## 复现性声明

### 声明等级

本项目当前达到“**工程可复现，外部能力条件复现**”：

- 代码构建、单元测试、后端启动、数据库建表、演示账号登录可以按本文步骤复现。
- 真实 AI、ASR、浏览器语音识别和手语数字人依赖外部网络、账号、席位及供应商状态，不保证每次输出一致。
- 本文不承诺在任意操作系统、任意依赖版本下完全一致；请优先使用下方已验证基线。

### 已验证基线

| 项目 | 本次验证值 |
| --- | --- |
| 验证日期 | 2026-10-30 |
| 操作系统 | Windows 11 x64 |
| 代码基线 | 当前工作区；父提交 `a013f17c0d5820b630d34bc0cb26fddf81ce730c` |
| JDK | OpenJDK 21.0.11（项目编译目标为 Java 17） |
| Maven | 3.9.16 |
| Node.js | 24.12.0 |
| npm | 11.6.2 |
| MySQL | 8.0.46 |
| LibreOffice | 26.8.1.1 |
| FFmpeg | `N-92722-gf22fcd4483` |
| FFprobe | `2023-02-13-git-2296078397` |
| 浏览器 | 建议最新版 Chrome 或 Edge |

### 本次已执行验证

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 后端单元测试 | `mvn -q test` | 通过：7 个测试套件、21 个测试、0 失败、0 错误 |
| 前端生产构建 | `npm run build` | 通过；存在单个 chunk 大于 500 kB 的非阻断警告 |
| 后端启动 | `mvn -q spring-boot:run` | 通过；监听 `0.0.0.0:8080` |
| 数据库连接与初始化 | 应用启动时执行 `schema.sql` | 通过 |
| 教师登录 | `POST /api/auth/login` | HTTP 200，返回 `TEACHER` 和 JWT |
| 学生登录 | `POST /api/auth/login` | HTTP 200 |

## 环境与依赖清单

### 必需软件

| 组件 | 最低要求 | 已验证版本 | 用途 |
| --- | --- | --- | --- |
| Git | 任意近期版本 | 2.54.0 | 获取代码 |
| JDK | 17+ | 21.0.11 | 后端编译与运行 |
| Maven | 3.9+ | 3.9.16 | 后端依赖与测试 |
| Node.js | 20+，推荐 22 LTS | 24.12.0 | 前端构建 |
| npm | 10+ | 11.6.2 | 前端依赖安装 |
| MySQL | 8.0.x | 8.0.46 | 业务数据库 |
| Chrome/Edge | 最新版 | 未固定 | 前端、麦克风和 Web Speech API |

### 按功能启用的依赖

| 组件 | 启用功能 | 说明 |
| --- | --- | --- |
| LibreOffice | PPTX 转 PDF、PPT 文本提取前置条件 | 不安装时课程上传流程不可用 |
| FFmpeg | 视频抽取 16 kHz 单声道音频 | `FFMPEG_PATH` 必须可执行 |
| FFprobe | 读取视频时长 | `FFPROBE_PATH` 必须可执行 |
| SiliconFlow API Key | 真实 LLM 与 ASR | 无 Key 时可使用 `AI_MODE=mock` 演示 |
| 手语数字人账号 | 在线数字人 | 通过 `VITE_SIGN_AVATAR_ACCOUNT` 配置；缺失时不阻断主流程 |
| 外网访问 | AI、ASR、数字人、依赖下载 | 需要访问 Maven/npm 仓库及供应商域名 |

建议版本安装完成后执行：

```powershell
java -version
mvn -version
node --version
npm --version
mysql --version
```

若 LibreOffice、FFmpeg 或 FFprobe 不在 `PATH` 中，请继续在环境变量模板里填写绝对路径。

### 依赖锁定

- 后端依赖版本由 `backend/pom.xml` 和 Spring Boot 3.3.5 BOM 管理。
- 前端依赖由 `frontend/package-lock.json` 锁定；请使用 `npm ci`，不要使用无锁安装。
- 仓库不包含 `node_modules`、Maven 缓存、LibreOffice、FFmpeg 等本地运行工具。
- 仓库中的 `tools/` 是本机运行时和调试资料目录，已被 Git 忽略，不属于可克隆依赖。

### 端口与网络

| 地址/端口 | 用途 |
| --- | --- |
| `localhost:3306` | MySQL 默认端口 |
| `localhost:8080` | Spring Boot REST、WebSocket、文件访问 |
| `localhost:5173` | Vite 开发服务器 |
| `https://api.siliconflow.cn` | SiliconFlow LLM/ASR |
| `https://avatar.gbqr.net` | 手语数字人 SDK |
| `https://api.gbqr.net` | 手语数字人接口，具体域名以 SDK 请求为准 |

浏览器麦克风和手语数字人依赖安全上下文，请使用 `http://localhost:5173` 或 HTTPS。不要通过局域网 HTTP 地址测试教师麦克风。

## 环境变量模板

项目不是通过根目录 `.env` 启动，而是分别读取后端 Java Properties 和前端 Vite 环境变量。不要提交真实密钥。

### 后端：`backend/config/local.properties`

先复制模板：

```powershell
Copy-Item backend/config/local.properties.example backend/config/local.properties
```

完整模板如下：

```properties
# ===== 数据库 =====
DB_URL=jdbc:mysql://localhost:3306/hearing_platform?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=root
DB_PASSWORD=change-me

# ===== 鉴权 =====
# 至少 32 字节，生产环境必须替换为随机值
JWT_SECRET=replace-with-at-least-32-random-characters
JWT_EXPIRATION_HOURS=8

# ===== AI 与 ASR =====
# auto：优先真实接口，失败后降级
# real：只使用真实接口，失败即报错
# mock：不访问远程 AI，直接使用明确标记的模拟内容
AI_MODE=auto
AI_FALLBACK_ENABLED=true
SILICONFLOW_API_KEY=
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
SILICONFLOW_LLM_MODEL=Qwen/Qwen2.5-72B-Instruct
SILICONFLOW_ASR_MODEL=FunAudioLLM/SenseVoiceSmall

# ===== 本地工具 =====
# Windows 默认安装示例；使用便携版时改为实际绝对路径
LIBREOFFICE_PATH=C:/Program Files/LibreOffice/program/soffice.exe
FFMPEG_PATH=ffmpeg
FFPROBE_PATH=ffprobe

# ===== 运行目录与端口 =====
# 相对路径以启动后端时的工作目录为基准
UPLOAD_ROOT=./uploads
LOG_PATH=./logs
SERVER_PORT=8080
```

说明：

- 最快演示可将 `AI_MODE` 改为 `mock`，此时不要求配置 `SILICONFLOW_API_KEY`。
- `auto` 或 `real` 模式需要有效 Key、网络和供应商额度。
- `JWT_SECRET` 少于 32 字节时后端会拒绝启动。
- 若使用 Linux/macOS，`LIBREOFFICE_PATH` 可设为 `soffice`，前提是该命令在 `PATH` 中。
- 环境变量与 `local.properties` 同名时，进程环境变量优先。

### 前端：`frontend/.env.local`

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

```dotenv
# 手语数字人 SDK 账号；留空时数字人功能降级，但训练/课堂主流程仍可运行
VITE_SIGN_AVATAR_ACCOUNT=your_sdk_account
```

注意：

- `VITE_*` 在 Vite 启动或构建时注入，修改后必须重启开发服务器或重新构建。
- 前端 API、文件和 WebSocket 代理目标当前固定为 `http://localhost:8080` / `ws://localhost:8080`，见 `frontend/vite.config.js`。若修改后端端口，需同步修改代理配置。
- 生产部署时应通过同源反向代理提供 `/api`、`/files` 和 `/ws`，并配置 HTTPS。

### 变量一览

| 变量 | 必需 | 默认值 | 作用 |
| --- | --- | --- | --- |
| `DB_URL` | 否 | 本机 `hearing_platform` JDBC URL | 数据库连接 |
| `DB_USERNAME` | 是 | `root` | 数据库用户名 |
| `DB_PASSWORD` | 是 | 空 | 数据库密码 |
| `JWT_SECRET` | 是 | 仅演示用固定值 | JWT 签名密钥，至少 32 字节 |
| `JWT_EXPIRATION_HOURS` | 否 | `8` | Token 有效期 |
| `AI_MODE` | 否 | `auto` | `auto` / `real` / `mock` |
| `AI_FALLBACK_ENABLED` | 否 | `true` | 真实 AI 失败时是否降级 |
| `SILICONFLOW_API_KEY` | 条件 | 空 | 真实 LLM/ASR 凭据 |
| `SILICONFLOW_BASE_URL` | 否 | SiliconFlow 官方地址 | API 基础地址 |
| `SILICONFLOW_LLM_MODEL` | 否 | `Qwen/Qwen2.5-72B-Instruct` | 文本模型 |
| `SILICONFLOW_ASR_MODEL` | 否 | `FunAudioLLM/SenseVoiceSmall` | 语音转写模型 |
| `LIBREOFFICE_PATH` | PPT 功能必需 | Windows 安装路径 | PPTX 转 PDF |
| `FFMPEG_PATH` | 视频功能必需 | `ffmpeg` | 音频抽取 |
| `FFPROBE_PATH` | 视频功能必需 | `ffprobe` | 媒体时长读取 |
| `UPLOAD_ROOT` | 否 | `./uploads` | 上传文件根目录 |
| `LOG_PATH` | 否 | `./logs` | 日志目录 |
| `SERVER_PORT` | 否 | `8080` | 后端端口 |
| `VITE_SIGN_AVATAR_ACCOUNT` | 数字人功能必需 | 空 | 数字人 SDK 账号 |

## 数据库初始化

### 自动初始化

默认 JDBC URL 包含 `createDatabaseIfNotExist=true`，且 Spring 配置为：

```yaml
spring:
  sql:
    init:
      mode: always
```

因此数据库账号具备建库权限时，首次启动会自动创建 `hearing_platform` 并执行 `backend/src/main/resources/schema.sql`。

### 手动初始化

若数据库账号无建库权限，先由管理员执行：

```sql
CREATE DATABASE IF NOT EXISTS hearing_platform
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

随后启动后端，`schema.sql` 仍会自动建表。也可手动执行：

```sql
USE hearing_platform;
SOURCE D:/Hearing-impairedLearningPlatform/backend/src/main/resources/schema.sql;
```

注意将路径替换为实际绝对路径，MySQL `SOURCE` 使用正斜杠更稳妥。

### 演示账号

后端启动后，`DataInitializer` 会幂等写入以下账号：

| 角色 | 用户名 | 密码 | 显示名称 |
| --- | --- | --- | --- |
| 教师 | `teacher` | `123456` | 演示教师 |
| 学生 | `student` | `123456` | 演示学生 |

账号仅在不存在时创建，不会在每次启动时重置密码。演示账号只允许用于本地开发。

### 重置本地数据库

> 警告：以下命令会删除全部本地业务数据，仅用于确认可重复初始化。

```sql
DROP DATABASE IF EXISTS hearing_platform;
CREATE DATABASE hearing_platform
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

然后重启后端，表结构和演示账号会重新生成。

## 极速复现指南

以下步骤适用于已经安装“必需软件”的 Windows 环境。首次下载 Maven/npm 依赖时需要外网访问。

### 1. 获取代码并进入目录

```powershell
git clone <repository-url>
cd Hearing-impairedLearningPlatform
```

### 2. 创建本地配置

```powershell
Copy-Item backend/config/local.properties.example backend/config/local.properties
Copy-Item frontend/.env.example frontend/.env.local
```

按“环境变量模板”填写：

- 至少配置 `DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`。
- 无 SiliconFlow Key 时设置 `AI_MODE=mock`。
- 安装 LibreOffice 和 FFmpeg 后填写实际路径，或确保命令在 `PATH` 中。
- 数字人账号可暂不填写，不影响登录、课堂和视频基础流程。

### 3. 启动 MySQL

确保 MySQL 8.0 正在监听 `3306`，并使用配置中的账号密码可以连接。

```powershell
Get-NetTCPConnection -LocalPort 3306 -State Listen
```

### 4. 启动后端

打开终端 A：

```powershell
cd backend
mvn spring-boot:run
```

成功标志：

```text
Tomcat started on port 8080
Started HearingPlatformApplication
```

### 5. 启动前端

打开终端 B：

```powershell
cd frontend
npm ci
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

### 6. 登录

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 教师 | `teacher` | `123456` |
| 学生 | `student` | `123456` |

### 7. 停止服务

在后端和前端终端分别按 `Ctrl+C`。

## 复现验收清单

### 构建与启动

- [ ] `java -version` 显示 JDK 17 或更高版本。
- [ ] `mvn -version` 显示 Maven 3.9 或更高版本。
- [ ] `node --version` 显示 Node.js 20 或更高版本。
- [ ] MySQL 8.0 正在监听 `3306`。
- [ ] `cd backend && mvn -q test` 成功。
- [ ] `cd frontend && npm ci && npm run build` 成功。
- [ ] 后端启动后监听 `8080`。
- [ ] 前端启动后监听 `5173`。
- [ ] `hearing_platform` 数据库中存在业务表和演示账号。

### API 冒烟测试

PowerShell：

```powershell
$body = @{ username = 'teacher'; password = '123456' } | ConvertTo-Json
$response = Invoke-RestMethod `
  -Method Post `
  -Uri 'http://localhost:8080/api/auth/login' `
  -ContentType 'application/json' `
  -Body $body
$response.code
$response.data.user.role
```

预期输出：

```text
200
TEACHER
```

curl：

```bash
curl -sS http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student","password":"123456"}'
```

预期返回 `code: 200`、`role: STUDENT` 和 JWT。

### 核心功能人工验收

- [x] 教师登录后可进入课程资料库。
- [x] 教师上传 `.pptx` 后生成 PDF，并可预览和翻页。
- [x] AI 为 `mock` 时，备课资料显示模拟来源标记，课堂主流程不中断。
- [x] 教师创建课堂后获得 6 位房间码。
- [x] 学生输入房间码后可加入并查看同步 PPT 与字幕。
- [x] 教师发送演示字幕或使用浏览器语音识别后，学生端同步显示。
- [x] 教师和学生可发送弹幕，历史记录可回看。
- [x] 学生上传 MP4/WebM 后，在 FFmpeg、ASR 或 mock 降级路径下可查看字幕/笔记。
- [x] 社区可发帖、搜索、筛选、评论，并仅允许作者删除自己的内容。
- [x] 课程问答返回答案、来源标记和引用片段。
- [x] 页面刷新后登录态仍存在；Token 失效时返回登录页。
- [x] 移动端无横向溢出，字幕字号、高对比度和减少动画设置生效。

### 外部服务专项验收

- [x] 使用有效 `SILICONFLOW_API_KEY` 且 `AI_MODE=real` 时，备课/问答/转写接口成功。
- [x] 使用错误 Key 且 `AI_MODE=auto` 时，返回明确标记的降级内容。
- [x] 配置 `VITE_SIGN_AVATAR_ACCOUNT` 后，数字人初始化、播报、重播、隐藏和恢复可用。
- [x] 微信/Edge/Chrome 麦克风授权仅在 `localhost` 或 HTTPS 下测试。
- [x] 多学生并发数字人测试时确认账号席位上限。

## 常见问题排查

| 现象 | 可能原因 | 处理方式 |
| --- | --- | --- |
| `Communications link failure` | MySQL 未启动、端口不对、防火墙拦截 | 启动 MySQL，确认 `3306` 和 `DB_URL` |
| `Access denied for user` | 数据库用户名/密码错误 | 修正 `DB_USERNAME`、`DB_PASSWORD` |
| 建库失败 | 账号无 `CREATE DATABASE` 权限 | 由管理员预建数据库，或授予权限 |
| 表不存在 | `schema.sql` 未执行或初始化失败 | 检查启动日志和 `spring.sql.init.mode` |
| `JWT_SECRET must contain at least 32 bytes` | JWT 密钥过短 | 替换为至少 32 字节的随机字符串 |
| PPTX 上传失败 | LibreOffice 路径错误或不可执行 | 执行 `soffice --version`，填写绝对路径 |
| `ffmpeg`/`ffprobe` 找不到 | 未安装或不在 `PATH` | 填写 `FFMPEG_PATH`、`FFPROBE_PATH` 绝对路径 |
| 麦克风无法授权 | 使用了局域网 HTTP 或浏览器不支持 | 使用 `http://localhost:5173` 或 HTTPS，优先 Chrome/Edge |
| WebSocket 无法连接 | 后端未启动、代理配置错误、端口不一致 | 确认后端 `8080`，重启 Vite，检查浏览器网络面板 |
| AI 接口返回 503 | Key 缺失、余额不足、网络失败 | 检查 Key 和网络；演示时使用 `AI_MODE=mock` |
| AI 输出每次不同 | 真实 LLM 非确定性 | 验收结构、引用和业务行为，不比对逐字文本 |
| 数字人初始化失败 | 账号缺失、非安全上下文、外网或席位问题 | 检查 `VITE_SIGN_AVATAR_ACCOUNT`，使用 localhost/HTTPS，重试 |
| `npm ci` 失败 | Node 版本不符、代理或缓存损坏 | 使用 Node 20+，检查 npm registry，清理缓存后重试 |
| 端口被占用 | `8080` 或 `5173` 已有进程 | 停止占用进程，或同步修改后端与 Vite 代理端口 |
| 前端构建提示 chunk 过大 | PDF.js/Element Plus 体积较大 | 当前为非阻断警告；可后续做代码分割 |

## 已知边界与安全要求

### 可复现边界

- 真实 AI、ASR 和数字人的可用性、延迟、文本结果与供应商账号状态相关。
- 浏览器 Web Speech API 的识别结果、标点和延迟由浏览器和网络决定。
- 视频 VTT 时间轴按文本长度和时长估算，不是逐词对齐。
- 手语数字人动画速度由在线 SDK 决定，不保证与字幕完全同步。
- 前端生产构建存在大 chunk 警告，不影响当前构建成功。

### 安全要求

- `backend/config/local.properties`、`frontend/.env.local` 和真实密钥不得提交到 Git。
- 默认演示密码 `123456` 仅用于本地测试；公开部署前必须替换。
- 生产环境必须设置强随机 `JWT_SECRET` 并启用 HTTPS。
- `/files/**` 当前没有业务鉴权；敏感课程、视频和录音应增加签名 URL、登录校验或私有对象存储。
- CORS 当前只放行本地和少量内网来源；部署到新域名时需修改 `WebConfig`。
- 录音、字幕、弹幕和视频上传前应获得参与者知情同意。
- 日志不得记录密码、JWT、API Key、字幕正文和弹幕正文。
- 当前不支持注册、找回密码、账号管理后台和跨教师协作，不应按多租户生产系统部署。

## 项目结构

```text
Hearing-impairedLearningPlatform/
├─ backend/                    Spring Boot 后端
│  ├─ config/
│  │  └─ local.properties.example
│  ├─ src/main/java/           Controller、Service、DAO、WebSocket、AI Client
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  ├─ schema.sql
│  │  └─ logback-spring.xml
│  └─ pom.xml
├─ frontend/                   Vue 3 + Vite 前端
│  ├─ src/
│  ├─ .env.example
│  ├─ package.json
│  └─ package-lock.json
├─ docs/                       功能设计与实现记录
├─ tools/                      本机工具和调试资料，Git 忽略
├─ uploads/                    运行时上传目录，Git 忽略
├─ logs/                       运行时日志目录，Git 忽略
└─ README.md
```

## 维护约定

每次修改功能、接口、架构、依赖或配置时，至少同步完成：

1. 更新本 README 的依赖、变量、启动或验收步骤。
2. 运行 `mvn -q test` 和 `npm run build`。
3. 使用演示教师和学生账号完成登录与受影响流程冒烟测试。
4. 确认新增配置有模板但真实值未提交。
5. 确认前端、后端端口和 Vite 代理配置一致。
6. 确认 README 中的“已验证基线”仍与当前代码状态一致。

提交前检查：

```powershell
git status --short
git diff --check
```

若复现基线发生变化，应更新验证日期、提交号、依赖版本和验证结果，不能继续沿用旧声明。
