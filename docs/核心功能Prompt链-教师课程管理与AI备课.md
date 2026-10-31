# 教师课程管理与 AI 备课 · 原生 Prompt 链


---

## Turn 1：我现在将要开发一个web应用，关于听障学生学习平台，我先给出以下几个核心功能，你告诉我怎么实现，以及实现的技术栈。调研背景:十五…

### 我的 Prompt

我现在将要开发一个web应用，关于听障学生学习平台，我先给出以下几个核心功能，你告诉我怎么实现，以及实现的技术栈。调研背景:十五五计划，全国特殊教育学校，特殊教育学校老师的教学经历

WebSocket多人房间Demo:雨课堂功能模板

MVP功能清单:

..分教师端和学生端
   1.教师端
    1.PPT div＋语音实时字幕（正在上课时使用）
    2.备课，根据ppt内容实现备课资料生成
   2.学生端
     3websocket接受教师端PPT和弹幕
    1.上传已有学习视频＋生成笔记实时字幕
    2.集成大模型根据一节课字幕生成学习资料

### codex 回复

建议先把 MVP 定义成一句话：

> 教师开课并同步 PPT，教师语音实时转字幕；学生用房间码进入看 PPT、发弹幕；课后字幕自动生成学习资料；学生上传视频也能生成同步字幕和笔记。

下面按“调研结论 → 技术栈 → 架构 → 功能实现 → 实施顺序”展开。

---

## 1. 调研背景如何落到产品上

### 十五五规划方向

建议在立项材料中以正式文件原文为准，不要引用二手解读。产品上可以对齐这几个方向：

- 特殊教育普惠发展、融合教育
- 教育数字化转型
- 教师减负与教师专业发展
- 个性化学习与教育公平

对应到产品价值就是：

- 用实时字幕降低听障学生获取课堂信息的门槛
- 用 AI 减少教师备课和课后资料整理时间
- 用可回看的字幕、笔记、学习资料支持个性化复习
- 用低成本 Web 方案适配不同学校的网络和设备条件

### 全国特殊教育学校的差异

不能把“特殊教育学校”当成一个完全统一的市场。实际至少有：

- 聋校
- 综合特殊教育学校
- 随班就读的听障学生
- 手语沟通为主、口语沟通为主、书面语沟通为主的不同学生
- 城市学校和县域学校在网络、终端、教师信息化能力上的差异

所以 MVP 必须坚持几个原则：

- **字幕优先**：不依赖音频也能完成学习
- **低带宽可用**：PPT 图片化、字幕文本化、支持弱网重连
- **教师可控**：教师能暂停字幕、隐藏弹幕、切换页面
- **可配置阅读体验**：大字体、高对比度、字幕位置和背景透明度可调
- **不强行上自动手语数字人**：手语内容需要聋人教师审核，MVP 先用字幕和可视化材料

### 特殊教育学校老师的真实教学经历

产品设计要围绕这些痛点：

- 备课时间长，资料整理重复
- 课堂上既要讲、又要切换 PPT，难以兼顾不同听障程度的学生
- 同一知识点需要重复解释
- 课后缺少可复习的课堂记录
- 家长或学生复习时没有结构化材料
- 弹幕、提问等互动如果设计不好，会干扰注意力

因此 MVP 的核心不是“功能多”，而是：

1. 课堂信息能实时、稳定地到达学生
2. 课后能自动沉淀为可搜索、可复习的资料
3. 教师操作尽量少

---

## 2. 推荐技术栈

### 推荐方案：稳妥、可扩展

| 层次 | 技术 | 说明 |
|---|---|---|
| 前端 | Next.js + React + TypeScript + Tailwind CSS + shadcn/ui | 教师端、学生端、管理端统一技术栈 |
| 状态与请求 | TanStack Query + Zustand | 服务端数据和课堂实时状态分开管理 |
| 实时通信 | Socket.IO | 房间、重连、事件确认比裸 WebSocket 更容易做 |
| 实时扩展 | Redis Adapter | 多实例部署时同步房间事件 |
| 主后端 | NestJS + Fastify + Prisma | 认证、课程、房间、文件、权限 |
| AI 服务 | Python FastAPI | ASR、字幕处理、LLM 生成、OCR 更合适 |
| 异步任务 | Celery 或 Arq + Redis | 视频转码、ASR、AI 资料生成不能阻塞课堂 |
| 数据库 | PostgreSQL | 用户、课程、字幕、弹幕、笔记、任务 |
| 向量检索 | pgvector，后期再加 | MVP 单节课不必一开始就上 RAG |
| 缓存/队列 | Redis | 房间状态、Pub/Sub、任务队列 |
| 文件存储 | MinIO 或阿里云 OSS / 腾讯云 COS | PPT、视频、音频、字幕文件 |
| 视频处理 | FFmpeg | 转 HLS、抽音频、生成缩略图 |
| PPT 转换 | LibreOffice Headless 或 Aspose.Slides | PPTX 转 PDF/图片 |
| OCR | PaddleOCR | 扫描版 PPT、图片中的文字 |
| ASR | 阿里云/讯飞/腾讯云实时 ASR，或 FunASR 自托管 | Demo 先用云服务，后续按合规和成本切换 |
| 大模型 | Qwen、DeepSeek、GLM、豆包，或 OpenAI-compatible 网关 | 不把业务锁死在某一家模型 |
| 部署 | Docker Compose + Nginx | MVP 足够，后续再上 K8s |
| 监控 | Sentry + OpenTelemetry | 记录 WebSocket、ASR、AI 任务错误 |
| 测试 | Vitest、Playwright、pytest、k6 | 前端、端到端、AI 服务、WebSocket 压测 |

### 如果团队只有 1～2 人

先不要拆太多微服务，建议：

- Next.js 负责页面和普通 API
- FastAPI 负责 ASR、视频处理、LLM
- PostgreSQL + Redis + MinIO
- Socket.IO 单独一个 Node 进程

等并发和团队规模上来，再把主后端拆成 NestJS。

---

## 3. 总体架构

```text
教师端 Web
  ├─ HTTPS ──────> Nginx ──> Web/API 服务
  ├─ WSS /live ───────────> Socket.IO 房间网关 ──> Redis
  ├─ WSS /asr ────────────> ASR 服务 ──> 云 ASR / FunASR
  └─ PPT/视频上传 ────────> 对象存储 MinIO/S3

学生端 Web
  ├─ HTTPS ──────> Web/API 服务
  ├─ WSS /live ──> Socket.IO 房间网关
  └─ HLS/PPT资源 ─> CDN 或对象存储

后台 Worker
  ├─ PPT 转 PDF/图片
  ├─ 视频转码、抽音频
  ├─ 批量 ASR
  └─ LLM 生成笔记/备课资料/学习资料
```

关键原则：

1. **WebSocket 只传控制消息**：页码、字幕、弹幕、状态，不传 PPT 文件本身。
2. **文件走 HTTP/对象存储**：PPT 图片、视频、字幕文件用普通 HTTPS 下载。
3. **ASR 独立服务**：实时音频流不要和课堂控制消息混在一个 WebSocket 里。
4. **AI 任务异步化**：LLM 生成可能几十秒，不能在课堂请求里同步等待。
5. **服务端权威状态**：当前 PPT 页码、房间状态由服务端维护，学生端跟随。

---

## 4. 核心功能实现

### 4.1 教师端：PPT div + 语音实时字幕

#### PPT 的处理方式

不建议浏览器直接解析 `.pptx`。MVP 推荐：

1. 教师上传 PPTX。
2. 后台使用 LibreOffice Headless 转成 PDF。
3. PDF 再转成每页图片，例如 1920×1080 的 WebP。
4. 同时提取每页文字、备注和图片。
5. 前端用一个 div 容器展示当前页：

```tsx
<div className="slide-stage">
  <img
    src={slides[currentSlide].imageUrl}
    alt={`第 ${currentSlide + 1} 页`}
  />
</div>
```

这样做的好处：

- 兼容复杂排版和字体
- 学生端加载快
- 不依赖 Office 插件
- 比直接解析 PPTX 稳定得多

如果一定要 DOM 可编辑、保留动画，可以使用 OnlyOffice Document Server，但成本高、部署重，不建议放进 MVP。

#### 课堂 PPT 同步

教师端维护：

- 当前课程
- 当前页码
- 当前课堂状态：`draft / live / ended`

学生加入房间后，服务端下发：

- 当前 PPT 页码
- 最近几条弹幕
- 当前字幕序号
- 房间成员信息

教师切页时广播：

```text
slide:change { slideIndex: 5 }
```

学生端只更新当前页，不重新下载整个 PPT。

建议预加载下一页和上一页，切页延迟可以控制在 500ms 以内。

#### 实时语音字幕

流程：

```text
教师麦克风
  -> getUserMedia
  -> AudioWorklet 采集音频
  -> 重采样为 16kHz 单声道 PCM
  -> WebSocket 发给 ASR 服务
  -> ASR 返回 partial / final 文本
  -> 房间 WebSocket 广播给学生
  -> final 文本写入数据库
```

几个关键点：

- 使用 `getUserMedia` 开启回声消除、降噪、自动增益。
- 使用 `AudioWorklet`，不要用已废弃的 `ScriptProcessorNode`。
- 字幕分“临时结果”和“最终结果”：
  - partial：灰色显示，可能被修正
  - final：正式显示并持久化
- 每条字幕带时间戳和当前 PPT 页码，方便课后定位。
- 把本节课 PPT 中的专业词汇提取出来，作为 ASR 热词，显著提升准确率。
- 建议字幕延迟目标：partial 300～800ms，final 1～2 秒。
- 网络差时降级为“只发 final”，减少带宽和计算压力。

浏览器自带的 Web Speech API 只适合 Demo，不建议生产使用。生产环境优先选：

- 快速上线：阿里云、讯飞、腾讯云实时 ASR
- 数据可控：FunASR / Paraformer 自托管
- 后续扩展：说话人分离、标点恢复、专业词热更新

#### 字幕界面

教师端和学生端都要支持：

- 大字体
- 高对比度
- 字幕背景透明度调节
- 显示最近 2～4 行
- 暂停/恢复字幕
- 查看完整字幕记录
- 断线重连提示
- 字幕与 PPT 页码关联

---

### 4.2 教师端：根据 PPT 备课

处理流水线：

```text
PPTX
  -> 提取每页标题、正文、备注
  -> 图片页 OCR
  -> 按页/chunk 结构化
  -> LLM 生成备课资料
  -> 教师审核、编辑、发布
```

可生成的资料：

- 课堂导入语
- 教学目标
- 重点难点
- 知识点提纲
- 专业术语解释
- 课堂提问
- 随堂练习和答案
- 课后复习清单
- 面向听障学生的简化版讲解
- 可视化教学建议

实现建议：

- 输出必须使用结构化 JSON，不要只返回一段自由文本。
- 每个知识点保留来源，例如“第 3 页”“第 7 页”。
- 教师端提供编辑和审核，AI 结果不要直接发布给学生。
- 含公式、图表、图片的 PPT，后续可以接视觉模型；MVP 先用文字提取和 OCR。

---

### 4.3 学生端：接收 PPT 和弹幕

学生通过 6 位房间码或二维码加入：

```text
room:join { roomCode }
```

服务端返回：

```text
room:state {
  lessonId,
  slideIndex,
  sessionStatus,
  recentDanmaku,
  lastCaptionSeq
}
```

弹幕功能建议：

- WebSocket 广播
- 数据库持久化
- 长度限制
- 发送频率限制
- 敏感词过滤
- 教师可关闭弹幕、隐藏单条弹幕、禁言
- 默认不要做高速滚动弹幕，避免干扰听障学生阅读字幕
- 可以提供“静态讨论区”模式，比飘屏更适合课堂

接入权限：

- JWT 中带 `teacher` / `student` 角色
- 只有教师能发 `slide:change`
- 只有房间成员能发弹幕
- 文件 URL 使用短期签名地址

---

### 4.4 学生端：上传视频 + 实时字幕 + 笔记

这里的“实时字幕”建议拆成两种模式：

#### 推荐模式：上传后预处理

```text
视频上传
  -> 对象存储
  -> FFmpeg 转 HLS
  -> 抽取音频
  -> ASR 生成带时间戳的字幕
  -> 生成 WebVTT
  -> 播放时与视频同步显示
```

优点：

- 字幕稳定
- 可以处理长视频
- 可以生成完整笔记
- 不依赖学生观看时的网络和算力

上传建议使用分片上传或预签名直传，避免大文件经过业务服务器。

#### 播放器

- 使用 hls.js 或 Vidstack
- 支持 `<track>` 字幕
- 建议自定义字幕渲染，方便做大字体、高对比度、字幕位置调节
- 字幕和视频时间轴同步
- 点击字幕可以跳转到对应时间点

#### 笔记生成

```text
字幕分段
  -> 按时间/主题合并
  -> LLM 生成结构化笔记
  -> 笔记条目带时间戳
  -> 学生点击时间戳跳转视频
```

笔记可以包含：

- 课程摘要
- 章节提纲
- 关键词
- 重点内容
- 难点解释
- 复习问题
- 个人补充笔记

支持导出 Markdown / PDF。

---

### 4.5 根据一节课字幕生成学习资料

不建议把整节课字幕一次性丢给模型。推荐：

```text
字幕清洗
  -> 按 5～10 分钟切段
  -> 每段生成摘要和知识点
  -> 合并成课堂总览
  -> 生成测验、术语表、复习卡片
  -> 结构化校验
  -> 教师审核发布
```

生成内容：

- 课堂摘要
- 知识结构图文本版
- 重点/难点
- 术语表
- 例题与答案
- 随堂测验
- 复习卡片
- 面向听障学生的简化阅读版
- 每条结论对应的字幕时间戳和 PPT 页码

技术要求：

- 使用 JSON Schema / Pydantic 校验输出
- 低温度生成，减少胡编
- 重要结论必须带来源
- 教师可以编辑、删除、重新生成
- 生成任务异步执行，前端显示进度
- MVP 单节课不需要复杂 RAG；后续做跨课程问答时再加 pgvector

---

## 5. WebSocket 房间设计

### 房间状态机

```text
draft -> live -> ended
```

教师创建课程后生成房间码，学生加入；教师开始上课后进入 `live`；下课后进入 `ended`。

### 建议事件

| 事件 | 方向 | 作用 |
|---|---|---|
| `room:join` | 客户端 → 服务端 | 加入房间 |
| `room:state` | 服务端 → 客户端 | 当前 PPT、状态、最近弹幕 |
| `slide:change` | 教师 → 服务端 → 学生 | 切换 PPT |
| `caption:partial` | ASR → 服务端 → 学生 | 临时字幕 |
| `caption:final` | ASR → 服务端 → 学生 | 最终字幕并入库 |
| `danmaku:send` | 学生 → 服务端 | 发送弹幕 |
| `danmaku:new` | 服务端 → 房间 | 广播弹幕 |
| `session:end` | 教师 → 服务端 → 学生 | 下课 |
| `job:progress` | 服务端 → 客户端 | AI 任务进度 |

### 重连与消息补偿

- 每条字幕带递增 `seq`
- 客户端重连时上报最后收到的 `seq`
- 服务端从数据库或 Redis 补发遗漏的 final 字幕
- partial 字幕不持久化，final 才入库
- 使用幂等事件 ID，避免重复消息

### 扩展性

- Socket.IO 使用 Redis Adapter
- 生产环境只开 WebSocket 传输，减少长轮询带来的粘性会话问题
- Nginx 配置 WebSocket Upgrade
- 单房间先按 1 教师 + 50 学生设计
- 压测至少覆盖 10 个并发房间
- ASR 并发数通常是瓶颈，要单独做限流和排队

---

## 6. 核心数据模型

建议至少有这些表：

- `users`：用户、角色、学校
- `schools`：学校和多租户隔离
- `classes`：班级
- `class_members`：班级成员
- `lessons`：课程
- `slides`：PPT 页码、图片地址、提取文本、备注
- `live_sessions`：直播课堂场次、房间码、状态
- `caption_segments`：字幕片段、时间戳、PPT 页码、是否 final
- `danmaku`：弹幕内容、发送人、审核状态
- `videos`：上传视频、转码状态、HLS 地址
- `video_transcripts`：视频字幕片段
- `notes`：学生笔记
- `materials`：AI 生成的备课资料/学习资料
- `ai_jobs`：ASR、转码、LLM 任务状态
- `audit_logs`：关键操作审计

所有业务表建议带 `school_id`，方便后续做多学校部署和数据隔离。

---

## 7. 无障碍与合规

这是特殊教育产品不能省的部分。

### 无障碍

- 按 WCAG 2.2 AA 设计
- 不依赖纯音频提示
- 连接断开、任务完成等状态要有视觉提示
- 支持键盘操作
- 支持字体放大
- 支持高对比度
- 支持减少动画
- 弹幕默认关闭或静态显示
- 字幕支持调节字号、行数、位置、背景透明度
- 重要内容不要只放在快速滚动的弹幕里

### 隐私与合规

- 涉及未成年人个人信息，需监护人/学校授权
- 默认不保存教师原始音频，只保存字幕文本
- 如要录音录像，必须明确告知、授权和设置保存期限
- 使用国内云服务或私有化部署，避免学生数据跨境
- 上传文件做类型、大小、病毒扫描
- PPT 转换在隔离容器中执行，不启用宏
- 弹幕做审核、限流、举报和教师管理
- AI 生成内容必须可追溯、可编辑、可撤回

---

## 8. 推荐实施顺序

以 2～3 人团队为例，MVP 大约 6～8 周：

| 阶段 | 内容 |
|---|---|
| 第 1 周 | 项目骨架、登录、角色、班级、课程 |
| 第 2 周 | PPT 上传、转 PDF/图片、教师端展示 |
| 第 3 周 | WebSocket 房间、学生加入、PPT 同步、弹幕 |
| 第 4 周 | 教师语音采集、实时 ASR、字幕展示与入库 |
| 第 5 周 | 视频上传、转码、批量字幕、视频播放器 |
| 第 6 周 | 字幕笔记生成、AI 学习资料、教师审核 |
| 第 7 周 | 无障碍、弱网、权限、安全、压测 |
| 第 8 周 | 试点学校试用、问题修复、Demo 打磨 |

### MVP 验收标准

- 教师 2 分钟内创建课程并上传 PPT
- 学生通过房间码进入
- PPT 切页 P95 小于 500ms
- 字幕 final 延迟 P95 小于 2 秒
- 弹幕延迟小于 500ms
- 断线重连 5 秒内恢复
- 课堂字幕完整保存并可搜索
- 视频上传后能生成同步字幕和带时间戳笔记
- AI 学习资料带来源，教师可编辑后发布
- 单房间 50 人、10 个并发房间压测通过

### Demo 演示流程

1. 教师登录，创建课程，上传 PPT。
2. 学生用房间码加入。
3. 教师开始上课，切换 PPT。
4. 教师讲话，学生端实时看到字幕。
5. 学生发送弹幕，教师端和学生端同步显示。
6. 下课后，系统保存完整字幕。
7. 教师一键生成课堂摘要、重点、测验。
8. 学生上传一段视频，系统生成字幕和笔记。
9. 学生点击笔记时间戳跳转到视频对应位置。

---

## 9. 最关键的五个架构决策

1. **PPT 文件不要走 WebSocket**，只同步页码和资源 URL。
2. **ASR 单独做服务**，不要塞进课堂房间 WebSocket。
3. **AI 生成异步化**，课堂中只做实时字幕，资料生成放到后台。
4. **字幕是核心资产**，必须结构化存储、带时间戳、可搜索、可追溯。
5. **MVP 不做实时音视频直播和自动手语数字人**，先把“PPT + 字幕 + 弹幕 + 课后资料”做稳。

当前工作目录还是空项目。如果你要继续，我可以直接按这个方案先搭一个最小可运行的骨架：WebSocket 多人房间、PPT 上传与同步、学生加入、弹幕，以及实时字幕服务的接口层。

---

## Turn 2：怎么实现ppt的播放功能，转成img吗。通过什么技术转

### 我的 Prompt

怎么实现ppt的播放功能，转成img吗。通过什么技术转

### codex 回复

是，MVP 建议把 PPT 转成图片来播放。但更稳妥的方式是两段式转换，不建议直接把 PPTX 转成图片：

```text
PPTX
  -> LibreOffice Headless
  -> PDF
  -> PDFBox / PDFium
  -> 每页 PNG 或 WebP
  -> MinIO/OSS + CDN
  -> Vue 按页展示
```

这样比浏览器直接解析 PPTX 稳定得多，能较好保留字体、版式、图表和复杂排版。

---

## 1. 推荐技术组合

```text
Spring Boot + Apache POI + LibreOffice Headless + PDFBox + MinIO/OSS + Vue 图片播放器
```

| 技术 | 作用 |
|---|---|
| Apache POI | 提取 PPT 文字、标题、备注 |
| LibreOffice Headless | PPTX 转 PDF |
| Apache PDFBox | PDF 每页转 PNG |
| MinIO/OSS | 保存原始 PPT、PDF、图片 |
| Vue | 用一个 div 容器按页播放图片 |
| WebSocket/STOMP | 只同步当前页码，不传图片 |

---

## 2. 转换流程

### 第一步：PPTX 转 PDF

在独立的转换 Worker 中执行：

```bash
soffice --headless --convert-to pdf --outdir /work/output /work/input.pptx
```

注意：

- 不要让 LibreOffice 直接跑在 Spring Boot 主进程里。
- 转换任务异步执行，避免阻塞上传接口。
- 容器内安装中文字体，例如 Noto Sans CJK、思源黑体、文泉驿字体。
- 限制文件大小、页数、CPU、内存和转换超时。
- 禁止宏、外链和网络访问。
- 保留原始 PPTX，方便后续重新转换。

### 第二步：PDF 转图片

Spring Boot 可以集成 Apache PDFBox：

```java
try (PDDocument document = Loader.loadPDF(pdfFile)) {
    PDFRenderer renderer = new PDFRenderer(document);

    for (int page = 0; page < document.getNumberOfPages(); page++) {
        BufferedImage image = renderer.renderImageWithDPI(
            page,
            150,
            ImageType.RGB
        );

        Path output = outputDir.resolve("slide-" + (page + 1) + ".png");
        ImageIO.write(image, "png", output.toFile());
    }
}
```

建议输出：

- 完整图：1920×1080 或 1600×900
- 缩略图：320×180
- 格式：PNG 或 WebP
- WebP 可再通过 `cwebp` 或 `webp-imageio` 转换
- 每页保存页码、图片地址、缩略图地址、宽度、高度

### 第三步：保存幻灯片数据

`slides` 表建议包含：

```text
id
lesson_id
page_no
image_url
thumb_url
width
height
text_content
notes
created_at
```

---

## 3. Vue 怎么播放

WebSocket 只同步当前页码：

```json
{
  "type": "slide:change",
  "slideIndex": 5
}
```

Vue 根据页码找图片：

```vue
<div class="ppt-stage">
  <img
    :src="currentSlide.imageUrl"
    :alt="currentSlide.altText || `第 ${currentSlide.pageNo} 页`"
  />
</div>
```

建议实现：

- 教师端提供上一页、下一页、跳页。
- 学生端默认跟随教师页码。
- 预加载下一页和上一页。
- 图片使用 `object-fit: contain`。
- 保持 16:9 舞台比例。
- 页面切换只发页码，不发图片文件。
- 支持“跟随教师”和“自由浏览”切换。

---

## 4. 关于 PPT 动画

转成图片后，PPT 的动画、切换效果和内嵌视频不会保留。

MVP 建议：

- 每页 PPT 视为静态图片。
- 教师手动切页。
- 复杂动画页面提前导出成多页 PPT。
- 如果后续确实需要完整动画，再评估 OnlyOffice Document Server 或 Aspose.Slides。

---

## 5. 其他方案对比

| 方案 | 是否推荐 | 说明 |
|---|---|---|
| PPTX → PDF → 图片 | 推荐 | 稳定、低客户端压力，适合课堂同步 |
| PDF.js 直接渲染 PDF | 可选 | 文字可搜索，但客户端渲染压力更大 |
| Aspose.Slides | 可选 | 保真度高，商业授权 |
| OnlyOffice Document Server | 后期 | 支持较完整播放和编辑，但部署重 |
| 浏览器直接解析 PPTX | 不推荐 | 兼容性差，版式容易错乱 |
| LibreOffice 直接转 PNG | 不推荐 | 多页处理和版式稳定性不如先转 PDF |

---

已把这部分补充到开发文档的 **7.1.1 PPT 播放与转换方案**：

[听障学生学习平台开发文档](D:/Hearing-impairedLearningPlatform/docs/DEVELOPMENT.md)

---

## Turn 3：PLEASE IMPLEMENT THIS PLAN:

### 我的 Prompt

PLEASE IMPLEMENT THIS PLAN:
# 听障学生学习平台完整 Demo MVP 实施计划

## 摘要

- 从空工作区创建 `backend/` 与 `frontend/`：Spring Boot 3、Java 17 语言级别、MyBatis-Plus、MySQL 8；Vue 3、Vite、JavaScript、Element Plus、Axios、原生 WebSocket。
- 完成文档中的完整主线：教师上传 PPTX、转 PDF、创建课堂；学生输入房间码加入；同步页码、字幕和弹幕；教师生成课堂资料；学生上传视频、生成字幕与笔记。
- AI 走硅基流动 OpenAI 兼容接口；ASR 返回整段文本，因此按句子长度和视频时长估算 VTT 时间轴并在界面明确标注。
- 预置教师、学生账号；不实现注册、账号管理和部署上线。

## 核心实现

- **基础环境**：安装 LibreOffice 与 FFmpeg，路径可配置；创建 MySQL `hearing_platform`；敏感配置放在未跟踪的本地配置文件或环境变量中。
- **鉴权与权限**：JWT Bearer Token、BCrypt 密码；预置 `teacher/123456`、`student/123456`；教师只能管理自己的课程和房间，学生只能访问已加入课堂的资料及自己的视频。
- **教师端**：上传 `.pptx`，校验大小和扩展名；LibreOffice 转 PDF，POI 提取文字，PDFBox 计算页数；课程列表与备课页展示 PDF、教案 Markdown；开课时创建新房间和唯一 6 位房间码，支持切页、字幕、弹幕和结束课堂。
- **学生端**：验证房间码并加入进行中的课堂；接收当前页码、字幕和弹幕；课堂结束后查看教师生成的摘要、重点、术语和测验。
- **实时通信**：`/ws/classroom?token=JWT` 使用原生 WebSocket；服务端验证角色、课程归属和房间状态，持久化最终字幕、弹幕与页码；教师端优先使用 Chrome/Edge Web Speech API，并提供逐句广播的演示字幕脚本兜底。
- **视频流程**：上传 MP4/WebM，FFmpeg 提取 16kHz 单声道音频；SiliconFlow `FunAudioLLM/SenseVoiceSmall` 返回整段文本；FFprobe 获取时长，按句长比例生成 VTT；HTML5 `<video>` 加载字幕，LLM 根据 transcript 生成笔记。
- **AI 能力**：SiliconFlow Chat Completions 默认模型 `Qwen/Qwen2.5-72B-Instruct`，实现 PPT 备课资料、课堂学习资料和视频笔记三套中文 Prompt；结果保存为 Markdown，前端安全渲染。
- **失败兜底**：配置 `AI_MODE=real|auto|mock`，默认 `auto`；真实接口失败时保存并展示明确标记为“模拟结果”的确定性中文示例，不阻断演示；关闭 Web Speech 或麦克风不可用时仍可演示字幕流程。
- **界面与无障碍**：登录、教师课程、教师备课、教师课堂、学生加入、学生课堂、学生视频七个页面；字幕大字号、高对比度、固定静态弹幕列表、文字化连接状态、键盘焦点和 ARIA 标签；桌面优先并支持窄屏布局。

## 接口、消息与数据

- **REST 主要接口**：
  - `POST /api/auth/login`、`GET /api/auth/me`
  - `GET/POST /api/lessons`、`GET /api/lessons/{id}`
  - `POST /api/rooms`、`GET /api/rooms/{id}`、`GET /api/rooms/by-code/{code}`、`POST /api/rooms/{id}/end`
  - `GET /api/rooms/{id}/captions`、`GET /api/rooms/{id}/danmaku`
  - `POST /api/ai/prepare/{lessonId}`、`POST /api/ai/material/{lessonId}`、`POST /api/ai/transcribe/{videoId}`、`POST /api/ai/note/{videoId}`
  - `GET /api/materials/class/{lessonId}`、`GET/POST /api/videos`、`GET /api/videos/{id}`
- **统一响应**：`{code, message, data}`，使用对应 HTTP 状态码；前端统一处理 401、403、业务错误和加载状态。
- **WebSocket 消息**：客户端发送 `join|slide|caption|danmaku|end`；服务端返回 `state|slide|caption|danmaku|end|error`。房间之间严格隔离，学生切页请求被拒绝。
- **数据库表**：`app_user`、`lesson`、`room`、`room_member`、`caption`、`danmaku`、`material`、`video`。`material` 按课程和类型唯一；`video` 保存原始视频、音频、VTT、转写、笔记、状态及 AI/模拟来源标记。
- **文件**：统一存入 `uploads/{pptx,pdf,video,audio,vtt}`，使用 UUID 文件名；通过 `/files/**` 提供本地访问；限制上传大小并清理转换失败产生的临时文件。

## 测试与验收

- 后端自动测试：登录与角色权限、PPTX 校验与转换失败、房间码唯一性、课堂加入条件、WebSocket 房间隔离/切页/字幕/弹幕、VTT 时间轴生成、AI 模拟降级、学生资料访问控制。
- 构建检查：`mvn test`、`npm run build` 全部通过；前后端可在 8080/5173 启动。
- 真实联调：教师上传 PPT 并转 PDF，创建课堂；双浏览器验证学生加入、同步切页、字幕和弹幕；结束课堂并生成资料；上传视频、转写、播放 VTT、生成笔记。
- 降级验收：分别使用 `AI_MODE=mock` 和错误的真实 Key，确认页面显示模拟/失败状态且课堂主流程不中断。
- 最终验收以 Chrome/Edge、真实 MySQL、真实 SiliconFlow Key 的完整 8–10 分钟演示流程为准。

## 假设与默认值

- 开始实施联调前，你通过本地未跟踪配置或环境变量提供 `DB_USERNAME`、`DB_PASSWORD`、`SILICONFLOW_API_KEY`；`JWT_SECRET` 由实现时随机生成，不提交仓库。
- 使用本机已运行的 MySQL 8，并新建 `hearing_platform`；使用 JDK 21 编译 Java 17 目标字节码。
- 第一版只接受 `.pptx` 和浏览器可播放的 MP4/WebM；不做 PPT 动画、视频转码、HLS、注册、管理后台或服务器部署。
- Web Speech API 假定使用联网的 Chrome/Edge；不可用时使用内置演示字幕脚本。
- 硅基流动 ASR 不提供时间戳，因此视频字幕明确标记为“按文本长度估算时间轴”，不宣称逐词精确同步。

### codex 回复

代码、测试与前端构建已经完成。现在进入真实联调阶段，需要你提供：

- 本机 MySQL 用户名和密码（默认数据库名 `hearing_platform`，会自动创建）
- SiliconFlow API Key

你可以直接回复这两项，或填入 `D:\Hearing-impairedLearningPlatform\backend\config\local.properties` 后回复“已填好”。该文件已被 `.gitignore` 忽略。

---

## Turn 4：LibreOffice 你下了吗，下在哪

### 我的 Prompt

LibreOffice 你下了吗，下在哪

### codex 回复

已下载。LibreOffice 26.8.1 已解包到：

```text
D:\Hearing-impairedLearningPlatform\tools\libreoffice
```

实际执行文件是：

```text
D:\Hearing-impairedLearningPlatform\tools\libreoffice\program\soffice.com
```

它不是系统级安装，而是从官方 MSI 解包后直接运行；后端配置中的 `LIBREOFFICE_PATH` 已指向这个文件。

---

## Turn 5：教师端课程列表应该设计一个删除课程的按钮。完善一下前后端。

### 我的 Prompt

教师端课程列表应该设计一个删除课程的按钮。完善一下前后端。

### codex 回复

教师端课程删除功能已完成，前后端均已联调通过。

## 后端

新增教师专用接口：

```text
DELETE /api/lessons/{id}
```

删除规则：

- 只能删除当前教师自己的课程
- 存在进行中的课堂时禁止删除，并提示“请先结束课堂再删除课程”
- 删除课程时会一并清理：
  - 房间和房间成员
  - 字幕与弹幕
  - AI 备课/课堂资料
  - PPTX 与 PDF 文件
  - 课程记录

## 前端

- 课程列表每一行新增“删除”按钮
- 点击后显示二次确认弹窗
- 删除成功自动刷新课程列表
- 失败时显示后端返回的具体原因

已验证：

- 进行中课堂删除会返回 `400`
- 已结束课堂及关联数据可正常级联删除
- PPTX/PDF 文件同步删除
- 浏览器端点击删除并确认后，课程立即从列表消失
- `mvn test` 和 `npm run build` 均通过

提交已推送到 `test001`：

- `8e0acf5`：页头按钮颜色
- `c0e463b`：教师课程删除功能

当前分支与 `origin/test001` 已同步。

---

## Turn 6：我已经结束课程，但是删不掉，提示我没有结束课程

### 我的 Prompt

我已经结束课程，但是删不掉，提示我没有结束课程

### codex 回复

问题已修复。

原因：这门课程下有多条历史房间仍处于 `LIVE` 状态。旧逻辑只要发现任意一个 `LIVE` 房间，就会拒绝删除，即使你已经结束了当前课堂。

现在的规则：

- 删除课程时会自动清理该课程的所有房间，包括仍标记为 `LIVE` 的历史房间
- 同时删除字幕、弹幕、成员、AI 资料和 PPTX/PDF 文件
- 以后每次重新开课时，会自动结束该课程之前遗留的 `LIVE` 房间，避免再次累积

已验证：

- 存在 `LIVE` 房间的测试课程可以正常删除
- 关联数据和文件均被清理
- 你的课程“光合作用”仍然保留，没有被测试删除

修复已推送到 `test001`，提交为 `09e7c88`。后端已重启，刷新页面后可以重新点击删除。

---

## Turn 7：你发现没，教师端3个快捷方式都返回同一个页面会不会太单调了。建议点击实时课堂可以进入课堂列表。其他两个快捷方法的ui界面做一下处…

### 我的 Prompt

你发现没，教师端3个快捷方式都返回同一个页面会不会太单调了。建议点击实时课堂可以进入课堂列表。其他两个快捷方法的ui界面做一下处理。现把每个快捷界面的功能体现出来吧。后面的慢慢改。不要每个页面都返回课程列表。你新建两个vue页面做展现。先体现核心功能，其他不用多管。不用commit和push

### codex 回复

教师端三个快捷入口已拆分，不再全部跳到课程列表：

- **课程与课件** → `/teacher/lessons`
- **实时课堂** → 新建页面 `/teacher/classrooms`
- **备课中心** → 新建页面 `/teacher/prepare-center`

## 实时课堂页面

![实时课堂](D:/Codex/.codex/visualizations/2026/10/19/01a15439-f72d-7122-9e4d-849453b3f731/teacher-classrooms-preview.png)

页面包含：

- 进行中、已结束课堂数量
- 全部 / 进行中 / 已结束筛选
- 6 位房间码、课程名称、当前页码和结束时间
- “进入课堂”和“查看课堂记录”按钮
- 新增后端接口 `GET /api/rooms/mine`

## 备课中心页面

![备课中心](D:/Codex/.codex/visualizations/2026/10/19/01a15439-f72d-7122-9e4d-849453b3f731/teacher-prepare-center-preview.png)

页面包含：

- 备课流程说明
- 可用课程数与课件总页数
- 每门课程的独立卡片
- “进入备课”按钮，直接进入原有 PDF 预览与 AI 备课资料页面

首页三个快捷卡片也增加了 `课程库 / LIVE / AI` 标识和不同配色。

`mvn test`、`npm run build` 和浏览器路由验收均通过。预览用临时课堂已清理，你的课程数据仍然保留。

按你的要求，**没有 commit，也没有 push**；当前修改保留在本地 `test001` 分支。

---
