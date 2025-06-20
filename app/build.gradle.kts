import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.compose)
    kotlin("kapt")
}

android {
    namespace = "com.example.test0"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.translite.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { localProps.load(it) }
        }
        
        // 始终创建BuildConfig字段，避免编译错误
        buildConfigField("String", "TENCENT_APP_ID", "\"${localProps.getProperty("TENCENT_APP_ID") ?: ""}\"")
        buildConfigField("String", "TENCENT_SECRET_ID", "\"${localProps.getProperty("TENCENT_SECRET_ID") ?: ""}\"")
        buildConfigField("String", "TENCENT_SECRET_KEY", "\"${localProps.getProperty("TENCENT_SECRET_KEY") ?: ""}\"")
        buildConfigField("String", "TENCENT_REGION", "\"${localProps.getProperty("TENCENT_REGION") ?: "ap-guangzhou"}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // 腾讯云SDK
    implementation("com.tencentcloudapi:tencentcloud-sdk-java:3.1.1274") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    
    // Permissions handling
    implementation(libs.accompanist.permissions)
    
    // Icons Extended
    implementation(libs.androidx.material.icons.extended)
    
    // Coil for image loading
    implementation(libs.coil.compose)
    // OkHttp for WebSocket
    implementation(libs.okhttp)
    
    // Okio for toByteString
    implementation("com.squareup.okio:okio:2.10.0")
    
    // Testing - 简化版本，只保留基本测试依赖
    testImplementation(libs.junit)
    testImplementation("org.json:json:20230227")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
}