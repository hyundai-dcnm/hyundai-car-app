plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.dcnm"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	// Render 빌드 서버의 Maven Central 429(요청 과다) 대비: Google 운영 Maven Central 복사본(미러) 먼저 조회
	// 미러에 없는 파일(404)만 Maven Central에서 받음
	maven("https://maven-central.storage-download.googleapis.com/maven2/")
	mavenCentral()
}

dependencies {
	// 웹 API(REST)
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// 실시간 연결(WebSocket)
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	// DB(JPA)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// 접근 권한(토큰 확인)
	implementation("org.springframework.boot:spring-boot-starter-security")
	// 입력값 검사(@NotBlank 등)
	implementation("org.springframework.boot:spring-boot-starter-validation")
	// 상태 확인 주소(/actuator/health)
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	// 통화 참가 토큰 발급(LiveKit)
	implementation("io.livekit:livekit-server:0.17.0")
	// 사진 저장(S3 호환 저장소: Supabase Storage)
	implementation(platform("software.amazon.awssdk:bom:2.42.30"))
	implementation("software.amazon.awssdk:s3")

	runtimeOnly("com.h2database:h2")          // 개발용 DB(내 컴퓨터 파일)
	runtimeOnly("org.postgresql:postgresql")  // 배포용 DB(Supabase PostgreSQL)

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
