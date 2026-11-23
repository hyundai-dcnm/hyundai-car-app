pluginManagement {
	repositories {
		// 플러그인(Spring Boot 등)도 Maven Central에서 받으므로 같은 미러 먼저 조회
		maven("https://maven-central.storage-download.googleapis.com/maven2/")
		gradlePluginPortal()
	}
}

rootProject.name = "server"
