# 동행

현대자동차그룹 차량용 앱 개발 공모전 ("Code your App into Pleos App Market") 팀 저장소.

같은 목적지로 가는 차량들이 하나의 여행방에 모여 통화, 경로, 음악, 현위치를 공유하는 차량 앱.

## MVP

1. 여행방 연결
   - 목적지가 같은 차량끼리 연결. 모르는 사람끼리는 서로 동의해야 연결된다.
   - 같이 이동하는 일행끼리 친구 추가, 또는 이미 친구인 차량끼리 같은 목적지 정하기.
2. 공유 기능
   - **통화(라디오)**: 차량끼리 음성 통화로 현재 상황과 안부를 나눈다.
   - 내비게이션 경로: 수락한 목적지·경유지로 각 차량 내비 안내 연동.
   - **음악**
   - 현위치(GPS): 묻지 않아도 일행 위치를 지도에서 확인. 공유는 언제든 끌 수 있다.

## 역할 분담

| 담당 | 역할 | 주요 작업 | 완료 기준 |
| --- | --- | --- | --- |
| ① | 차량 위치·지도·내비 | GPS 수신, 위치 전송, 지도에 일행 표시, 공동 목적지·경유지 내비 연동 | 두 차량의 위치가 표시되고, 수락한 목적지로 각각 길 안내 요청 |
| ② | 차량 앱 화면·음성 통화 | 방 생성·참여, 위치 공유 설정, 목적지 수락 화면, 통화·음소거·종료, Android 앱 통합 | 두 앱이 같은 방에 참여하고 실제 양방향 음성 연결 |
| ③ | 웹 | 사진 업로드, 공동 앨범·여행 카드 | 같은 여행방의 사진을 실제 데이터로 표시 |
| ④ | 서버·DB·실시간 동기화 | 계정·초대 코드, 여행방, 위치 중계, 목적지 수락 상태, 사진 저장, 접근 권한 | 앱과 웹의 상태가 동기화되고 공유 중지·퇴장이 반영됨 |

| 담당 | 주로 사용할 기술 |
| --- | --- |
| ① | Kotlin, Pleos FusedLocation·NaviHelper, Coroutines/Flow, NAVER 지도 Android SDK 후보 |
| ② | Kotlin, Jetpack Compose, ViewModel/StateFlow, Retrofit/OkHttp, LiveKit Android SDK 후보 |
| ③ | TypeScript, React, Vite, CSS Modules, fetch/WebSocket |
| ④ | Java/Spring Boot, Spring Data JPA, PostgreSQL, Spring WebSocket, Spring Security, S3 계열 저장소 후보, LiveKit 서버 SDK 후보 |

## 일정

| 날짜 | 항목 |
|---|---|
| 2026-11-15 (일) | 접수 마감 (기획서 DOCX 필수, 영상·GitHub·APK 선택) |
| 2026-11-23 | 1차 서류 발표 |
| 2026-12-03 ~ 04 | 2차 오프라인 PT (핵심 기능 구현 + 시연 필수) |
| 2026-12-18 | 최종 발표 |
| 2027-02 | 전시·시상 |

## 저장소 구성

```
app/                차량 앱 진입점. 수동 DI(AppContainer), 화면 전환, 마이크 권한          ②
core/model          순수 Kotlin. 여행방·멤버·위치·목적지 타입, 이벤트, 상태 갱신 규칙       공통
core/network        REST(Retrofit) + WebSocket(OkHttp, 재연결), 가짜 서버(FakeTripRepository) ②
core/call           음성 통화. CallClient 인터페이스 + LiveKit 구현                      ②
core/location       차량 위치. Pleos FusedLocation(TODO) + 시뮬레이션                     ①
core/navi           순정 내비 안내 요청. Pleos NaviHelper(TODO)                          ①
feature/room        방 만들기·초대 코드로 참여 화면                                       ②
feature/trip        여행방 화면: 일행, 목적지 제안·수락, 위치 공유, 통화                    ②
feature/map         일행 지도(TripMap). 지금은 좌표만 찍는 자리표시                         ①
server/             Spring Boot. 여행방·초대 코드·이벤트 중계·통화 토큰. 지금은 메모리 저장   ④
web/                React + Vite. 여행 앨범                                              ③
contract/api.md     앱·서버·웹이 맞춰야 하는 REST·WebSocket 형식
```

의존 방향: `app → feature/* → core/* → core/model`. 각자 맡은 곳의 `TODO(①)` 같은 표시부터 채우면 된다.

## 개발 환경

- JDK 17 (Android Studio > Settings > Build Tools > Gradle > Gradle JDK 를 17로)
- Android: AGP 8.8, Kotlin 2.0, compileSdk 35, minSdk 28, Jetpack Compose
- 서버: Spring Boot 4.1, Java 17. 웹: Node 24, React 19, Vite
- Pleos Connect SDK (Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`)
  - Vehicle 2.3.2, NaviHelper 3.0.5, TextToSpeech 3.3.1, SpeechToText 3.3.1, FusedLocation 1.1.3
- Pleos Connect Emulator 2.1.0 (API 34). AVD는 Automotive > Pleos Connect v2.1.0
  - **Cold boot 필수** (Quick boot면 VHAL을 못 씀), RAM 4GB
  - Mac(arm64) 시스템 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
  - Android Studio > Settings > SDK Update Sites 에 위 주소 추가 → SDK Platforms 에서 API 34 Pleos 이미지 설치
  - 통화 테스트: Extended Controls > Microphone 에서 호스트 마이크 입력 켜기

## 실행

```bash
# 차량 앱 (서버 없이 가짜 일행으로)
./gradlew :app:installDebug

# 차량 앱 → 노트북 서버 (에뮬레이터에서 노트북은 10.0.2.2)
./gradlew :app:installDebug -Pdonghaeng.apiBaseUrl=http://10.0.2.2:8080/

# 서버 (기본 H2 메모리. 통화 토큰은 LiveKit 환경 변수가 있어야 발급)
cd server
LIVEKIT_URL=wss://<프로젝트>.livekit.cloud LIVEKIT_API_KEY=... LIVEKIT_API_SECRET=... ./gradlew bootRun
docker compose up -d && ./gradlew bootRun --args='--spring.profiles.active=postgres'   # PostgreSQL

# 웹 (/api, /ws 는 localhost:8080 으로 프록시)
cd web && npm install && npm run dev

# 테스트
./gradlew :core:model:test
cd server && ./gradlew test
```

## 문서

- 공모전 가이드: https://appdevchallenge.auto-hmg.io/guide/overview
- Pleos SDK: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- 샘플: https://github.com/Pleos-Playground
