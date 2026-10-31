# AI 课程问答与课后学习资料 · 原生 Prompt 链


---

## Turn 1：现在要求学生端也可以实现课后生成本节课的学习资料，不需要老师发。你可以删掉这个功能。

### 我的 Prompt

现在要求学生端也可以实现课后生成本节课的学习资料，不需要老师发。你可以删掉这个功能。

### codex 回复

已修改为学生端自助生成课后学习资料。

## 当前行为

- 教师端已删除“生成课堂学习资料”按钮和资料弹窗
- 课堂结束后，学生端自动检查本节课资料是否存在：
  - 不存在：显示“生成本节课学习资料”
  - 已存在：显示“查看课堂学习资料”和“重新生成学习资料”
- 学生生成后立即弹窗展示，并保存到数据库供其他同学查看
- 只有加入过该课程、且课堂已经结束的学生才能生成
- AI 输入使用已结束课堂的字幕和 PPT 文字

## 已验证

- 学生身份真实调用 AI 生成成功，返回来源为 `AI`
- 学生可生成并再次读取同一份资料
- 教师端生成按钮数量为 `0`
- 学生端查看和重新生成按钮正常显示
- 页面仍保持一屏无滚动
- 后端已重启并加载新逻辑，前端已热更新

刷新当前页面即可使用。

---

## Turn 2：好的，你觉得现在还可以新增哪些功能。并给出可行的实现方案

### 我的 Prompt

好的，你觉得现在还可以新增哪些功能。并给出可行的实现方案

### codex 回复

<proposed_plan>
# 实时课堂课程 AI 问答助手实施方案

## 摘要

- 在学生实时课堂页新增课程 AI 问答抽屉，自动绑定当前房间和课程。
- 仅学生可使用；加入过该课堂的学生才能提问。
- 检索来源限定为：课程 PPT 文本、当前课堂字幕、当前课程已生成的课堂学习资料。
- 使用现有 SiliconFlow LLM，不新增向量数据库或聊天记录表。
- 问答历史只保留在当前页面内，关闭或刷新后清空。

## 后端实现

- 新增 `CourseQaService`，负责权限校验、资料分块、关键词检索、Prompt 构造和 AI 降级。
- 新增问答接口：

```text
POST /api/course-qa/ask
```

请求：

```json
{
  "roomId": 27,
  "question": "光合作用需要哪些条件？"
}
```

响应：

```json
{
  "answer": "根据课堂内容，光合作用需要阳光、水和二氧化碳。[S1]",
  "source": "AI",
  "sources": [
    {
      "type": "CAPTION",
      "label": "课堂字幕",
      "excerpt": "植物需要阳光、水和二氧化碳。"
    }
  ]
}
```

- 权限：
  - 根据 `roomId` 调用现有房间权限校验。
  - 学生必须是该房间成员。
  - 仅支持 `STUDENT` 角色。
- 资料来源：
  - PPT：使用 `lesson.ppt_text`。
  - 字幕：使用当前房间 `caption` 记录，并保留时间标签。
  - 资料：仅使用学生有权查看的 `CLASS_MATERIAL`，不使用教师专属备课资料。
- 检索策略：
  - 将内容按段落和约 500 字切块。
  - 从问题中提取中文双字词、英文单词并过滤常见停用词。
  - 按关键词命中次数和短语完整匹配度评分。
  - 最多选择 5 个相关片段，总上下文不超过约 6000 字。
  - MVP 不使用 Embedding、向量数据库或 Elasticsearch。
- Prompt 约束：
  - 只根据提供的课程资料回答。
  - 资料不足时明确说明，不编造。
  - 使用简洁中文，并通过 `[S1]`、`[S2]` 标注来源。
- AI 模式：
  - `real`：失败直接报错。
  - `auto`：失败时返回明确标记为 `MOCK` 的本地兜底回答。
  - `mock`：直接返回基于检索片段的演示回答。
- 无资料时返回 400，并提示“当前课程暂无可用于问答的资料”。
- 不新增数据库表，不保存请求、回答或来源记录。

## 前端实现

- 在 `StudentClassroomView.vue` 顶部操作区增加“课程问答”按钮。
- 使用 Element Plus 抽屉：
  - 桌面宽度约 420px。
  - 手机端接近全屏。
- 抽屉内包含：
  - 当前课程标题和资料范围说明。
  - 用户问题、AI 回答的气泡列表。
  - AI/MOCK 来源标签。
  - 可折叠的来源片段列表。
  - 多行问题输入框和发送按钮。
- 支持：
  - Enter 发送、Shift+Enter 换行。
  - 请求中禁用重复提交。
  - 请求失败保留用户问题并显示重试按钮。
  - 关闭抽屉后保留当前页面内的对话，离开课堂页后清空。
- 未加载到 `lessonId` 时按钮禁用。
- 手机端输入区域适配软键盘和底部安全区。

## 测试计划

- 验证未加入课堂的学生无法调用接口。
- 验证空问题、超长问题和无效房间返回明确 400/403/404。
- 验证 PPT、课堂字幕和课堂资料都能被检索并返回来源。
- 验证无相关资料时不会编造答案。
- 验证 `AI_MODE=mock` 和错误 Key 场景下返回明确标记的模拟回答。
- 验证课程 A 的学生不能通过课程 B 的 roomId 获取资料。
- 验证前端发送、加载、错误、重试、来源展开和关闭重开流程。
- 验证手机端抽屉、输入框和软键盘布局。
- 运行 `mvn test`、`npm run build`，并执行一次真实课程问答接口冒烟测试。
- 功能完成后更新 `README.md`，单独提交到 `test001`，不执行 push。

## 假设

- 只支持学生使用，不做教师端问答。
- 问答入口仅在实时课堂页，不加入视频页。
- `lesson.ppt_text` 没有可靠的分页映射，因此 PPT 来源统一标记为“PPT 内容”，不虚构页码。
- 当前聊天不持久化，不会出现在课堂记录或课后资料中。
- 本版本只做关键词检索，不引入 Embedding 模型、向量库或全文检索中间件。
</proposed_plan>

---

## Turn 3：PLEASE IMPLEMENT THIS PLAN:

### 我的 Prompt

PLEASE IMPLEMENT THIS PLAN:
# 实时课堂课程 AI 问答助手实施方案

## 摘要

- 在学生实时课堂页新增课程 AI 问答抽屉，自动绑定当前房间和课程。
- 仅学生可使用；加入过该课堂的学生才能提问。
- 检索来源限定为：课程 PPT 文本、当前课堂字幕、当前课程已生成的课堂学习资料。
- 使用现有 SiliconFlow LLM，不新增向量数据库或聊天记录表。
- 问答历史只保留在当前页面内，关闭或刷新后清空。

## 后端实现

- 新增 `CourseQaService`，负责权限校验、资料分块、关键词检索、Prompt 构造和 AI 降级。
- 新增问答接口：

```text
POST /api/course-qa/ask
```

请求：

```json
{
  "roomId": 27,
  "question": "光合作用需要哪些条件？"
}
```

响应：

```json
{
  "answer": "根据课堂内容，光合作用需要阳光、水和二氧化碳。[S1]",
  "source": "AI",
  "sources": [
    {
      "type": "CAPTION",
      "label": "课堂字幕",
      "excerpt": "植物需要阳光、水和二氧化碳。"
    }
  ]
}
```

- 权限：
  - 根据 `roomId` 调用现有房间权限校验。
  - 学生必须是该房间成员。
  - 仅支持 `STUDENT` 角色。
- 资料来源：
  - PPT：使用 `lesson.ppt_text`。
  - 字幕：使用当前房间 `caption` 记录，并保留时间标签。
  - 资料：仅使用学生有权查看的 `CLASS_MATERIAL`，不使用教师专属备课资料。
- 检索策略：
  - 将内容按段落和约 500 字切块。
  - 从问题中提取中文双字词、英文单词并过滤常见停用词。
  - 按关键词命中次数和短语完整匹配度评分。
  - 最多选择 5 个相关片段，总上下文不超过约 6000 字。
  - MVP 不使用 Embedding、向量数据库或 Elasticsearch。
- Prompt 约束：
  - 只根据提供的课程资料回答。
  - 资料不足时明确说明，不编造。
  - 使用简洁中文，并通过 `[S1]`、`[S2]` 标注来源。
- AI 模式：
  - `real`：失败直接报错。
  - `auto`：失败时返回明确标记为 `MOCK` 的本地兜底回答。
  - `mock`：直接返回基于检索片段的演示回答。
- 无资料时返回 400，并提示“当前课程暂无可用于问答的资料”。
- 不新增数据库表，不保存请求、回答或来源记录。

## 前端实现

- 在 `StudentClassroomView.vue` 顶部操作区增加“课程问答”按钮。
- 使用 Element Plus 抽屉：
  - 桌面宽度约 420px。
  - 手机端接近全屏。
- 抽屉内包含：
  - 当前课程标题和资料范围说明。
  - 用户问题、AI 回答的气泡列表。
  - AI/MOCK 来源标签。
  - 可折叠的来源片段列表。
  - 多行问题输入框和发送按钮。
- 支持：
  - Enter 发送、Shift+Enter 换行。
  - 请求中禁用重复提交。
  - 请求失败保留用户问题并显示重试按钮。
  - 关闭抽屉后保留当前页面内的对话，离开课堂页后清空。
- 未加载到 `lessonId` 时按钮禁用。
- 手机端输入区域适配软键盘和底部安全区。

## 测试计划

- 验证未加入课堂的学生无法调用接口。
- 验证空问题、超长问题和无效房间返回明确 400/403/404。
- 验证 PPT、课堂字幕和课堂资料都能被检索并返回来源。
- 验证无相关资料时不会编造答案。
- 验证 `AI_MODE=mock` 和错误 Key 场景下返回明确标记的模拟回答。
- 验证课程 A 的学生不能通过课程 B 的 roomId 获取资料。
- 验证前端发送、加载、错误、重试、来源展开和关闭重开流程。
- 验证手机端抽屉、输入框和软键盘布局。
- 运行 `mvn test`、`npm run build`，并执行一次真实课程问答接口冒烟测试。
- 功能完成后更新 `README.md`，单独提交到 `test001`，不执行 push。

## 假设

- 只支持学生使用，不做教师端问答。
- 问答入口仅在实时课堂页，不加入视频页。
- `lesson.ppt_text` 没有可靠的分页映射，因此 PPT 来源统一标记为“PPT 内容”，不虚构页码。
- 当前聊天不持久化，不会出现在课堂记录或课后资料中。
- 本版本只做关键词检索，不引入 Embedding 模型、向量库或全文检索中间件。

### codex 回复

课程 AI 问答助手已完成并单独提交。

### 已实现

- 学生课堂页新增“课程问答”入口和抽屉。
- 自动绑定当前课堂与课程。
- 仅课堂成员中的学生可以提问。
- 检索来源：
  - 课程 PPT 文本
  - 当前课堂字幕
  - 课堂学习资料
- 中文关键词和双字词检索，不使用向量数据库。
- 最多选择 5 个相关片段，结合 SiliconFlow LLM 回答。
- 回答附带 `[S1]`、`[S2]` 引用，并展示来源片段。
- 支持 `AI`、`MOCK`、`LOCAL` 三种来源状态。
- `AI_MODE=mock` 时返回明确标记的模拟回答。
- 问答仅在当前页面内保留，不写入数据库。
- 支持 Enter 发送、Shift+Enter 换行、失败重试和加载状态。
- 手机端抽屉、输入框和安全区已适配。

### 验证结果

- 后端测试：12 个全部通过。
- 新增课程问答检索单元测试：关键词提取、相关度评分。
- 前端 `npm run build` 通过。
- 临时测试后端已关闭。
- 当前数据库没有课程数据，因此未完成真实课程接口冒烟测试。

### Git 状态

```text
f8b2e98 feat: add course AI Q&A assistant
test001 比 origin/test001 领先 2 个提交
工作区干净
未 push
```

README 已同步更新课程问答接口、功能说明和限制。

---

## Turn 4：Files pasted by the user:

### 我的 Prompt

# Files pasted by the user:

## "[vite] connecting... [vite] connected. [SignAvatar] SDK 脚本已加载，开始初始化数字人 [SignAva…": D:\Codex\.codex/attachments/9a560d99-f177-4b82-81db-2a3bb0bb4eed/Pasted text.txt

Pasted text contains the user's request.

## My request:

### codex 回复

## Handoff Summary

### Current Task
Debug the problems shown in the attached browser console log:

`D:\Codex\.codex\attachments\9a560d99-f177-4b82-81db-2a3bb0bb4eed\Pasted text.txt`

The user only supplied the log. No new feature request was stated. Relevant errors include:

1. `ElCarousel` warns that `indicatorPosition="inside"` is invalid.
2. `HomeView` renders `teacher` but the property is not defined.
3. Dynamic human SDK shows its UI loaded, but action service times out after 45 seconds.
4. `POST /api/course-qa/ask` returns HTTP 500 twice.
5. The log contains approximately 890K tokens and includes a huge PDF.js blob; avoid treating base64 content as language input.

### Repository State
- Workspace: `D:\Hearing-impairedLearningPlatform`
- Branch: `test001`
- Latest commit: `f8b2e98 feat: add course AI Q&A assistant`
- `test001` is 2 commits ahead of `origin/test001`.
- Do not push unless explicitly requested.
- Current code changes from the interrupted log-debugging pass may exist. Re-run `git status --short` before editing.
- User requires:
  - Every completed feature gets its own commit, but no push.
  - Every technical/feature change updates `README.md`.
  - Respond in Chinese.
  - Do not commit secrets: `frontend/.env.local`, `backend/config/local.properties`, `backend/logs/`.

### Ideas Already Explored
The attached log was read. A huge `rg` output was truncated and did not reveal the backend 500 stack trace. Need targeted extraction:

```powershell
Select-String -Path 'D:\Codex\.codex\attachments\9a560d99-f177-4b82-81db-2a3bb0bb4eed\Pasted text.txt' -Pattern 'course-qa|CourseQa|Unexpected server error|NullPointerException|SQLException' | Select-Object -First 80
```

### Immediate Known Fixes
#### 1. Carousel warning
`frontend/src/views/HomeView.vue` currently contains:

```vue
indicator-position="inside"
```

Element Plus supports `""`, `"none"`, or `"outside"`. Change to `indicator-position="none"` or remove the prop. Preserve current compact layout.

#### 2. Undefined `teacher` in Home template
`HomeView.vue` currently has role-conditional objects, but the third carousel visual still references `teacher` directly:

```vue
<span class="material-mark">{{ teacher ? 'AI' : '学' }}</span>
<span>{{ teacher ? 'AI 备课中心' : '视频学习助手' }}</span>
<h3>{{ teacher ? '教学目标' : '视频字幕' }}</h3>
...
```

`teacher` is not exposed in setup, so Vue warns repeatedly. Fix by either:

- Add:
  ```js
  const teacher = computed(() => role.value === 'TEACHER')
  ```
  or
- Replace template expressions with `role === 'TEACHER'`, but `role` is a computed ref and template unwrapping should work. Recommended: add `teacher` computed and use it.

#### 3. Course QA 500
Relevant files:

```text
D:\Hearing-impairedLearningPlatform\backend\src\main\java\com\hearing\platform\service\CourseQaService.java
D:\Hearing-impairedLearningPlatform\backend\src\main\java\com\hearing\platform\service\MaterialService.java
```

`CourseQaService.ask()` calls:

```java
RoomDto room = roomService.getForUser(request.roomId(), user);
Lesson lesson = lessonService.get(room.lessonId());
List<CaptionDto> captions = roomService.captions(room.id(), user);
MaterialDto classMaterial = materialService.findClassMaterialForQa(room.lessonId(), user);
```

`MaterialService.findClassMaterialForQa()` currently performs student membership validation:

```java
} else if (!roomService.isMemberOfLesson(lessonId, user.id())) {
    throw BusinessException.forbidden("加入过该课堂的学生才能使用课程问答");
}
Material material = materialMapper.selectOne(...);
return material == null ? null : MaterialDto.from(material);
```

Potential causes to verify:

- Circular dependency between `CourseQaService` and `RoomService`/`MaterialService`.
- `roomService.captions()` or `getForUser()` rejects current student.
- `lessonService.get()` behavior for students.
- NPE in `CourseChunk` or source handling.
- Backend stack trace is needed before guessing.
- The test database has no lessons/rooms when last smoke-tested, so reproduce with the user’s live database, or create temporary test data carefully and clean up.

Also inspect the log directly around the final lines before the two `500` entries. The console log may omit backend stack trace; check running backend console or `backend/logs/hearing-platform-error.log`.

#### 4. Digital human timeout
Current `SignAvatarWidget.vue` logs:

```text
[SignAvatar] SDK 脚本已加载，开始初始化数字人
[SignAvatar] 数字人界面资源已加载，等待动作服务就绪
```

then times out after 45 seconds. Earlier diagnosis:

- `avatar.gbqr.net`, `api.gbqr.net`, SignalR and CORS were reachable.
- Only one SDK account is available, and concurrent sessions may be limited.
- Teacher no longer initializes the avatar; only students do.
- Do not create extra avatar instances.
- The avatar is globally mounted once in `App.vue`.
- If action service still times out, likely vendor account concurrency, WebGL, model resource, or SDK action-service issue. The 45-second timeout is already diagnostic.
- Do not “fix” by creating page-level avatars.

### Current Architecture Constraints
- Per browser: exactly one global `SignAvatarWidget`, mounted in `frontend/src/App.vue`.
- Global controller/store: `frontend/src/avatar/avatarStore.js`.
- Student-only avatar and controls.
- Teacher only performs Web Speech recognition and broadcasts final captions.
- Student classroom receives WebSocket `caption` and calls `pushSignCaption`.
- Student video page uses VTT to drive the same global avatar.
- Course QA is student-only, classroom-only, no persistence, keyword retrieval over PPT + captions + class material.

### Course QA Feature Already Implemented and Committed
Commit `f8b2e98` includes:

- `CourseQaController`, DTOs, `CourseQaService`, `CourseQaServiceTest`.
- `PromptTemplates.courseQa`.
- `MockContent.courseQa`.
- Student classroom Q&A drawer.
- README section.
- 12 backend tests passed; frontend build passed.
- Real smoke test could not complete because no lesson data existed.

### Recommended Next Steps
1. Run `git status --short` and inspect only relevant source files.
2. Fix carousel prop and undefined `teacher`.
3. Extract backend 500 stack trace from:
   - attachment near final `course-qa` lines,
   - `backend/logs/hearing-platform-error.log`,
   - live backend console.
4. Add a focused backend unit test or integration reproduction for the failing Q&A call.
5. Run:
   ```powershell
   cd D:\Hearing-impairedLearningPlatform\backend
   mvn "-Duser.language=en" "-Duser.country=US" test
   cd ..\frontend
   npm run build
   ```
6. Update `README.md` if behavior/config changes.
7. Commit this bug-fix work separately on `test001`; do not push.

### Important Tooling Notes
- In this environment, some `exec_command` calls fail if `shell` is `"bash"` or if multiple command strings are used incorrectly. Prefer `shell: "powershell"` and single PowerShell commands/scripts.
- Avoid dumping huge log files. Use `Select-String` with line numbers and bounded context.
- The attachment is ~2.5 MB / ~890K tokens and includes base64 PDF.js content. Never read it raw again.

---

## Turn 5：学生端课程问答，不用md,形式。会出现md的特殊语法，而浏览器端不识别，改用txt

### 我的 Prompt

学生端课程问答，不用md,形式。会出现md的特殊语法，而浏览器端不识别，改用txt

### codex 回复

学生端课程问答已改为纯文本输出，其他备课资料、课堂资料和视频笔记的 Markdown 格式不受影响。

改动内容：
- 课程问答使用独立的纯文本系统提示词，禁止输出标题、加粗、代码块、表格和链接等 Markdown 语法。
- 后端增加兜底清洗：即使模型仍返回 `#`、`**`、代码块、列表或链接语法，也会转换为普通文本。
- 保留 `[S1]`、`[S2]` 引用标记，来源展示功能不受影响。
- 增加对应单元测试。

涉及文件：
- `D:\Hearing-impairedLearningPlatform\backend\src\main\java\com\hearing\platform\ai\PromptTemplates.java`
- `D:\Hearing-impairedLearningPlatform\backend\src\main\java\com\hearing\platform\service\CourseQaService.java`
- `D:\Hearing-impairedLearningPlatform\backend\src\test\java\com\hearing\platform\ai\PromptTemplatesTest.java`
- `D:\Hearing-impairedLearningPlatform\backend\src\test\java\com\hearing\platform\service\CourseQaServiceTest.java`

已运行完整后端测试：`mvn -q test`，全部通过。

---
