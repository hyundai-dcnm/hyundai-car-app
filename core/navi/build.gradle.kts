plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dcnm.donghaeng.core.navi"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pleos.navihelper)
}
