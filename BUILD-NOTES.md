# Python IDE (Android) — v1.1

## Kya hai
Offline Python code editor + runner, aur ek **experimental Linux (proot + Alpine)** screen.

## Navigation
- **Home** (launcher) — project list, ⋮ menu (सेटिंग्स / पैकेज इंस्टॉल / **Linux (proot)** / ऐप के बारे में), + FAB
- **Editor** — Sora Editor + special keys + Run → alag **Output** screen
- **Install** — runtime pip (pure-Python) + GitHub clone (zip)
- **Linux (proot)** — proot + Alpine ke andar shell commands (apk add python3, numpy...)

## Linux (proot) — experimental
- `proot` binaries (Android, arm64 + x86_64) jniLibs me
- Alpine minirootfs assets me (`assets/proot/alpine-*.rootfs`), pehli baar app khulne par extract hota hai
- Home ⋮ → **Linux (proot)** → command likho (jaise `apk add python3 py3-pip`) → chalao
- `apk add py3-numpy` / `py3-matplotlib` bhi mil jate hain (Alpine packages)

### Dhyan
- Ye **experimental** hai. proot Android 10+ par W^X ki wajah se kabhi fail ho sakta hai.
- `targetSdk 34` hai; agar proot exec block kare to `PROOT_NO_SECCOMP=1` try kiya gaya hai.
- Internet chahiye `apk add` ke liye.

## Build
JDK 17 + Android SDK (platform 34, build-tools 34.0.0) + Gradle 8.11.1 / Kotlin 2.2.20 / AGP 8.9.1.
```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

## Package jodna (Chaquopy, build-time)
`app/build.gradle.kts` me `chaquopy { defaultConfig { pip { install("numpy") } } }`.

## Structure
```
app/src/main/
  java/com/sarvam/pythonide/
    HomeActivity / EditorActivity / OutputActivity / SettingsActivity
    InstallActivity (runtime pip + git clone)
    LinuxActivity   (proot terminal)   + ProotEnv.kt
    EditorSetup / PythonRunner / FileStore / Prefs
  python/runner.py
  jniLibs/{arm64-v8a,x86_64}/libproot.so, libproot_loader.so
  assets/proot/alpine-*.rootfs
  assets/textmate/...
```
