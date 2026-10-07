plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

android {
    namespace = "com.sarvam.pythonide"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.termux"
        minSdk = 24
        targetSdk = 28
        versionCode = 1
        versionName = "1.0-termux"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

}

chaquopy {
    defaultConfig {
        version = "3.12"
        buildPython("python3")
        pip {
            // Third-party packages yahan jodein (build ke waqt bundle honge).
            install("numpy")
            install("matplotlib")
        }
    }
}

dependencies {
    // Real Termux terminal (terminal-view + terminal-emulator, native PTY)
    implementation("com.termux.termux-app:terminal-view:0.118.0")

    implementation(platform("io.github.rosemoe:editor-bom:0.24.4"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("org.apache.commons:commons-compress:1.26.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}
