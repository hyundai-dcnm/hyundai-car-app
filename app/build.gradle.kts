plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val appVersionMajor = 0
val appVersionMinor = 1
val appVersionPatch = 0

android {
    namespace = "com.dcnm.carapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dcnm.carapp"
        minSdk = 28          // 공모전 요건: API 28+
        targetSdk = 35
        versionCode = appVersionMajor * 10_000 + appVersionMinor * 100 + appVersionPatch
        versionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 정비사 웹 리포트(web/mechanic/) 호스팅 주소. QR이 이 주소 + #r=<요약>을 연다.
        // 로컬 시연은 -Pcarapp.reportWebUrl=http://<노트북 IP>:8000/mechanic/ 로 덮어쓴다.
        val reportWebUrl = providers.gradleProperty("carapp.reportWebUrl")
            .getOrElse("https://hyundai-dcnm.github.io/hyundai-car-app/mechanic/")
        buildConfigField("String", "REPORT_WEB_URL", "\"$reportWebUrl\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "ENABLE_MOCK_SIGNALS", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_MOCK_SIGNALS", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 서명 키는 로컬 keystore.properties 로 주입 (저장소에 올리지 않음)
        }
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
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:vehicle"))
    implementation(project(":core:voice"))
    implementation(project(":core:navi"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
