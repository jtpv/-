plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jtpv.powerconsumption"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jtpv.powerconsumption"
        minSdk = 26
        targetSdk = 34
        // 版本号每次出包递增：Android 以 versionCode 判断升级安装，
        // +1 才能在不卸载旧版的情况下直接覆盖安装
        versionCode = 3
        versionName = "1.2"
    }

    buildFeatures {
        // 使用 ViewBinding：可在编译期校验控件 id，适合无法本地编译的场景
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    // 依赖保持精简，避免云构建拉取失败
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
}
