# 동행 - 서버

여행방·초대 코드·실시간 동기화(위치·목적지 수락)·통화 토큰·공동 앨범을 맡는 Spring Boot 서버.
API 형식은 [`../contract/api.md`](../contract/api.md)

## 기술

Java 17, Spring Boot 4.1, Spring Data JPA, Spring WebSocket, Spring Security, PostgreSQL(배포)/H2(개발), LiveKit 서버 SDK, AWS SDK(S3 호환 저장소)

## 배포 주소

- https://donghaeng-server.onrender.com (상태 확인: /actuator/health)
- Render 무료 플랜이라 15분 동안 요청이 없으면 서버가 중지된다. 이를 막기 위해 외부 스케줄러(cron-job.org)가 10분마다 상태 확인 주소를 호출한다.
- 배포 및 재시작 직후에는 첫 요청이 최대 1분 걸릴 수 있다.

## 내 컴퓨터에서 실행

별도 DB 설치 없이 `server/data/` 폴더에 H2 파일 DB 가 생긴다.

    cd server
    ./gradlew bootRun          # Windows: gradlew.bat bootRun

- 상태 확인: http://localhost:8080/actuator/health → `{"status":"UP"}`
- 에뮬레이터에서는 `http://10.0.2.2:8080`
- 통화 토큰까지 시험하려면 환경 변수 `LIVEKIT_URL`, `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET` 을 넣고 실행

## 테스트

    ./gradlew test

## 배포

Render(Docker, Root Directory `server`) + Supabase(PostgreSQL·Storage). 비밀값은 Render 환경 변수에만 작성.

| 환경 변수 | 예 |
|---|---|
| DATABASE_URL | `jdbc:postgresql://<pooler 주소>:5432/postgres?sslmode=require` |
| DATABASE_USERNAME / DATABASE_PASSWORD | Supabase Session pooler 의 user / 비밀번호 |
| LIVEKIT_URL / LIVEKIT_API_KEY / LIVEKIT_API_SECRET | LiveKit Cloud |
| STORAGE_TYPE | `s3` |
| S3_ENDPOINT / S3_REGION / S3_BUCKET / S3_ACCESS_KEY / S3_SECRET_KEY | Supabase Storage S3 |
| CORS_ALLOWED_ORIGINS | `https://<서버주소>,https://<웹주소>,http://localhost:5173` |