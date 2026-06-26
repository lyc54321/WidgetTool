# 组件工具箱 - Android 桌面组件应用

## 功能特性

### 三大组件
1. **图片组件** - 可自定义图片、点击音效、点击打开应用/网页，支持大小和不透明度调整
2. **木鱼组件** - 功德+1木鱼，带震动和音效，归零按钮，最小尺寸1×2
3. **倒计时组件** - 可设置时/分/秒，支持开始/暂停/重置

### 核心功能
- 微信风格的主界面列表
- 桌面组件与应用内组件数据互通
- 多个桌面组件可绑定同一组件数据，数据实时同步
- 所有组件支持不透明度调整
- 图片组件支持任意大小调整
- 平板适配：横屏/竖屏优化，圆角自适应
- 平板大屏：网格布局（竖屏2列，横屏3列）

## 项目结构

```
WidgetTool/
├── app/
│   ├── src/main/
│   │   ├── java/com/widgettool/app/
│   │   │   ├── data/              # 数据存储
│   │   │   ├── model/             # 数据模型
│   │   │   ├── ui/                # UI适配器
│   │   │   ├── util/              # 工具类
│   │   │   ├── widget/            # Widget Provider
│   │   │   └── ...                # Activity
│   │   ├── res/                   # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建 APK 方法

### 方法一：Android Studio（推荐）

1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，选择 "Open an existing project"
3. 选择本项目根目录
4. 等待 Gradle 同步完成
5. 点击菜单：**Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
6. 构建完成后，点击通知中的 "locate" 找到 APK 文件
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

### 方法二：命令行（Gradle）

前置条件：安装 JDK 17+ 和 Android SDK

```bash
# Linux/macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

输出位置：`app/build/outputs/apk/debug/app-debug.apk`

### 构建 Release 版本

```bash
./gradlew assembleRelease
```

> 注意：Release 版本需要签名配置。

## 使用说明

### 1. 添加应用内组件
- 打开应用，点击右上角 **+** 按钮
- 选择组件类型（图片/木鱼/倒计时）
- 点击列表中的组件进入编辑页面

### 2. 添加桌面组件
- 长按桌面空白处，选择 "添加小组件"
- 找到 "组件工具箱"，选择要添加的组件类型
- 自动跳转到应用，选择要绑定的组件
- 桌面组件即会显示对应内容

### 3. 数据互通说明
- 多个桌面组件绑定同一个组件 → 数据实时同步
- 桌面组件绑定不同组件 → 数据各自独立
- 在应用内修改组件设置后，桌面组件会自动更新

## 注意事项

- 图片组件的图片会保存到应用内部存储
- 木鱼音效可在设置中开关（默认使用系统震动）
- 倒计时使用 AlarmManager 实现精确计时
- 最低支持 Android 7.0 (API 24)
- 目标版本 Android 14 (API 34)
