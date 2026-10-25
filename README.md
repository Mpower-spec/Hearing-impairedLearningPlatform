# 声学无碍学习平台

面向听障学生的课堂学习平台 Demo。教师上传 PPTX 后创建实时课堂，学生通过房间码加入；课堂中的 PPT 页码、教师语音字幕、弹幕和课堂状态通过 WebSocket 实时同步。课后学生可查看课堂资料、生成课程问答、上传视频生成字幕与笔记，并通过全局手语数字人获得辅助表达。

平台同时提供教师与学生共同参与的文字学习社区、移动端学生界面，以及字幕字号、行距、高对比度、减少动画和主题切换等无障碍阅读设置。

> 当前定位是比赛与演示用 MVP，不是可直接暴露到公网的正式生产系统。部署前请先阅读“安全与部署限制”。

## 项目价值

- **字幕优先**：教师语音转成最终字幕，学生在课堂中同步阅读，降低对声音信息的依赖。
- **多通道同步**：PPT 页码、字幕、弹幕、课堂结束状态都在同一房间内实时广播。
- **课后可复习**：保留课堂字幕、弹幕和学习资料，学生可重新进入已结束课堂回看。
- **学习闭环**：课堂问答、学习资料、视频字幕、视频笔记和手语数字人形成完整学习辅助链路。
- **无障碍可配置**：字幕字号和行距可调，支持高对比度、深色主题和减少动画。

## 主要功能

### 教师端

- 上传 `.pptx`，校验扩展名与大小，通过 LibreOffice 转为 PDF。
- 使用 Apache POI 提取 PPT 文本，通过 PDFBox 计算页数。
- 查看课程列表、PDF 预览、备课资料和课堂历史。
- 创建课堂并生成唯一 6 位房间码。
- 实时切换 PPT 页码，向学生广播同步状态。
- 使用 Chrome/Edge Web Speech API 生成教师语音字幕。
- 无麦克风或不支持语音识别时，使用内置演示字幕脚本。
- 查看学生弹幕、结束课堂。
- 生成课堂学习资料，供学生课后查看。
- 删除自己的课程及其关联内容。

### 学生端

- 输入 6 位房间码加入进行中的课堂。
- 在“我加入的课堂”中查看进行中和已结束课堂：
  - 进行中课堂可直接继续进入，无需再次输入邀请码。
  - 已结束课堂可进入回看字幕、弹幕和课后资料。
- 同步查看 PPT、实时字幕、弹幕和课堂状态。
- 结束课堂后自行生成或查看本节课学习资料。
- 上传 MP4/WebM，提取音频并调用 SiliconFlow ASR 生成字幕。
- 播放带 VTT 字幕的视频，并根据 VTT 时间轴驱动手语数字人。
- 根据转写文本生成结构化学习笔记。
- 在实时课堂中使用课程 AI 问答，回答附带资料来源。
- 删除自己的视频及其关联文件。
- 手机浏览器使用首页、课堂、视频、社区底部导航。

### 学习社区

- 教师和学生均可发布文字帖子。
- 支持分类筛选、关键词搜索和分页。
- 支持评论与删除自己发布的内容。
- 社区数据持久化到 MySQL。

### 无障碍与数字人

- 字幕字号：28–64 px。
- 字幕行距：1.2–2.0 倍。
- 高对比度模式、明亮/深色主题。
- 减少动画：停止背景光效、声波、悬浮和主要过渡动画。
- 阅读设置保存在浏览器 `localStorage`，刷新或重新登录后自动恢复。
- 学生端使用唯一全局手语数字人实例：切换页面时不重复创建。
- 实时课堂最终字幕和视频 VTT 字幕可驱动数字人表达手语。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 17 语言级别、Spring Boot 3.3.5、Spring WebSocket、MyBatis-Plus 3.5.9 |
| 鉴权 | JWT、BCrypt |
| 数据库 | MySQL 8、`schema.sql` 自动初始化 |
| 前端 | Vue 3、Vite 6、JavaScript、Element Plus、Axios、Vue Router |
| PDF 预览 | `vue-pdf-embed` |
| Markdown | `marked` + `DOMPurify` |
| PPT 转换 | LibreOffice |
| 文本提取与页数 | Apache POI、PDFBox |
| 视频处理 | FFmpeg、FFprobe |
| ASR | SiliconFlow `FunAudioLLM/SenseVoiceSmall` |
| LLM | SiliconFlow `Qwen/Qwen2.5-72B-Instruct` |
| 手语数字人 | `avatar.gbqr.net` 在线 SDK、浏览器 WebGL |
| 日志 | SLF4J + Logback |

## 系统架构

```text
浏览器
├─ Vue 3 前端
│  ├─ REST API ───────────────┐
│  ├─ 原生 WebSocket ───────┐ │
│  ├─ PDF.js 预览            │ │
│  └─ 手语数字人 SDK         │ │
│                            │ │
└─ Node/Vite 开发服务器      │ │
                             ▼ ▼
Spring Boot 后端
├─ Controller / WebSocket Handler
├─ Service
├─ DAO（MyBatis-Plus Mapper）
├─ Entity / DTO
├─ JWT 鉴权与角色校验
├─ SiliconFlow LLM / ASR Client
├─ LibreOffice / FFmpeg 调用
└─ 本地文件存储 uploads/
                             │
                             ▼
                          MySQL 8
```

后端采用 SSM 风格分层，依赖方向为：

```text
controller/websocket -> service -> dao -> entity
```

Controller 不直接访问 Mapper，DAO 不依赖业务层。

## 目录结构

```text
Hearing-impairedLearningPlatform/
├─ backend/
│  ├─ config/                    本地配置模板
│  ├─ logs/                      运行时日志（不提交）
│  ├─ src/main/java/.../
│  │  ├─ controller/             REST 接口
│  │  ├─ service/                业务逻辑
│  │  ├─ dao/                    MyBatis-Plus Mapper
│  │  ├─ entity/                 数据库实体
│  │  ├─ dto/                    请求与响应模型
│  │  ├─ security/               JWT、鉴权与角色控制
│  │  ├─ websocket/              课堂实时消息
│  │  ├─ client/                 SiliconFlow 客户端
│  │  ├─ config/                 Spring 配置与初始化
│  │  ├─ logging/                请求日志
│  │  └─ common/                 统一响应与异常处理
│  ├─ src/main/resources/
│  │  ├─ application.yml
│  │  ├─ schema.sql
│  │  └─ logback-spring.xml
│  └─ src/test/                  后端测试
├─ frontend/
│  ├─ src/api/                   Axios 封装
│  ├─ src/auth/                  登录会话
│  ├─ src/avatar/                全局数字人控制
│  ├─ src/accessibility/         阅读设置持久化
│  ├─ src/components/            公共组件
│  ├─ src/views/                 页面
│  ├─ src/styles/                全局样式
│  └─ .env.example               数字人账号配置模板
├─ tools/                        本地媒体工具（被 .gitignore 忽略）
├─ uploads/                      运行时上传文件（被 .gitignore 忽略）
└─ README.md
```

## 快速开始

### 1. 前置条件

- JDK 17 或更高版本，推荐 JDK 21。
- Maven 3.9。
- Node.js 18 或更高版本。
- MySQL 8，并确保本机可以连接。
- Chrome 或 Edge。教师实时语音字幕需要 Web Speech API。
- LibreOffice、FFmpeg 和 FFprobe。

Windows 演示环境可以使用本地 `tools/` 目录：

```text
tools/libreoffice/program/soffice.com
tools/node_modules/@ffmpeg-installer/win32-x64/ffmpeg.exe
tools/node_modules/@ffmpeg-installer/win32-x64/ffprobe.exe
```

`tools/` 被 `.gitignore` 忽略。新机器需自行安装媒体工具，或在配置中填写实际可执行文件路径。

### 2. 准备数据库

默认连接地址：

```text
jdbc:mysql://localhost:3306/hearing_platform
```

默认连接 URL 包含 `createDatabaseIfNotExist=true`，MySQL 账号有权限时会自动创建数据库。服务启动后执行 `schema.sql`，自动创建业务表。

如需手动创建：

```sql
CREATE DATABASE hearing_platform
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 3. 配置后端

复制配置模板：

```powershell
Copy-Item backend/config/local.properties.example backend/config/local.properties
```

至少填写：

```properties
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
SILICONFLOW_API_KEY=sk-你的硅基流动Key
```

`backend/config/local.properties` 已被 `.gitignore` 忽略，禁止提交真实密码和 API Key。

### 4. 配置数字人

复制前端环境模板：

```powershell
Copy-Item frontend/.env.example frontend/.env.local
```

填写：

```properties
VITE_SIGN_AVATAR_ACCOUNT=你的数字人SDK账号
```

`frontend/.env.local` 已被忽略，禁止提交真实账号。

### 5. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

### 6. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开：

```text
http://localhost:5173
```

Vite 已配置代理到 `http://localhost:8080`。如果后端未启动，前端页面和 API 会出现 `ECONNREFUSED` 或代理错误。

### 7. 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 教师 | `teacher` | `123456` |
| 学生 | `student` | `123456` |

演示账号由后端启动时自动初始化。当前不支持注册、找回密码和账号管理。

## 配置项

### 后端配置

配置可以写入 `backend/config/local.properties`，也可以通过环境变量覆盖。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/hearing_platform?...` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `SERVER_PORT` | `8080` | 后端监听端口 |
| `JWT_SECRET` | Demo 默认值 | JWT 秘钥，正式环境必须替换 |
| `JWT_EXPIRATION_HOURS` | `8` | JWT 有效期 |
| `UPLOAD_ROOT` | `./uploads` | 上传文件根目录 |
| `LIBREOFFICE_PATH` | `soffice.exe` 默认路径 | LibreOffice 可执行文件 |
| `FFMPEG_PATH` | `ffmpeg` | FFmpeg 可执行文件 |
| `FFPROBE_PATH` | `ffprobe` | FFprobe 可执行文件 |
| `AI_MODE` | `auto` | `real`、`auto`、`mock` |
| `AI_FALLBACK_ENABLED` | `true` | `auto` 模式下是否启用模拟降级 |
| `SILICONFLOW_BASE_URL` | `https://api.siliconflow.cn/v1` | SiliconFlow API 地址 |
| `SILICONFLOW_API_KEY` | 空 | SiliconFlow API Key |
| `SILICONFLOW_LLM_MODEL` | `Qwen/Qwen2.5-72B-Instruct` | LLM 模型 |
| `SILICONFLOW_ASR_MODEL` | `FunAudioLLM/SenseVoiceSmall` | ASR 模型 |
| `LOG_PATH` | `./logs` | 日志目录 |

### AI 运行模式

| 模式 | 行为 |
| --- | --- |
| `real` | 只调用真实 SiliconFlow 接口，失败时直接报错 |
| `auto` | 优先调用真实接口；失败且 `AI_FALLBACK_ENABLED=true` 时返回明确标记的模拟结果 |
| `mock` | 不调用远程 AI，直接返回确定性的演示内容 |

模拟结果会通过 `source=MOCK` 或界面“模拟结果”标识展示，不会伪装成真实 AI 输出。

### 前端配置

| 变量 | 说明 |
| --- | --- |
| `VITE_SIGN_AVATAR_ACCOUNT` | 手语数字人 SDK 登录账号 |

其他无障碍设置保存在浏览器 `localStorage` 的 `hp_accessibility_settings_v1` 键中，不写入数据库。

## 数据库表

| 表 | 用途 |
| --- | --- |
| `app_user` | 用户、角色与 BCrypt 密码哈希 |
| `lesson` | 课程、PPTX/PDF 路径、PPT 文本和页数 |
| `room` | 一次实时课堂、6 位房间码、当前页和状态 |
| `room_member` | 学生与课堂的加入关系，用于课堂记录和权限 |
| `caption` | 教师最终字幕 |
| `danmaku` | 课堂弹幕 |
| `material` | 备课资料与课堂学习资料 |
| `video` | 学生视频、音频、VTT、转写和笔记 |
| `community_post` | 学习社区帖子 |
| `community_comment` | 学习社区评论 |

服务启动时会执行 `backend/src/main/resources/schema.sql`。当前没有迁移工具，正式部署前建议引入 Flyway 或 Liquibase。

## REST API

所有 `/api/**` REST 接口默认返回统一结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

鉴权接口使用：

```http
Authorization: Bearer <JWT>
```

### 鉴权

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/auth/login` | 用户名密码登录 |
| `GET` | `/api/auth/me` | 获取当前用户 |

### 课程与文件

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/lessons` | 教师课程列表 |
| `POST` | `/api/lessons` | 教师上传 PPTX 并创建课程 |
| `GET` | `/api/lessons/{id}` | 课程详情 |
| `DELETE` | `/api/lessons/{id}` | 教师删除自己的课程 |
| `GET` | `/files/**` | 访问 PDF、视频、音频等上传文件 |
| `GET` | `/files/vtt/{filename}` | 以 `text/vtt` 返回字幕文件 |

`/api/lessons` 整个控制器仅教师可访问；学生通过课堂记录、课堂资料和视频接口访问自己的学习内容。

### 课堂与实时记录

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/rooms` | 教师创建课堂 |
| `GET` | `/api/rooms/mine` | 教师课堂列表 |
| `GET` | `/api/rooms/joined` | 学生加入过的课堂记录 |
| `POST` | `/api/rooms/join` | 学生通过房间码加入并写入成员关系 |
| `GET` | `/api/rooms/by-code/{code}` | 按房间码预览课堂 |
| `GET` | `/api/rooms/{id}` | 课堂详情与权限校验 |
| `POST` | `/api/rooms/{id}/end` | 教师结束课堂 |
| `GET` | `/api/rooms/{id}/captions` | 课堂字幕历史 |
| `GET` | `/api/rooms/{id}/danmaku` | 课堂弹幕历史 |

学生加入请求：

```json
{
  "roomCode": "131728"
}
```

课堂记录包含 `LIVE` 和 `ENDED` 状态。学生重新进入进行中课堂时无需重复输入邀请码；已结束课堂仅允许曾加入过的学生查看。

### AI 与学习资料

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `POST` | `/api/ai/prepare/{lessonId}` | 生成教师备课资料 |
| `POST` | `/api/ai/material/{lessonId}` | 生成课堂学习资料 |
| `POST` | `/api/ai/transcribe/{videoId}` | 视频转写并生成 VTT |
| `POST` | `/api/ai/note/{videoId}` | 生成视频学习笔记 |
| `GET` | `/api/materials/prepare/{lessonId}` | 教师备课资料 |
| `GET` | `/api/materials/class/{lessonId}` | 学生课堂学习资料 |
| `POST` | `/api/course-qa/ask` | 学生课程 AI 问答 |

课程问答示例：

```json
{
  "roomId": 27,
  "question": "这节课的重点是什么？"
}
```

问答上下文限定为当前课程的 PPT 文本、课堂字幕和学生可访问的课堂学习资料。回答会返回 `AI`、`MOCK` 或 `LOCAL` 来源标记，并附带 `[S1]` 等引用片段。

### 视频

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/videos` | 当前学生的视频列表 |
| `POST` | `/api/videos` | 上传 MP4/WebM |
| `GET` | `/api/videos/{id}` | 视频详情 |
| `DELETE` | `/api/videos/{id}` | 删除自己的视频 |

### 学习社区

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/api/community/posts` | 帖子列表、分类、搜索和分页 |
| `POST` | `/api/community/posts` | 发布帖子 |
| `GET` | `/api/community/posts/{id}` | 帖子详情 |
| `DELETE` | `/api/community/posts/{id}` | 删除自己的帖子 |
| `POST` | `/api/community/posts/{id}/comments` | 发表评论 |
| `DELETE` | `/api/community/comments/{id}` | 删除自己的评论 |

## WebSocket 实时课堂

连接地址：

```text
ws://localhost:8080/ws/classroom?token=<JWT>
```

生产环境使用 WSS：

```text
wss://<域名>/ws/classroom?token=<JWT>
```

服务端会校验 JWT、用户角色、房间状态和房间归属。学生发送翻页、字幕、结束课堂消息会被拒绝。

### 客户端消息

| `type` | 发送者 | 主要字段 | 说明 |
| --- | --- | --- | --- |
| `join` | 教师/学生 | `roomCode` | 加入课堂 |
| `slide` | 教师 | `page` | 切换 PPT 页码 |
| `caption` | 教师 | `text` | 持久化并广播最终字幕 |
| `danmaku` | 教师/学生 | `content` | 持久化并广播弹幕 |
| `end` | 教师 | 无 | 结束课堂 |
| `ping` | 教师/学生 | 无 | 心跳 |

### 服务端消息

| `type` | 主要字段 | 说明 |
| --- | --- | --- |
| `state` | 课程、房间、页码、页数、PDF、状态 | 加入成功后的完整课堂状态 |
| `slide` | `page` | 同步翻页 |
| `caption` | `id`、`text`、`createdAt` | 新最终字幕 |
| `danmaku` | `id`、`content`、`userName`、`createdAt` | 新弹幕 |
| `end` | 无 | 课堂结束 |
| `pong` | 无 | 心跳响应 |
| `error` | `message` | 消息处理失败 |

中间语音识别结果只在教师页面显示，不广播、不驱动手语；只有最终字幕会持久化并触发学生端手语。

## AI 与媒体处理流程

### PPTX 转 PDF

```text
上传 .pptx
  -> 校验扩展名和大小
  -> UUID 文件名保存
  -> LibreOffice 转 PDF
  -> POI 提取 PPT 文本
  -> PDFBox 计算页数
  -> 保存课程记录
```

转换失败时会清理失败产生的临时文件。

### 视频字幕

```text
上传 MP4/WebM
  -> FFmpeg 提取 16kHz 单声道音频
  -> SiliconFlow FunAudioLLM/SenseVoiceSmall 转写
  -> 按句子长度和视频时长估算时间轴
  -> 生成 VTT
  -> HTML5 video 加载字幕
  -> 根据 currentTime 驱动手语数字人
```

SiliconFlow ASR 不返回时间戳，因此 VTT 时间轴是**估算值**，不保证逐词或逐帧同步。

### AI 学习资料

- 教师备课资料：教学目标、重点、术语、练习。
- 课堂学习资料：课堂摘要、重点、术语和测验。
- 视频笔记：根据转写文本生成结构化笔记。
- 课程问答：关键词检索 PPT、字幕和课堂资料后调用 LLM。

课程问答当前不使用 Embedding、向量数据库或 Elasticsearch，适合 Demo 和小规模课程内容。

## 手语数字人

- SDK：`https://avatar.gbqr.net/yiyu.js`
- 学生端每个浏览器只挂载一个全局 `SignAvatarWidget`。
- 教师端不初始化数字人，只负责语音识别和字幕广播。
- 学生课堂收到最终 `caption` 后，通过全局 `avatarStore` 交给数字人。
- 视频页根据 VTT 字幕时间轴驱动同一个数字人。
- 数字人支持重播、隐藏/显示和拖动。
- 队列最多保留“当前一句 + 最新待播一句”，避免内容无限积压。
- 数字人不可用时，课堂、视频、字幕和问答主流程仍可使用。

当前只有一个 SDK 账号时，只保证一个学生演示设备同时加载数字人。多个学生同时使用时，需要额外 SDK 席位或厂商授权。

## 无障碍阅读设置

登录后点击页头“阅读设置”：

- 字幕字号（28–64 px）。
- 字幕行距（1.2–2.0 倍）。
- 高对比度模式。
- 减少动画。
- 明亮/深色主题。

核心 CSS 变量：

```text
--caption-font-size
--caption-line-height
--caption-history-font-size
--caption-card-bg
--caption-card-text
--reading-card-bg
--reading-card-border
--reading-card-text
```

设置仅在当前浏览器生效，不写入数据库。

## 界面视觉系统

前端采用“声音极光”视觉主题，强调字幕、声波和可访问性，同时保持内容优先：

- 全局夜空动态背景：银河光带、月亮辉光、闪烁星点、多层流星、极光、细网格、漂浮粒子和底部声波全部使用 CSS 绘制，不依赖额外图片。
- 深色玻璃页头：品牌、主导航、工具区三段式布局，青绿与琥珀色动态渐变线。
- 玻璃拟态卡片：统一圆角、内高光、柔和阴影和悬停抬升效果。
- 首页取消轮播，改为“主学习台 + 功能侧卡 + 快捷入口”的仪表盘式布局。
- 教师端三个主页面采用不同结构：课程资料库使用课程卡片架，实时课堂使用控制台式会话列表，备课中心使用 AI 工作流侧栏。
- 页面进入、卡片滚动出现、声波、光斑和悬浮元素均有轻量动画。
- 课堂页使用深色 PPT 舞台、字幕聚焦卡和半透明控制栏，保证页面层次清楚。
- 学生加入页、视频页、社区页和教师课程页统一为同一套视觉语言。
- 所有装饰元素使用 `aria-hidden`，不进入屏幕阅读器和键盘焦点顺序。
- 开启“减少动画”后，流星、背景动画、页面过渡、悬浮和滚动出现动画会自动减弱。
- 高对比度和深色主题仍优先覆盖装饰性颜色，保证字幕、文字和控件可读性。

视觉升级只调整模板装饰、CSS 和公共组件，不修改登录、课堂、视频、社区、AI 问答和手语数字人的业务逻辑。
## 推荐演示流程

### 教师

1. 使用 `teacher / 123456` 登录。
2. 进入课程列表，上传 `.pptx`。
3. 等待 LibreOffice 转换 PDF，确认 PDF 预览和页数正确。
4. 进入备课中心，生成备课资料。
5. 点击“开课”，获得 6 位房间码。
6. 进入教师课堂，打开语音字幕或使用“发送演示字幕”。
7. 切换 PPT 页码，让学生端同步。
8. 查看学生弹幕，结束课堂。
9. 生成课堂学习资料。

### 学生

1. 使用 `student / 123456` 登录。
2. 输入教师提供的房间码，点击加入。
3. 查看同步 PPT、实时字幕和弹幕。
4. 返回首页后进入“加入课堂”，在“我加入的课堂”中直接继续课堂。
5. 课堂结束后重新进入该记录，查看字幕和课后资料。
6. 打开课程问答，基于 PPT、字幕和资料提问。
7. 进入视频学习，上传 MP4/WebM。
8. 生成字幕和 VTT，播放视频并查看手语数字人。
9. 生成视频笔记。
10. 打开阅读设置，调整字幕字号、行距、高对比度和主题。

## 移动端适配

学生端主要断点：

| 屏幕宽度 | 布局 |
| --- | --- |
| `≤767px` | 手机：单列布局、底部导航、全宽表单 |
| `768–1119px` | 平板：单列或双列自适应 |
| `≥1120px` | 桌面：完整多栏布局 |

学生底部导航：

- 首页
- 加入课堂
- 视频学习
- 学习社区

手机课堂采用字幕、PPT、控制栏依次排列的竖屏布局，不强制横屏。底部导航使用 `env(safe-area-inset-bottom)` 适配手势安全区，移动端输入字号不低于 16px。

## 局域网访问

前后端默认监听所有网卡：

```text
前端：http://<本机局域网IPv4>:5173
后端：http://<本机局域网IPv4>:8080
```

查看本机 IPv4：

```powershell
ipconfig
```

注意事项：

- 同学设备必须与教师电脑处于同一局域网，或处于同一 VPN 虚拟网络。
- Windows 防火墙需放行 TCP `5173` 和 `8080`。
- 教师语音识别建议在教师电脑上使用 `http://localhost:5173`，因为麦克风通常要求安全上下文。
- 学生可通过局域网地址访问页面，但 Chrome/Edge 的语音识别和部分移动端能力可能受 HTTPS 限制。

## 日志

日志目录默认是 `backend/logs/`，可通过 `LOG_PATH` 修改。

主要文件：

```text
hearing-platform.log           INFO 及以上业务日志
hearing-platform-error.log     ERROR 日志和异常堆栈
hearing-platform-access.log    API 访问日志
```

日志包含请求方法、路径、状态码、耗时、用户 ID 和角色；不会记录密码、JWT、API Key、字幕正文或弹幕内容。

控制台同时输出日志，便于本地开发排查。

## 构建与测试

### 后端

```powershell
cd backend
mvn test
```

当前测试覆盖：

- JWT 生成与解析。
- 请求访问日志与 MDC 清理。
- VTT 时间轴生成。
- 课程问答关键词检索。
- 学生课堂记录查询。
- REST 加入课堂时持久化成员关系。
- Prompt 模板约束。

### 前端

```powershell
cd frontend
npm run build
```

构建产物输出到 `frontend/dist/`。PDF.js 和 Element Plus 会产生较大的异步 chunk，首次加载时属正常现象。

## 常见问题

### 前端启动报 `ERR_MODULE_NOT_FOUND`

不要直接用 Node 运行 `src/main.js` 或 `src/router/index.js`。使用：

```powershell
cd frontend
npm run dev
```

### Vite 代理报 `ECONNREFUSED`

确认后端已在 `8080` 启动：

```powershell
Invoke-WebRequest http://localhost:8080/api/auth/me
```

未登录时返回 `401` 是正常的；连接被拒绝才说明后端未启动。

### PPT 上传或转换失败

- 仅支持 `.pptx`。
- 确认 `LIBREOFFICE_PATH` 指向 `soffice.com`，不是 `soffice.exe`。
- 确认文件没有损坏。
- 查看 `backend/logs/hearing-platform-error.log`。
- 转换服务使用独立 LibreOffice 用户目录，避免进程锁。

### 视频处理失败

- 仅支持浏览器可播放的 MP4/WebM。
- 确认 `FFMPEG_PATH` 和 `FFPROBE_PATH` 正确。
- 分别运行 `ffmpeg -version` 和 `ffprobe -version`。
- 检查 SiliconFlow Key、余额和网络。

### 登录失败或数据库连接失败

检查：

- MySQL 8 是否正在运行。
- `DB_USERNAME`、`DB_PASSWORD`、`DB_URL` 是否正确。
- `hearing_platform` 数据库是否存在。
- 数据库账号是否有建表和读写权限。

### AI 返回模拟结果

可能原因：

- 未配置 `SILICONFLOW_API_KEY`。
- API Key 错误、余额不足或网络不可达。
- `AI_MODE=mock`。
- `AI_MODE=auto` 且真实请求失败。

需要严格真实调用时设置：

```properties
AI_MODE=real
```

### 课程问答返回 500 或提示接口不存在

确认启动的是最新后端代码并已重启 Spring Boot。后端代码更新后，仅刷新前端不会加载新的 Controller。

### WebSocket 无法连接

- 检查 JWT 是否有效。
- 检查房间是否仍处于 `LIVE`。
- 检查前端代理是否支持 `/ws` 升级。
- 检查 HTTPS 页面是否使用 `wss://`。
- 查看后端 WebSocket 相关日志。

### 手语数字人一直显示“正在为你生成手语教学”

- 当前 SDK 账号通常只支持一个并发会话，只保留一个学生端标签页。
- 关闭旧课堂或视频页面，等待约 30 秒后点击“重试”。
- 确认浏览器可访问 `avatar.gbqr.net` 和 `api.gbqr.net`。
- 确认未拦截 HTTPS/WSS，且浏览器已启用 WebGL。
- 数字人失败不会阻断课堂、视频、字幕和问答功能。

## 安全与部署限制

本项目当前是本地 Demo，正式部署前至少需要完成：

- 为 `/files/**` 增加鉴权或改用带签名的临时 URL。
- 使用 HTTPS 和 WSS。
- 使用强随机 `JWT_SECRET`，不要使用 Demo 默认值。
- 将数据库密码、API Key 和 SDK 账号放入服务器密钥管理或环境变量。
- 配置严格的 CORS 白名单。
- 增加上传文件大小、类型、路径和恶意文件校验。
- 增加接口限流、登录失败限制和审计。
- 使用对象存储保存视频和大文件。
- 引入数据库迁移工具和备份策略。
- 为数字人 SDK 购买足够并发席位。
- 增加监控、告警、健康检查和集中日志。

当前明确不做：

- 用户注册、找回密码和账号管理后台。
- PPT 动画、内嵌视频和复杂版式还原。
- 视频转码、HLS 和对象存储。
- 微信小程序独立端。
- 多租户、负载均衡和高可用部署。
- 社区图片、附件、点赞、私信和内容审核后台。
- 跨课程 AI 问答和问答历史持久化。
- 手语词元库标注和自定义离线数字人模型。

## 维护约定

每次新增功能、修改接口、调整架构、改变配置或进行重要 UI/技术改动时，必须同步更新本 README。

提交前至少执行：

```powershell
git diff --check
cd backend
mvn test
cd ..\frontend
npm run build
```

禁止提交：

```text
backend/config/local.properties
backend/logs/
frontend/.env.local
uploads/
tools/
```
