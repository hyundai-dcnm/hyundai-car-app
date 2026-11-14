import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// NAVER 지도 Client ID 는 저장소에 올리지 않는다. local.properties 의 NAVER_MAP_CLIENT_ID 에서 읽고, 없으면 빈 값(지도 대신 안내 문구가 보인다).
val naverMapClientId: String = rootProject.file("local.properties").takeIf { it.exists() }
    ?.let { f -> Properties().apply { f.inputStream().use { load(it) } }.getProperty("NAVER_MAP_CLIENT_ID") }
    .orEmpty()

android {
    namespace = "com.dcnm.donghaeng.core.map"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
        buildConfigField("String", "NAVER_MAP_CLIENT_ID", "\"$naverMapClientId\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    api(project(":core:model"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.naver.map.sdk)
    implementation(libs.naver.map.compose)
}
