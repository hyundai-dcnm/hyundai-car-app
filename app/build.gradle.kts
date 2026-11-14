plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dcnm.donghaeng"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dcnm.donghaeng"
        minSdk = 28          // 공모전 요건: API 28+
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 서버 없이 통화를 시험할 때 쓰는 LiveKit 접속 정보. ~/.gradle/gradle.properties 나 -P 로 넣는다.
        // 토큰은 `lk token create --join --room test --identity <이름>` 으로 만든다. 저장소에 올리지 않는다.
        val livekitUrl = providers.gradleProperty("donghaeng.livekitUrl").getOrElse("")
        val livekitToken = providers.gradleProperty("donghaeng.livekitToken").getOrElse("")
        buildConfigField("String", "DEV_LIVEKIT_URL", "\"$livekitUrl\"")
        buildConfigField("String", "DEV_LIVEKIT_TOKEN", "\"$livekitToken\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(project(":core:trip"))
    implementation(project(":core:call"))
    implementation(project(":core:location"))
    implementation(project(":core:map"))
    implementation(project(":feature:room"))
    implementation(project(":feature:trip"))

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
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
