# 声学无碍学习平台（Demo MVP）

面向听障学生的课堂学习平台 Demo：教师上传 PPTX 并开课，学生通过房间码加入，实时同步幻灯片、字幕和弹幕；课后学生可自助生成学习资料，也可上传视频生成字幕、学习笔记和手语表达。平台还包含教师与学生共同参与的文字学习社区，并支持学生手机端底部导航和响应式布局。



**上传视频字幕**：FFmpeg 提取 MP3 后，调用 **SiliconFlow（硅基流动）** 的 `FunAudioLLM/SenseVoiceSmall` ASR 模型，再生成估算时间轴 VTT。

**实时字幕**：使用浏览器的 **Web Speech API**（Chrome/Edge），由教师端浏览器识别语音，再通过 WebSocket 广播给学生。

视频笔记和课堂资料使用 **SiliconFlow** 的 `Qwen/Qwen2.5-72B-Instruct`。





## 主要功能

- 教师端：PPTX 上传、LibreOffice 转 PDF、房间创建、页码同步、实时语音字幕、弹幕、课堂结束和学习资料生成。
- 学生端：房间码加入、同步 PPT、大字号实时字幕、弹幕、课堂资料、视频上传、视频转写、VTT 字幕、AI 学习笔记。
- 学习社区：教师和学生均可发帖、评论、分类筛选、关键词搜索和删除自己发布的内容。
- 手语数字人：学生端使用唯一全局数字人实例，课堂最终字幕和视频 VTT 字幕可驱动手语表达，支持重播、隐藏和拖动。
- 课程 AI 问答：学生可在实时课堂内基于 PPT、课堂字幕和课堂学习资料提问，回答附带引用来源并支持 AI 模拟降级。
- 移动端：学生手机浏览器提供首页、课堂、视频、社区底部导航，并适配竖屏课堂、全宽视频、表单和弹窗。
- 后端：Java 17（使用 JDK 21 编译）、Spring Boot 3.3、Spring WebSocket、MyBatis-Plus、MySQL 8
- 前端：Vue 3、Vite、JavaScript、Element Plus、Axios、vue-pdf-embed
- 数字人：`avatar.gbqr.net` 在线 SDK，浏览器 WebGL，单例全局挂载
- 媒体：LibreOffice 将 PPTX 转 PDF，FFmpeg/FFprobe 处理视频
- AI：SiliconFlow OpenAI 兼容接口
  - LLM 默认：`Qwen/Qwen2.5-72B-Instruct`
  - ASR 默认：`FunAudioLLM/SenseVoiceSmall`

## 目录

```text
backend/                  Spring Boot 服务
frontend/                 Vue 3 前端
tools/                    本地媒体工具（LibreOffice、FFmpeg/FFprobe）
uploads/                  运行时文件，启动后自动创建
```

### 后端分层结构

后端位于 `backend/src/main/java/com/hearing/platform/`，遵循 SSM 风格的分层与依赖方向：

```text
controller/        REST 接口，只负责参数接收、权限注解和统一响应
service/           业务规则、事务编排、文件与 AI 流程
dao/               MyBatis-Plus Mapper，负责数据库访问
entity/            数据库实体，与表结构一一对应
dto/               接口请求/响应模型
security/          JWT、登录上下文、角色校验
websocket/         课堂实时消息适配层
client/            SiliconFlow LLM / ASR 外部服务客户端
config/            配置属性、Bean 初始化与演示账号初始化
common/            统一响应、业务异常与全局异常处理
```

依赖方向保持为 `controller/websocket -> service -> dao -> entity`；Controller 不直接访问 Mapper，DAO 不依赖业务层。

## 前置条件

- JDK 17+、Maven 3.9
- Node.js 18
- MySQL 8，Windows 服务名默认为 `MySQL80`
- Chrome 或 Edge（Web Speech API）
- 本仓库已准备：
  - `tools/libreoffice/program/soffice.com`
  - `tools/node_modules/@ffmpeg-installer/win32-x64/ffmpeg.exe`
  - `tools/node_modules/@ffprobe-installer/win32-x64/ffprobe.exe`

如果在新机器上重新准备工具：

```powershell
npm install --prefix tools @ffmpeg-installer/ffmpeg @ffprobe-installer/ffprobe
# 下载 LibreOffice 官方 MSI 后，以管理安装模式解包：
msiexec /a LibreOffice_*.msi /qn TARGETDIR=D:\path\to\tools\libreoffice
```

## 配置

复制配置模板并填写本地数据库和 SiliconFlow Key：

```powershell
Copy-Item backend/config/local.properties.example backend/config/local.properties
```

需要填写：

```properties
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
SILICONFLOW_API_KEY=sk-你的硅基流动Key
LIBREOFFICE_PATH=D:/Hearing-impairedLearningPlatform/tools/libreoffice/program/soffice.com
FFMPEG_PATH=D:/Hearing-impairedLearningPlatform/tools/node_modules/@ffmpeg-installer/win32-x64/ffmpeg.exe
FFPROBE_PATH=D:/Hearing-impairedLearningPlatform/tools/node_modules/@ffprobe-installer/win32-x64/ffprobe.exe
```

`backend/config/local.properties` 已被 `.gitignore` 忽略，不应提交。

数据库名默认是 `hearing_platform`，连接串启用了 `createDatabaseIfNotExist=true`。账号需要具备创建数据库和表的权限。

### 手语数字人配置

在未跟踪的 `frontend/.env.local` 中配置数字人 SDK 账号：

```properties
VITE_SIGN_AVATAR_ACCOUNT=你的数字人SDK账号
```

该文件已被 `.gitignore` 忽略，不要提交到仓库。数字人 SDK 由浏览器直接访问 `avatar.gbqr.net` 和 `api.gbqr.net`，部署环境需要允许 HTTPS/WSS 及厂商 WebRTC/TURN 网络连接。
```properties
AI_MODE=auto   # real / auto / mock
```

- `real`：只调用真实接口，失败直接报错。
- `auto`：默认值，优先真实接口；失败时自动使用明确标记的模拟结果。
- `mock`：不联网，直接使用模拟备课资料、字幕和笔记。

## 启动

### 1. 后端

```powershell
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080`。首次启动会自动建表并创建演示账号。

### 2. 前端

```powershell
cd frontend
npm install
npm run dev
```

打开：`http://localhost:5173`。`npm run dev` 和 `npm start` 都会启动 Vite；不要直接用 Node 运行 `src/main.js` 或 `src/router/index.js`。

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 教师 | `teacher` | `123456` |
| 学生 | `student` | `123456` |

## 学习社区接口

```text
GET    /api/community/posts
POST   /api/community/posts
GET    /api/community/posts/{id}
DELETE /api/community/posts/{id}
POST   /api/community/posts/{id}/comments
DELETE /api/community/comments/{id}
```

社区数据保存在 `community_post` 和 `community_comment` 表。教师和学生均可访问，删除操作仅允许内容作者本人执行。
## 课程 AI 问答

学生加入课堂后，可在课堂页打开“课程问答”抽屉：

```text
POST /api/course-qa/ask
```

```json
{
  "roomId": 27,
  "question": "这节课的重点是什么？"
}
```

问答上下文仅包含当前课程的 PPT 文本、当前房间字幕和学生可访问的课堂学习资料。系统使用关键词切块检索后交给 SiliconFlow LLM，回答附带 `[S1]` 等来源标记；`AI_MODE=auto` 下调用失败会返回明确标记的模拟回答。

当前问答仅在课堂页面内保存，不写入数据库；刷新或离开页面后自动清空。

## 演示流程
1. 教师登录，上传 `.pptx`，等待转 PDF 完成。
2. 进入“备课”生成备课资料。
3. 在课程列表点击“开课”，获得 6 位房间码。
4. 学生登录，输入房间码加入课堂。
5. 教师翻页，学生端同步。
6. 教师开启语音字幕；若浏览器不支持，使用“发送下一句演示字幕”。
7. 学生发送弹幕，教师端实时显示。
8. 教师结束课堂。
9. 学生在课堂页自行生成本节课学习资料并查看。
10. 学生进入“视频学习”，上传 MP4/WebM，生成字幕、播放带字幕视频并生成笔记。
11. 学生播放带 VTT 字幕的视频时，学生端全局数字人根据当前字幕同步表达手语。
12. 教师和学生进入“学习社区”，发布学习讨论、提问或经验分享，并进行评论交流。
13. 在实时课堂点击“课程问答”，基于 PPT、字幕和课堂资料提问并查看引用来源。
14. 使用手机打开学生端，验证底部导航、竖屏课堂、全宽视频和社区表单。

## 重要限制

- PowerPoint 仅支持 `.pptx`；不还原动画、内嵌视频和复杂版式。
- 视频仅支持浏览器可播放的 MP4/WebM，不做转码或 HLS。
- SiliconFlow 语音转写接口只返回整段文本，因此 VTT 时间轴按句长和视频时长估算，界面会显示“估算时间轴”提示。
- Web Speech API 依赖 Chrome/Edge 和麦克风权限，部分环境还需要联网。
- 本项目是本地比赛 Demo：文件 URL 使用不可猜测 UUID，但 `/files/**` 不做额外鉴权，不适合直接部署公网。
- 不支持注册、管理后台、对象存储和负载均衡。
- 社区当前仅支持文字帖子、评论、分类、搜索和分页，不支持图片、附件、点赞、私信和内容审核后台。
- 手语数字人在每个浏览器内保持单例；当前只有一个 SDK 账号时，只保证一个学生演示设备同时加载数字人。
- 教师端不初始化数字人，只负责语音识别和最终字幕广播；学生端接收 WebSocket 字幕后再驱动数字人。
- 课程问答当前使用关键词检索，不使用 Embedding、向量数据库或全文检索中间件；不支持跨课程问答和聊天记录持久化。

## 构建与测试

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

## 最近修复

- 清理首页轮播与模板表达式告警，避免开发控制台被重复警告刷屏。
- 修复课堂记录抽屉时间函数未注册导致的渲染异常。
- 课程问答接口更新后必须重启后端，确保 Spring Boot 加载 `CourseQaController`；未登录或非课堂成员仍会被权限层拒绝。
- 手语数字人动作服务超时属于在线 SDK 会话或厂商服务问题，不影响字幕、视频、课堂和问答功能。
## 常见问题

### PPT 转换失败

确认 `LIBREOFFICE_PATH` 指向 `soffice.com`（不是 `soffice.exe`），并确认文件存在。转换服务使用独立的 LibreOffice 用户配置目录，避免进程锁。

### 视频处理失败

确认 `FFMPEG_PATH` 和 `FFPROBE_PATH` 正确；运行 `ffmpeg -version` 和 `ffprobe -version` 验证。视频必须是浏览器可直接播放的格式。

### 手语数字人一直显示“正在为你生成手语教学”

当前 SDK 账号只支持一个并发会话。请只保留一个学生端标签页，关闭旧的课堂或视频页面，等待约 30 秒后点击“重试”；同时确认浏览器可访问 `avatar.gbqr.net` 与 `api.gbqr.net`，且没有拦截 HTTPS/WSS。若界面资源已加载但超过 45 秒仍未就绪，通常是厂商动作服务或账号会话问题，不影响课堂主流程。
### 登录失败或数据库连接失败

检查 MySQL 服务、`DB_USERNAME`、`DB_PASSWORD` 和 `DB_URL`。启动日志会显示具体连接错误。

### AI 显示“模拟结果”

说明当前没有配置 Key、真实接口失败，或 `AI_MODE=mock`。检查 `SILICONFLOW_API_KEY` 和网络后，将 `AI_MODE` 设为 `real` 重新生成。
## 局域网访问

前后端已配置为监听所有网卡。启动后其他设备可通过本机局域网 IPv4 访问：

```text
http://<本机局域网IPv4>:5173
```

本机当前地址：

- Radmin VPN：`http://26.255.29.43:5173`（同学需加入同一 Radmin 虚拟网络）
- Wi-Fi 局域网：`http://10.157.172.135:5173`（同学需在同一 Wi-Fi 且路由器未开启客户端隔离）

`5173` 是前端入口，`8080` 仅为后端 API 和 WebSocket 服务。Windows 的 Public/Private 防火墙配置当前已关闭；若以后重新开启防火墙，需要放行 TCP `5173` 和 `8080`。
### 教师语音字幕

浏览器只允许在安全上下文（`localhost` 或 HTTPS）中使用麦克风。教师讲课的电脑应打开：

```text
http://localhost:5173
```

学生仍使用局域网地址访问。若浏览器语音识别服务连接失败，可点击“发送下一句演示字幕”完成课堂演示。
## 学生端移动适配

- 手机断点：`max-width: 767px`
- 平板断点：`768px–1119px`
- 桌面断点：`1120px` 以上
- 学生底部导航：首页、课堂、视频、社区
- 手机课堂采用竖屏上下布局：字幕、PPT、控制栏依次排列
- 底部导航使用 `env(safe-area-inset-bottom)` 适配手机手势安全区
- 移动端输入框字号不低于 16px，避免 iOS 自动放大
## README 维护约定

每次新增功能、修改接口、调整架构、改变配置或进行重要 UI/技术改动时，必须同步更新本 README。
