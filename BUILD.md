# TickMate 构建说明

## 环境要求
- Node.js 18+
- Android Studio
- Android SDK

## 快速开始

### 1. 升级Node (如果当前版本 < 18)
```bash
# 使用nvm升级
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc
nvm install 18
nvm use 18
```

### 2. 安装依赖
```bash
npm install
```

### 3. 运行开发版
```bash
npx expo start
# 然后按 'a' 在Android模拟器运行
```

### 4. 构建APK
```bash
# 方式1: 使用EAS Build (推荐)
npm install -g eas-cli
eas build --platform android --profile production

# 方式2: 本地构建
npx expo prebuild
cd android
./gradlew assembleRelease
# APK位置: android/app/build/outputs/apk/release/app-release.apk
```

## 功能清单
- ✅ 手动录入消费记录
- ✅ 记录列表展示
- ✅ 统计图表(饼图)
- ✅ 类目管理(预设6个类目)
- ✅ 预算设置
- ✅ 暗黑科技风格UI
- ✅ 数据持久化(AsyncStorage)

## 项目结构
```
src/
├── screens/          # 页面
│   ├── HomeScreen.js
│   ├── StatsScreen.js
│   └── SettingsScreen.js
├── components/       # 组件
│   └── AddExpenseModal.js
└── services/         # 服务
    └── storage.js
```
