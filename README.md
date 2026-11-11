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

지금은 ② 차량 앱 화면·음성 통화 부분만 있다. 위치·지도·내비(①), 웹(③), 서버(④)는 각자 구조를 잡아 추가한다.

```
app/                차량 앱 진입점. 수동 DI(AppContainer), 화면 전환, 마이크 권한
core/model          순수 Kotlin. 화면이 쓰는 여행방·멤버·목적지 타입
core/trip           TripRepository 인터페이스 + 서버 없이 도는 FakeTripRepository
core/call           음성 통화. CallClient 인터페이스 + LiveKit 구현
feature/room        방 만들기·초대 코드로 참여 화면
feature/trip        여행방 화면: 일행, 목적지 제안·수락, 위치 공유 토글, 통화. 왼쪽은 지도 자리
```

의존 방향: `app → feature/* → core/* → core/model`.
서버 연동(`TripRepository` 서버 구현)은 ④와 API 형식을 정한 뒤 붙인다.

## 개발 환경

- JDK 17 (Android Studio > Settings > Build Tools > Gradle > Gradle JDK 를 17로)
- AGP 8.8, Kotlin 2.0, compileSdk 35, minSdk 28, Jetpack Compose
- Pleos Connect SDK (Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`)
  - Vehicle 2.3.2, NaviHelper 3.0.5, TextToSpeech 3.3.1, SpeechToText 3.3.1, FusedLocation 1.1.3 (`gradle/libs.versions.toml`에 등록만 됨)
- Pleos Connect Emulator 2.1.0 (API 34). AVD는 Automotive > Pleos Connect v2.1.0
  - **Cold boot 필수** (Quick boot면 VHAL을 못 씀), RAM 4GB
  - Mac(arm64) 시스템 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
  - Android Studio > Settings > SDK Update Sites 에 위 주소 추가 → SDK Platforms 에서 API 34 Pleos 이미지 설치
  - 통화 테스트는 아래 "통화 소리 테스트" 참고

## 실행

```bash
./gradlew :app:installDebug       # 가짜 여행방(일행 두 대)으로 화면 확인
```

## 통화 소리 테스트 (서버 없이)

LiveKit Cloud 프로젝트에 초대받은 뒤 진행한다.

1. 토큰 만들기. 기본 유효시간이 5분이라 `--valid-for`를 꼭 붙인다.
   ```bash
   brew install livekit-cli
   lk cloud auth      # 별도 터미널 앱에서 직접 실행 (브라우저 승인이 필요해서)
   lk token create --join --room test --identity <내이름>-car --valid-for 24h   # 에뮬레이터용
   lk token create --join --room test --identity browser --valid-for 24h        # 브라우저용
   ```
2. 에뮬레이터용 토큰을 `~/.gradle/gradle.properties`에 넣고 `./gradlew :app:installDebug` (저장소에 올리지 않음).
   ```properties
   donghaeng.livekitUrl=wss://dcnm-hyundai-donghaeng-oirv3cmd.livekit.cloud
   donghaeng.livekitToken=<에뮬레이터용 토큰>
   ```
3. 에뮬레이터는 **Android Studio Device Manager에서** 켠다. 터미널에서 직접 띄우면 맥 마이크 권한을 못 받아 무음이 된다.
4. 에뮬레이터 툴바 ⋮ > Extended Controls > Microphone > **Virtual microphone uses host audio input** 켜기.
   에어팟 등으로 맥 입력 장치가 바뀌었으면 이 스위치를 껐다 켜야 새 장치를 잡는다.
5. 앱에서 방 만들기 → 통화 참여. 브라우저는 https://meet.livekit.io 의 Custom 탭에 URL과 브라우저용 토큰을 넣고 입장.
6. 둘 다 같은 맥이라 소리가 같은 이어폰으로 나온다. 한 방향씩 확인할 때는 반대쪽을 음소거한다.

마이크에 소리가 들어오는지는 로그로 확인할 수 있다. `level`은 10초 동안 가장 큰 값(0~32767)이다.

```bash
adb logcat | grep -E "REC :|PLAY:"
# REC  level: 8     → 무음. 위 3·4번 확인
# REC  level: 수천  → 정상
# REC 줄이 안 나옴  → 앱에서 음소거 중 (정상 동작)
```

Pleos에서는 홈 키(`adb shell input keyevent 3`)로 앱이 뒤로 가지 않는다. 앱이 뒤에 있을 때를 시험하려면 하단 바의 내비 아이콘을 눌러 순정 내비를 앞으로 띄운다.

## 문서

- 공모전 가이드: https://appdevchallenge.auto-hmg.io/guide/overview
- Pleos SDK: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- 샘플: https://github.com/Pleos-Playground
