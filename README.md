# Python IDE — Termux variant

A **modified Termux-style app with a GUI code editor** (so you are not limited to nano/CLI).

## How it works
- **applicationId = `com.termux`** — because Termux binaries have the prefix
  `/data/data/com.termux/files/usr` compiled in. With this package name, the paths line up,
  so **no package rebuild is needed**.
- **targetSdk = 28** — required so binaries can be executed from the app's data directory
  on Android 10+ (the W^X restriction applies to apps targeting API 29+).
- **GUI editor:** Sora Editor + TextMate Python grammar (syntax highlighting).
- **Python for the editor's Run:** Chaquopy (with `numpy` and `matplotlib` bundled).
- **Real Termux environment:** the Termux bootstrap (bash, coreutils, apt/`pkg`, pip) is
  downloaded and extracted on first run. Use the **Termux** screen.

## IMPORTANT
- This app uses the package name `com.termux`, so **it cannot coexist with the real Termux app**
  (Android allows only one app per package name).
- It **cannot be published on Google Play** (package-name impersonation). **Sideload only.**
- This is an **experimental fork**. Test it yourself.

## Get the APK
GitHub → **Actions** → **Build APK** → **Run workflow** → download the **app-debug-apk** artifact.

## Usage
1. Home → **⋮** → **Termux**. First run downloads + extracts the bootstrap (~33 MB, needs internet).
2. Then run e.g. `pkg update -y`, `pkg install -y python`, `pip install numpy`, `python --version`.
3. Home → a project → write code in the GUI editor → **Run**.
