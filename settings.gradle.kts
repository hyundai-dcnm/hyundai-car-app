pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Connect SDK (Vehicle, NaviHelper, TTS, STT, FusedLocation)
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
        // LiveKit Android SDK의 audioswitch 의존성
        maven("https://jitpack.io") { content { includeGroup("com.github.davidliu") } }
        // NAVER 지도 SDK
        maven("https://repository.map.naver.com/archive/maven") { content { includeGroup("com.naver.maps") } }
    }
}

rootProject.name = "donghaeng"

include(":app")
include(":core:model")
include(":core:trip")
include(":core:call")
include(":core:location")
include(":core:navi")
include(":core:map")
include(":feature:room")
include(":feature:trip")
