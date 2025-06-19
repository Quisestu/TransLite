# 多功能翻译应用

这是一个基于Android Compose开发的多功能翻译应用，支持文本翻译、语音翻译和图片翻译功能。使用腾讯云翻译服务作为后端支持。

## 功能特性

- 📝 **文本翻译**：支持多种语言间的文本翻译
- 🎤 **语音翻译**：实时语音识别和翻译，支持中英互译
- 📷 **图片翻译**：拍照或选择图片进行文字识别和翻译
- 📚 **历史记录**：自动保存翻译记录，支持按类型过滤和查看 (v3.0新增)
- 🎨 **Material Design 3**：现代化的UI设计，深色模式优化配色
- 🌙 **主题切换**：支持亮色/暗色/跟随系统主题
- 🔊 **语音朗读**：翻译结果支持TTS朗读

## 安装和配置

### 1. 克隆项目
```bash
git clone [你的项目地址]
cd Test0
```

### 2. 配置腾讯云服务

#### 2.1 获取腾讯云API密钥
1. 注册并登录 [腾讯云控制台](https://cloud.tencent.com/)
2. 开通以下服务：
   - [机器翻译 TMT](https://console.cloud.tencent.com/tmt)
3. 获取API密钥：访问 [API密钥管理](https://console.cloud.tencent.com/cam/capi)
   - 记录您的 `SecretId` 和 `SecretKey`
   - 记录您的应用ID（AppId）

#### 2.2 创建本地配置文件
1. 复制配置模板：
   ```bash
   cp local.properties.example local.properties
   ```

2. 编辑 `local.properties` 文件，填入您的配置信息：
   ```properties
   # Android SDK路径 (Android Studio会自动生成)
   sdk.dir=YOUR_ANDROID_SDK_PATH

   # 腾讯云配置
   TENCENT_APP_ID=你的应用ID
   TENCENT_SECRET_ID=你的SecretId  
   TENCENT_SECRET_KEY=你的SecretKey
   TENCENT_REGION=ap-guangzhou
   ```

#### 2.3 选择服务器地区
根据您的地理位置选择最近的服务器地区（修改 `TENCENT_REGION` 值）：

**中国大陆：**
- `ap-guangzhou` - 广州（华南）
- `ap-shanghai` - 上海（华东）  
- `ap-beijing` - 北京（华北）
- `ap-chengdu` - 成都（西南）

**海外地区：**
- `ap-singapore` - 新加坡
- `ap-hongkong` - 香港
- `ap-tokyo` - 东京
- `na-siliconvalley` - 硅谷
- `na-ashburn` - 弗吉尼亚

### 3. 打开项目
使用 Android Studio 打开项目，IDE会自动：
- 下载依赖
- 配置Android SDK路径
- 生成必要的构建文件

### 4. 运行应用
1. 连接Android设备或启动模拟器
2. 点击 "Run" 按钮或使用快捷键 `Shift+F10`

## 权限说明

应用需要以下权限：
- `RECORD_AUDIO` - 语音翻译功能
- `CAMERA` - 拍照翻译功能  
- `READ_EXTERNAL_STORAGE` - 选择图片翻译功能
- `INTERNET` - 网络请求翻译服务

## 技术栈

- **Kotlin** - 开发语言
- **Jetpack Compose** - UI框架
- **Material Design 3** - 设计系统
- **Room** - 本地数据库 (v3.0新增)
- **CameraX** - 相机功能
- **腾讯云SDK** - 翻译服务
- **OkHttp** - 网络请求
- **Coil** - 图片加载

## 项目结构

```
app/src/main/java/com/example/test0/
├── config/          # 配置文件
├── database/        # Room数据库 (v3.0新增)
├── model/           # 数据模型
├── navigation/      # 导航配置
├── repository/      # 数据仓库层 (v3.0新增)
├── service/         # 服务层
├── ui/
│   ├── components/  # UI组件
│   ├── screens/     # 界面
│   └── theme/       # 主题配置
└── viewmodel/       # 视图模型
```

## 注意事项

⚠️ **重要提醒：**
- `local.properties` 文件包含敏感信息，请勿提交到版本控制系统
- 腾讯云服务按使用量计费，请注意控制成本
- 首次使用需要在腾讯云控制台开通相关服务

## 故障排除

### 常见问题

**Q: 编译失败，提示找不到 BuildConfig**  
A: 确保已正确配置 `local.properties` 文件，并重新构建项目

**Q: 翻译服务无法使用**  
A: 检查网络连接和腾讯云服务是否正常开通

**Q: 语音识别无响应**  
A: 确认已授予录音权限，并检查设备音频设置

## 版本历史

### v3.0 (2024) - 历史记录功能 + 深色模式优化
- ✨ **新增历史记录功能**
  - Room数据库支持，自动保存翻译记录（最多100条）
  - 支持按类型过滤（文本/语音/图片翻译）
  - 智能长文本展开/收起功能
  - 支持复制和删除记录
- 🎨 **界面优化**
  - 调整深色模式为深蓝色主题
  - 优化过滤器样式和交互
  - 统一图标和按钮设计
- 🔧 **用户体验改进**
  - 平滑的展开动画效果
  - 清晰的操作提示和反馈

### v2.2 (2024) - 图片翻译优化
- 🖼️ 图片翻译用户体验优化
- 🔄 清除逻辑一致性改进
- 🎯 检测状态智能重置

### v2.1 (2024) - AI语音翻译
- 🤖 集成腾讯云智能语音
- 🎤 实时语音识别和翻译
- 🔊 语音合成(TTS)支持

### v2.0 (2024) - 图片翻译
- 📷 图片翻译功能
- 📸 相机拍照和图库选择
- 🔍 OCR文字识别

### v1.0 (2024) - 基础翻译
- 📝 文本翻译功能
- 🌙 主题切换支持
- 🎨 Material Design 3 UI

## 许可证

本项目为个人学习项目，仅供学习和参考使用。 