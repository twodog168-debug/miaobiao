# 秒表（独立版）

这是一个只包含秒表功能的 Android 项目，可直接上传到 GitHub，通过 GitHub Actions 云编译。

## 功能
- 开始、暂停、继续
- 计次（圈速）
- 重置
- 百分秒显示
- 使用 `SystemClock.elapsedRealtime()` 计算经过时间，避免依赖定时刷新次数累计时间

## GitHub 云编译
1. 解压本项目。
2. 将解压后的所有文件上传到 GitHub 仓库根目录（不要把外层文件夹再套一层）。
3. 打开仓库的 **Actions**。
4. 选择 **Android CI**，点击 **Run workflow**；也可以 push 到 `main` / `master` 自动编译。
5. 编译完成后，在该次运行页面底部的 **Artifacts** 下载 `StopwatchOnly-debug`。
6. 解压后安装 `app-debug.apk`。

## 项目信息
- applicationId：`com.example.stopwatch`
- 最低 Android 版本：Android 6.0（API 23）
- 编译：JDK 17 / Gradle 8.9 / Android Gradle Plugin 8.7.3
- 不需要网络、定位、存储或后台运行权限
