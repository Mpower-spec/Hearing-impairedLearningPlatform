# 声学无碍学习平台（Demo MVP）

面向听障学生的课堂学习平台 Demo：教师上传 PPTX 并开课，学生通过房间码加入，实时同步幻灯片、字幕和弹幕；课后学生可自助生成学习资料，也可上传视频生成字幕与笔记。



**上传视频字幕**：FFmpeg 提取 MP3 后，调用 **SiliconFlow（硅基流动）** 的 `FunAudioLLM/SenseVoiceSmall` ASR 模型，再生成估算时间轴 VTT。

**实时字幕**：使用浏览器的 **Web Speech API**（Chrome/Edge），由教师端浏览器识别语音，再通过 WebSocket 广播给学生。

视频笔记和课堂资料使用 **SiliconFlow** 的 `Qwen/Qwen2.5-72B-Instruct`。





## 技术栈

- 后端：Java 17（使用 JDK 21 编译）、Spring Boot 3.3、Spring WebSocket、MyBatis-Plus、MySQL 8
- 前端：Vue 3、Vite、JavaScript、Element Plus、Axios、vue-pdf-embed
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

- JDK 17+、Maven 3.9+
- Node.js 18+
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

### AI 模式

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

## 重要限制

- PowerPoint 仅支持 `.pptx`；不还原动画、内嵌视频和复杂版式。
- 视频仅支持浏览器可播放的 MP4/WebM，不做转码或 HLS。
- SiliconFlow 语音转写接口只返回整段文本，因此 VTT 时间轴按句长和视频时长估算，界面会显示“估算时间轴”提示。
- Web Speech API 依赖 Chrome/Edge 和麦克风权限，部分环境还需要联网。
- 本项目是本地比赛 Demo：文件 URL 使用不可猜测 UUID，但 `/files/**` 不做额外鉴权，不适合直接部署公网。
- 不支持注册、管理后台、多实例、Redis、对象存储和负载均衡。

## 构建与测试

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

## 常见问题

### PPT 转换失败

确认 `LIBREOFFICE_PATH` 指向 `soffice.com`（不是 `soffice.exe`），并确认文件存在。转换服务使用独立的 LibreOffice 用户配置目录，避免进程锁。

### 视频处理失败

确认 `FFMPEG_PATH` 和 `FFPROBE_PATH` 正确；运行 `ffmpeg -version` 和 `ffprobe -version` 验证。视频必须是浏览器可直接播放的格式。

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