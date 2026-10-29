# 外部首页 Demo

这版把 `/` 改为公开外部首页，登录后仍进入原有的 `/home` 学习工作台。首屏右侧现在使用真实手语数字人实例，不再使用静态演示图。

## 文件位置

| 用途 | 文件 |
| --- | --- |
| 外部首页页面 | `frontend/src/views/PublicHomeView.vue` |
| 手语数字人组件 | `frontend/src/components/SignAvatarWidget.vue` |
| 数字人全局状态 | `frontend/src/avatar/avatarStore.js` |
| 路由与登录保护 | `frontend/src/router/index.js` |
| 数字人环境变量 | `frontend/.env.local` |
| 登录页返回入口 | `frontend/src/views/LoginView.vue` |
| 隐私说明与使用条款 | `frontend/src/views/LegalView.vue` |
| 404 页面 | `frontend/src/views/NotFoundView.vue` |
| 备用动态 GIF | `frontend/public/assets/homepage-demo.gif` |
| GIF 动画源文件 | `tools/homepage-gif-stage.html` |
| GIF 导出脚本 | `tools/build-homepage-gif.mjs` |

## 本地预览

```bash
cd frontend
npm run dev
```

打开 `http://127.0.0.1:5173/`。数字人需要 `VITE_SIGN_AVATAR_ACCOUNT`，当前本地值位于 `frontend/.env.local`。

右上角登录按钮通往 `/login`。如果本地已经保存登录状态，按钮会显示为“进入平台”。

## 首屏数字人

首屏数字人使用 `SignAvatarWidget.vue` 的 `embedded` 模式：

```vue
<SignAvatarWidget
  ref="avatarRef"
  embedded
  :avatar-size="4"
  :initial-text="avatarText"
  :sign-text="avatarText"
  :auto-play="true"
/>
```

当前手语文案位于 `PublicHomeView.vue`：

```js
const avatarText = '欢迎使用声学无碍平台，皆在解决听障学生在学习中遇到所有问题。'
```

数字人加载时显示连接状态，失败时显示错误原因和“重新连接”按钮。加载成功后会自动播放欢迎手语，也可以点击右上角“重播手语”。

## 单例与路由复用

手语 SDK 是全局单例。`SignAvatarWidget.vue` 会把运行时状态放到 `window.__hpSignAvatarRuntime`，因此：

- 外部首页使用嵌入式定位，数字人显示在首屏右侧。
- 登录后的学习页面继续使用同一个数字人实例。
- 学习页面使用原有固定悬浮模式，可拖动调整位置。
- 返回外部首页时，实例会重新回到首屏容器内，不重复创建 3D 场景。

## 页面结构

1. 悬浮导航与右上角登录
2. 首屏文案和手语数字人
3. 产品能力摘要
4. 三张能力卡片
5. 大字逐词显现主张
6. 三步学习流程
7. 课堂时间线场景
8. 六条常见问题
9. 末次登录转化与页脚

## 备用动态 GIF

原 GIF 资源仍保留，可随时替换回首屏：

```vue
<img
  src="/assets/homepage-demo.gif"
  alt="课堂演示：实时字幕、课件同步、手语辅助和课后笔记依次生成"
  width="960"
  height="600"
  fetchpriority="high"
/>
```

重新导出 GIF：

```bash
cd tools
npm run gif:homepage
```

## 页面验收

先启动前端开发服务器，再执行：

```bash
cd tools
npm run verify:homepage
node verify-avatar.mjs
node verify-avatar-responsive.mjs
node verify-avatar-route-reuse-2.mjs
```

前两条检查页面和数字人加载，后两条检查桌面、平板、移动端布局及路由切换后的实例复用。

## 修改建议

- 改手语内容时编辑 `PublicHomeView.vue` 的 `avatarText`。
- 改数字人模型大小时调整 `avatar-size`，SDK 接受 1 到 7。
- 数字人需要 WebGL，推荐使用最新版 Chrome 或 Edge。
- 数字人容器高度由 `lp-avatar-canvas` 控制，移动端当前为 260px。
- 系统启用 `prefers-reduced-motion: reduce` 时，页面装饰动画会减少。
