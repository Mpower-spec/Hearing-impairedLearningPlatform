# 学生端视频学习与笔记 · 原生 Prompt 链

---

## Turn 1：PLEASE IMPLEMENT THIS PLAN:

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

## Turn 2：我了解一下，现在上传视频是调用了阿里云的ASR模型，而实时字幕是speech API是吗。回答问题就行

### 我的 Prompt

我了解一下，现在上传视频是调用了阿里云的ASR模型，而实时字幕是speech API是吗。回答问题就行

### codex 回复

不是阿里云。

- **上传视频字幕**：FFmpeg 提取 MP3 后，调用 **SiliconFlow（硅基流动）** 的 `FunAudioLLM/SenseVoiceSmall` ASR 模型，再生成估算时间轴 VTT。
- **实时字幕**：使用浏览器的 **Web Speech API**（Chrome/Edge），由教师端浏览器识别语音，再通过 WebSocket 广播给学生。
- 视频笔记和课堂资料使用 **SiliconFlow** 的 `Qwen/Qwen2.5-72B-Instruct`。

---

## Turn 3：FFmpeg/FFprobe下在哪

### 我的 Prompt

FFmpeg/FFprobe下在哪

### codex 回复

FFmpeg 和 FFprobe 位于：

```text
D:\Hearing-impairedLearningPlatform\tools\node_modules\@ffmpeg-installer\win32-x64\ffmpeg.exe
D:\Hearing-impairedLearningPlatform\tools\node_modules\@ffprobe-installer\win32-x64\ffprobe.exe
```

后端 `backend/config/local.properties` 中的 `FFMPEG_PATH` 和 `FFPROBE_PATH` 已指向这两个文件。

---

## Turn 4：如果实时字幕调用 **SiliconFlow（硅基流动）** 的 `FunAudioLLM/SenseVoiceSmall` A…

### 我的 Prompt

如果实时字幕调用 **SiliconFlow（硅基流动）** 的 `FunAudioLLM/SenseVoiceSmall` ASR 模型。可行吗？告诉我答案。不做任何修改

### codex 回复

可行，但只能做“准实时”，不是真正的流式识别。

原因和限制：

- `FunAudioLLM/SenseVoiceSmall` 是**文件转写接口**，不是实时流式 ASR
- 需要浏览器用 `MediaRecorder` 录制 3–5 秒音频分片，再上传到后端调用 SiliconFlow
- 需要处理分片重叠、重复文本、断句、网络延迟和请求频率
- 实际字幕延迟通常在数秒左右，网络或模型排队时会更长
- 浏览器麦克风仍要求 **localhost 或 HTTPS**，局域网 HTTP 无法授权
- SiliconFlow 单文件限制约 50MB、1 小时，课堂短分片通常不会触及
- 该接口只返回整段文本，不提供精确时间戳，但实时字幕不依赖时间戳

结论：**比赛 Demo 可以这样做，但体验不如 Web Speech API；正式课堂实时字幕不建议把 SenseVoice 文件接口当作真正的流式 ASR。**

---

## Turn 5：那有没有替代speech api的改良方法

### 我的 Prompt

那有没有替代speech api的改良方法

### codex 回复

有，主要有三类改良方案。

| 方案 | 延迟 | 稳定性 | 是否需要新 Key | 适合程度 |
|---|---:|---|---|---|
| 优化 Web Speech API | 低 | 一般 | 否 | 最快落地 |
| SiliconFlow 分片转写 | 约 3–8 秒 | 较高 | 否 | 现有条件下最适合的兜底 |
| 真正的流式 ASR 服务 | 约 1–2 秒 | 高 | 是 | 正式课堂最佳 |

## 1. 优化现有 Web Speech API

这是改动最小的方案：

- 教师端固定使用本机 `http://localhost:5173`，避免局域网 HTTP 无法授权麦克风
- 优先使用 Edge，国内通常比 Chrome 的语音服务更稳定
- 开启 `continuous` 和 `interimResults`
- 遇到 `network`、`not-allowed` 时停止重试，并自动提示使用演示字幕
- 保留“下一句演示字幕”作为现场兜底
- 增加静音检测，避免长时间无语音时反复重启识别

优点是零额外费用，缺点是仍依赖浏览器厂商的云服务，无法保证现场网络环境下一定可用。

## 2. SiliconFlow 分片实时转写

用浏览器录制 3–5 秒音频分片，发送到 Spring Boot，再由后端调用 SiliconFlow `SenseVoiceSmall`。

需要处理：

- 音频分片重叠 0.5–1 秒
- 相邻分片文本去重
- 静音检测，避免上传空白音频
- 请求队列和失败重试
- 字幕延迟和顺序控制
- 浏览器麦克风仍要求 `localhost` 或 HTTPS

优点是不需要新增供应商，现有 Key 可以直接使用；缺点是这不是真正的流式 ASR，通常会有数秒延迟。

## 3. 真正的流式 ASR

更可靠的方式是改用支持 WebSocket 流式识别的服务，例如：

- 阿里云百炼 Paraformer 实时语音识别
- 腾讯云实时语音识别
- 讯飞实时语音转写
- 火山引擎流式 ASR

架构为：

```text
教师浏览器麦克风
  -> Spring Boot WebSocket
  -> 云厂商流式 ASR WebSocket
  -> 返回中间结果和最终结果
  -> 再广播给学生
```

这种方式延迟通常能控制在 1–2 秒，支持实时断句和标点，但需要额外的供应商 Key，并且浏览器麦克风仍必须运行在 `localhost` 或 HTTPS 下。

## 推荐结论

- **不想增加新服务商**：保留 Web Speech API，增加 SiliconFlow 分片转写作为自动兜底。
- **希望课堂现场稳定**：接入阿里云 Paraformer 或腾讯云实时 ASR。
- **只做比赛 Demo**：当前“Web Speech + 演示字幕”已经够用，但应明确使用 `localhost` 和 Edge。
- **教师从另一台电脑讲课**：必须配置 HTTPS，否则浏览器不会开放麦克风。

---

## Turn 6：学生端我的视频也应该有删除的逻辑，如上面教师端删除课程一般

### 我的 Prompt

学生端我的视频也应该有删除的逻辑，如上面教师端删除课程一般

### codex 回复

学生端“视频学习”已增加视频删除功能。

## 功能

- 选中视频后显示“删除视频”按钮
- 删除前弹出二次确认
- 删除时同时清理：
  - 视频数据库记录
  - 原始视频文件
  - FFmpeg 提取的音频文件
  - VTT 字幕文件
  - 转写文本和 AI 学习笔记
- 删除后自动选中列表中的下一个视频

## 后端

新增学生专用接口：

```text
DELETE /api/videos/{id}
```

只能删除当前学生自己的视频，教师或其他学生无法删除。

## 验证

- API 删除返回 `200`，数据库记录和三类文件均已删除
- 浏览器端点击删除、确认后，视频立即从列表移除
- `mvn test` 和 `npm run build` 均通过
- 原有视频数据没有被测试影响

提交已推送到 `test001`：

- 提交：`9ca8955`
- 分支状态：`test001...origin/test001` 已同步

---

## Turn 7：PLEASE IMPLEMENT THIS PLAN:

### 我的 Prompt

PLEASE IMPLEMENT THIS PLAN:
# 学生端移动端适配方案

## 摘要

- 优先适配学生手机浏览器，教师端保持桌面体验并确保窄屏下可正常使用。
- 使用三档响应式断点：
  - `≤767px`：手机
  - `768–1119px`：平板
  - `≥1120px`：桌面
- 学生端增加固定底部导航：首页、加入课堂、视频学习、学习社区。
- 手机课堂采用竖屏上下布局：字幕、PPT、控制按钮依次排列，不作横屏强制。
- 不修改后端 API、数据库和业务数据流。

## 实现改动

- 新增移动端底部导航组件，仅学生登录后显示：
  - 首页：`/home`
  - 课堂：`/student/join`
  - 视频：`/student/videos`
  - 社区：`/community`
  - 使用安全区 `env(safe-area-inset-bottom)`，避免被手机底部手势区域遮挡。
- Header 移动端压缩：
  - 品牌标题和副标题缩小。
  - 用户信息、退出按钮和数字人控制按钮紧凑排列。
  - 隐藏与底部导航重复的“学习社区”“返回首页”按钮。
  - 页面根布局增加底部安全间距，避免内容被底部导航覆盖。
- 全局移动端规范：
  - 按钮、输入框和可点击卡片最小触控高度 `44px`。
  - 移动端输入字号不低于 `16px`，防止 iOS 自动放大。
  - 禁止横向溢出，保证长标题、标签和文本自动换行。
  - 统一弹窗、抽屉和表单在手机上使用 `94vw` 左右宽度。
- 首页：
  - 欢迎区改为紧凑上下布局。
  - 轮播高度缩减到 `220–260px`，隐藏纯装饰示意图。
  - 快捷入口改为单列横向卡片，图标和文字保持清晰。
- 加入课堂页：
  - 降低顶部空白。
  - 房间码输入完整占满宽度。
  - 课堂预览和加入按钮改为全宽。
- 学生课堂页：
  - 顶部课堂信息允许换行。
  - 实时字幕置顶并保持大字号、高对比度。
  - PPT 使用全宽 `16:9` 自适应，不设置固定高度。
  - 翻页、弹幕开关、课堂记录按钮换行排列。
  - 课堂记录抽屉手机端使用全屏或 `92vh` 高度。
  - 竖屏下保持正常页面滚动，不启用全屏 `overflow: hidden`。
- 学生视频页：
  - 上传标题、文件选择、上传按钮改为全宽纵向布局。
  - 视频播放器保持 `16:9` 全宽。
  - 视频列表、字幕、笔记按单列排列。
  - 操作按钮支持自动换行。
- 学习社区：
  - 社区介绍卡改为上下布局。
  - 分类、搜索框、搜索按钮变为全宽。
  - 帖子卡片保持单列，作者、评论数和删除按钮重新排布。
  - 发帖和详情弹窗接近全屏，评论输入区域适配软键盘。
- 教师端：
  - 课程、课堂、备课和社区页面沿用现有堆叠布局。
  - 避免表格和按钮在窄屏横向溢出。
  - 保持桌面端布局和交互不变。

## 接口与兼容性

- 新增 `MobileBottomNav.vue`，根据当前路由和学生角色显示。
- 不新增 REST 接口、WebSocket 消息、数据库字段或业务状态。
- 不改变桌面端路由和现有页面地址。
- 移动端使用同一套 Vue 页面和 JWT 登录状态，不创建独立移动站点。
- 后续微信小程序仍需独立项目，本方案只覆盖移动端 H5。

## 测试计划

- 在 `375×667`、`390×844`、`430×932` 和 `768×1024` 尺寸下逐页检查。
- 确认首页、社区、加入课堂、视频学习和课堂页无横向滚动。
- 确认底部导航不会遮挡按钮、表单、视频控制栏或聊天输入框。
- 验证手机竖屏课堂字幕清晰、PPT 全宽、翻页和记录功能可用。
- 验证手机上传视频、生成字幕、生成笔记和删除视频流程。
- 验证社区分类、搜索、发帖、评论和删除流程。
- 验证软键盘弹出时评论和发帖输入区域可滚动。
- 验证数字人控制栏、隐藏/显示和拖动在手机上不覆盖主要操作。
- 检查底部手势安全区、刘海屏和横竖屏切换。
- 运行 `npm run build` 和后端现有测试。

## 假设

- 本轮只做移动端 H5 响应式适配，不做微信小程序或 PWA。
- 学生手机是主要适配目标，教师手机端只保证功能和布局可用。
- 课堂页面优先支持竖屏上下布局，不强制横屏。
- 数字人仍保持全局单例，移动端不创建额外实例。

### codex 回复

学生端移动端适配已完成：

- 新增学生端底部导航：首页、课堂、视频、社区。
- 底部导航支持手机安全区，避免被手势条遮挡。
- 移动端 Header 改为紧凑上下布局。
- 学生端隐藏与底部导航重复的顶部导航按钮。
- 手机端按钮、输入框和点击区域增大，输入字号保持 16px 防止 iOS 自动放大。
- 首页欢迎区、轮播、快捷入口完成手机布局调整。
- 加入课堂页降低顶部空白，房间码和操作按钮适配全宽。
- 学生课堂改为竖屏上下布局：字幕、PPT、控制栏依次排列。
- 课堂记录抽屉手机端接近全屏。
- 学生视频页上传区改为纵向全宽，视频播放器保持 16:9。
- 学习社区介绍、筛选栏、帖子卡片、评论输入和弹窗完成移动端适配。
- 表格、分页、Markdown 表格增加窄屏横向滚动保护。
- 增加 `viewport-fit=cover` 支持刘海屏安全区域。

主要修改：

- `D:\Hearing-impairedLearningPlatform\frontend\src\components\MobileBottomNav.vue`
- `D:\Hearing-impairedLearningPlatform\frontend\src\App.vue`
- `D:\Hearing-impairedLearningPlatform\frontend\index.html`
- `D:\Hearing-impairedLearningPlatform\frontend\src\components\AppHeader.vue`
- `D:\Hearing-impairedLearningPlatform\frontend\src\styles\main.css`
- `D:\Hearing-impairedLearningPlatform\frontend\src\views\HomeView.vue`
- `D:\Hearing-impairedLearningPlatform\frontend\src\views\StudentVideosView.vue`

`npm run build` 已通过，当前改动尚未提交。

---

## Turn 8：学生端上传视频生成字幕，背景不需要。直接透明。不然会看不见字幕

### 我的 Prompt

学生端上传视频生成字幕，背景不需要。直接透明。不然会看不见字幕

### codex 回复

已修复学生视频字幕不可见问题。

原因：

```text
浅色主题下字幕文字是深色
字幕背景也是深色
导致文字和背景接近同色
```

现在改为：

- 字幕背景完全透明
- 字幕文字改为白色
- 增加多层黑色文字阴影和描边效果
- 视频画面亮或暗时字幕都能保持清晰
- 不遮挡视频内容

生产 CSS 已验证：

```css
.video-player::cue {
  color: #fff;
  background: transparent;
  background-color: transparent;
  text-shadow: 0 1px 2px rgba(0, 0, 0, .98), ...;
}
```

前端构建通过。

提交：

```text
497ce90 fix: make video captions transparent
```

当前提交在 `main` 分支，未部署服务器。

---

## Turn 9：你觉得可以怎么改进两个核心功能：

### 我的 Prompt

你觉得可以怎么改进两个核心功能：
第一个：实时课堂字幕：依赖网络和浏览器版本问题
第二个：数字人依赖第三方并发限制和网络问题

### codex 回复

这两个问题的本质相同：**核心功能分别依赖浏览器语音服务和第三方单账号 WebGL 会话，都是不可控的单点。**

改进方向不是继续加异常提示，而是把它们改成“**主通道 + 降级通道 + 服务抽象**”。

## 零、先解决共同基础：HTTPS 与 WSS

两个功能都受安全上下文影响：

- Web Speech API 需要麦克风权限，公网 HTTP 基本不可用
- 手语数字人在公网 HTTP 下已经验证会初始化失败，HTTPS/localhost 下正常

所以正式环境第一步必须是：

```text
HTTPS + WSS
```

这是两个核心功能上线的前置条件。

---

## 一、实时课堂字幕

### 当前方案的问题

教师浏览器使用 Web Speech API：

- 依赖 Chrome/Edge 版本
- 依赖浏览器内置语音服务网络
- 识别结果和延迟不可控
- 浏览器更新可能改变行为
- 公网 HTTP 无法使用麦克风

### 推荐架构

```text
教师麦克风
  -> 浏览器 AudioWorklet 采集音频
  -> WebSocket 发送 16kHz PCM
  -> 服务端流式 ASR
  -> 临时字幕和最终字幕
  -> 现有课堂 WebSocket 广播
```

使用国内云厂商的**流式 ASR**，例如阿里云、腾讯云或讯飞，而不是把几秒音频切块后整段识别。重点是：

- 支持 partial/final 结果
- 端到端延迟控制在 1–2 秒
- 支持中文标点和数字
- 支持课程热词
- 支持 VAD，避免静音时持续计费

### 三层降级

```text
1. 云端流式 ASR
2. 浏览器 Web Speech API
3. 演示字幕或教师键盘快速输入
```

页面明确显示当前模式：

```text
云端高精度
浏览器识别
演示模式
```

### 音频优化

- 使用教师本机麦克风
- 开启降噪、回声消除和自动增益
- 服务端保存最终字幕，临时结果不持久化
- 课程 PPT 文本自动生成热词
- 支持教师手动修正最终字幕

### 为什么不建议直接自建 Whisper

自建 Whisper 可以摆脱第三方服务，但需要 GPU、模型运维、并发队列和较高的工程成本。当前项目更适合先接入成熟流式 ASR API，再考虑本地部署。

---

## 二、手语数字人

当前最大的问题是第三方账号的并发席位，而不是前端代码。

### 方案一：集中渲染，学生只接收视频流

这是解决单账号并发限制最有效的方案：

```text
教师最终字幕
  -> 手语渲染服务
  -> 一个 SDK 会话生成数字人视频
  -> WebRTC 或 HLS 分发给多个学生
```

服务端只占用一个或少量数字人会话，学生端不直接初始化 SDK。

优点：

- 不再受学生人数并发限制
- 学生设备不需要 WebGL
- 移动端性能明显更好
- 模型资源由服务器加载

限制：

- 需要一台有 GPU 或高性能 WebGL 的渲染节点
- 延迟比本地 SDK 稍高
- 需要确认厂商许可证允许集中渲染和转发

如果厂商允许这种使用方式，这会是当前项目最合适的正式方案。

### 方案二：购买多席位账号池

如果每个学生必须独立操作数字人：

- 向厂商购买多并发席位
- 后端维护账号池
- 使用 Redis 租约分配账号
- 学生进入课堂时申请一个数字人席位
- 离开页面后立即释放
- 没有空闲席位时显示降级状态

不要让前端自行选择一个账号连接，否则并发冲突很难控制。

### 方案三：录播视频预生成并缓存

学生视频学习不需要每个学生实时调用 SDK。

可以按视频字幕预生成手语动画视频，然后缓存：

```text
视频转写
  -> 按句切分
  -> 批量生成手语片段
  -> 缓存片段
  -> 学生播放时直接播放缓存
```

优点：

- 完全规避实时并发
- 同一个视频所有学生共享缓存
- 网络失败时仍可播放
- 移动端压力最小

缺点是首次生成需要时间，因此更适合视频学习，不适合教师实时讲课。

### 方案四：手语词元库与本地动画

长期来看，可以建立自己的手语词元库：

- 常用词和课程术语对应手语片段
- 文本先分词，再映射动作片段
- 使用 Three.js、VRM 或预录视频播放
- 无法表达的句子降级为关键词手语和字幕

这种方式不完全依赖单一 SDK，但建设成本较高，不建议当前阶段直接自研完整手语生成模型。

---

## 三、具体优先级建议

### P0：现在就应该做

1. 配置域名和 HTTPS/WSS
2. 数字人失败时不影响字幕和课堂主流程
3. 增加“只听字幕”模式
4. 数字人隐藏或离开页面时释放会话
5. 单个学生只保留一个数字人实例

### P1：下一步做

1. 接入国内流式 ASR
2. 保留 Web Speech 和演示字幕作为降级
3. 增加课程热词
4. 为数字人增加账号租约或席位队列
5. 学生视频优先使用预生成手语片段

### P2：产品成熟后

1. 集中式数字人渲染与视频分发
2. 自建手语词元库
3. 视频手语片段缓存
4. 教师字幕修正与导出
5. 多厂商 ASR/数字人适配层

---

## 我的建议

如果只能选择两个最有效的改造：

1. **实时字幕改成服务端流式 ASR，保留 Web Speech 作为备用。**
2. **数字人改成“一个服务端实例渲染、多个学生观看视频流”，或购买多席位账号池。**

这两项能把系统从“依赖某个浏览器和某个账号的演示项目”，提升为真正可稳定运行的课堂平台。

---

## Turn 10：FunASR

### 我的 Prompt

FunASR

### codex 回复

**FunASR 很适合替代 Web Speech API 做服务端实时字幕**，而且比浏览器语音识别更可控。

不过要注意：当前项目视频上传用的 SenseVoice 是整段识别模型，不适合直接拿来做实时字幕。实时场景应使用 FunASR 的流式 Paraformer。

## 推荐组合

```text
FunASR 流式 Paraformer
+ FSMN-VAD
+ CT-Punc 标点模型
```

对应流程：

```text
教师麦克风
  -> 浏览器采集 16kHz 单声道 PCM
  -> WebSocket 发送到 FunASR 服务
  -> 输出临时字幕 partial
  -> 输出最终字幕 final
  -> Spring Boot 保存 final
  -> 现有课堂 WebSocket 广播给学生
```

## 优点

- 不依赖 Chrome/Edge 自带语音服务
- 不依赖浏览器版本，只依赖浏览器麦克风采集能力
- 支持流式返回，字幕延迟可控
- 支持 VAD，自动识别一句话开始和结束
- 支持中文标点
- 可以配置课程专业术语和热词
- 模型部署在自己的服务器上，数据不离开自己的系统
- 可以给字幕增加词级或句级时间信息

## 关键限制

### 1. 当前 2 核 2G 服务器可能不够

FunASR 即使使用 CPU 模式，也需要：

- Python 运行环境
- 模型驻留内存
- 音频解码和推理资源

当前服务器还同时运行：

- Spring Boot
- MySQL 8
- LibreOffice
- FFmpeg
- Nginx

2G 内存下同时跑 FunASR 风险较高，可能出现 OOM。

建议：

```text
最低：4 核 8G，CPU 模式，单教师并发
推荐：8 核 16G 或独立 GPU 服务
```

更合理的是把 FunASR 部署成**独立 ASR 服务**，不要让 Java、MySQL 和 ASR 抢同一台 2G 机器。主应用可以通过内网地址调用：

```text
http://127.0.0.1:10095
```

### 2. 仍然需要 HTTPS

FunASR 替代的是语音识别服务，不是浏览器麦克风权限。

教师端页面仍然必须通过：

```text
https://域名
```

才能正常获得麦克风权限。

### 3. 需要处理临时字幕和最终字幕

建议：

- `partial` 字幕只在教师端和课堂实时显示
- `final` 字幕才写入 `caption` 表
- 防止同一句话被重复保存
- 学生看到最终字幕后再驱动手语数字人

### 4. 需要增加断线恢复

建议教师浏览器：

- 暂存音频片段
- ASR WebSocket 断开后自动重连
- 网络恢复后补发未确认片段
- FunASR 不可用时自动切换 Web Speech 或演示字幕

## 推荐部署方式

```text
2C2G ECS
├─ Spring Boot
├─ Nginx
├─ MySQL
├─ LibreOffice
└─ FFmpeg

独立 FunASR 服务
├─ Python / Docker
├─ FunASR Runtime
├─ Paraformer 流式模型
├─ FSMN-VAD
└─ CT-Punc
```

也可以在一台更大的服务器上部署：

```text
4C8G 或 8C16G ECS
├─ FunASR CPU Runtime
└─ 现有应用
```

但不建议继续挤在当前 2C2G 配置里。

## 我的建议

**实时字幕优先采用 FunASR 流式 Paraformer，Web Speech 保留为备用。**

推荐最终形成三层模式：

```text
1. FunASR 服务端流式识别：默认、稳定、可控
2. Web Speech API：FunASR 不可用时的备用
3. 演示字幕/教师键盘输入：最终兜底
```

这样既保留了现有功能，又能逐步摆脱对浏览器语音服务的依赖。

---
