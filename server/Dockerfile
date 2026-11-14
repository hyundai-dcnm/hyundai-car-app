# Render 서버 생성용 파일

# 1. 소스 -> 실행 파일(jar) 생성
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 2. 실행에 필요한 것만 담기
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# 메모리 절약
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]