# 동행

같은 목적지로 이동하는 여러 차량의 일행이 여행방에서 음성으로 대화하고, 위치와 공동 목적지를 공유하는 차량용 앱입니다.

현대자동차그룹 차량용 앱 개발 공모전 출품작 · 팀 **타고난 사람들**

## 제출 APK

- [v0.1.0 GitHub Release](https://github.com/hyundai-dcnm/hyundai-car-app/releases/tag/v0.1.0)
- [TagonanPeople_Donghaeng_release.apk 다운로드](https://github.com/hyundai-dcnm/hyundai-car-app/releases/download/v0.1.0/TagonanPeople_Donghaeng_release.apk)
- 버전: `0.1.0` (`versionCode=1`), 패키지: `com.dcnm.donghaeng`
- 약 174MiB의 서명된 release APK입니다. 용량 때문에 Git에 직접 커밋하지 않고 Release의 Assets로 제공합니다.
- SHA-256: `89ea8c874343d18f69e83b4c50084ff1391b92e397b569d39fae3f5934913f33`
- release 빌드에는 로컬 시험용 LiveKit URL·토큰을 포함하지 않습니다. 실제 통화 접속 정보는 여행방 참여 후 서버에서 발급받습니다.

## 실행 방법

### 제출 APK 설치와 실행

1. Android Studio Device Manager에서 **Pleos Connect v2.1.0** 에뮬레이터를 Cold Boot로 실행합니다.
2. 위 Release에서 APK를 내려받아 아래 명령으로 설치합니다. Android SDK의 `platform-tools`를 PATH에 추가한 환경을 기준으로 합니다.

```bash
adb install -r TagonanPeople_Donghaeng_release.apk
adb shell am start -n com.dcnm.donghaeng/com.dcnm.donghaeng.MainActivity
```

3. 정차 상태에서 앱의 **동행 만들기**로 여행방을 생성합니다. 다른 기기는 **초대 코드로 참여**를 선택해 같은 방에 들어갑니다.
4. 공동 목적지를 제안하고 참여자 전원이 수락하면 목적지가 확정됩니다. 위치 공유와 통화 참여는 각각 사용자가 선택합니다. 권한 요청이 나오면 해당 기능에 필요한 권한을 허용합니다.
5. 전체 화면 흐름은 앱 왼쪽 아래 **설정 → 디자인 시연 시작**에서 확인할 수 있습니다. 설정은 차량의 UX 제한이 해제된 정차 상태에서 이용할 수 있습니다.

디자인 시연의 주변 탐색·친구 연결·통화·음악은 예시 상태입니다. 시연 통화는 실제 음성을 전송하지 않으며 음악도 재생하지 않습니다. 시연 모드에서도 차량 UX 제한은 유지합니다.

### 소스에서 빌드

JDK 17과 Android SDK 35를 준비하고 Android Studio에서 프로젝트를 엽니다. Gradle JDK를 17로 설정하고 동기화합니다.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. debug APK 경로는 `app/build/outputs/apk/debug/app-debug.apk`, 패키지는 `com.dcnm.donghaeng.debug`입니다.

제출용 APK를 직접 만들 때는 Android Studio의 **Build → Generate Signed App Bundle or APK → APK → release**에서 본인의 서명 키를 사용합니다. `assembleRelease`만 실행하면 서명 설정이 없는 환경에서는 unsigned APK가 생성됩니다. 서명 키와 비밀번호는 저장소에 올리지 않습니다.

## 테스트 환경 및 필요한 설정

### 개발·검증 환경

| 항목 | 환경 |
| --- | --- |
| 개발 및 실행 에뮬레이터 | **Pleos Connect Emulator 2.1.0** |
| Android 버전 | Android 14 / API 34 |
| 화면 규격 | **2560 × 1440 px, 160 dpi** |
| UI 디자인 기준 | 1280 × 800. 차량 시스템 UI가 차지하는 공간에 따라 실제 앱 창 크기는 달라집니다. |
| 실행 설정 | Cold Boot, RAM 4GB |
| 이번 빌드·테스트 호스트 | macOS / Apple Silicon, JDK 17 |
| Android 빌드 | Gradle 8.10.2, AGP 8.8.2, Kotlin 2.0.21, Jetpack Compose |
| SDK 레벨 | compileSdk / targetSdk 35, minSdk 28 |
| 차량 SDK 연동 | Pleos NaviHelper 3.0.5, FusedLocation 1.1.3 |
| 지도·통화 | NAVER 지도 SDK 3.24.1, LiveKit Android SDK 2.30.0 |

일반 휴대폰 에뮬레이터에서는 Pleos 차량 API를 동일하게 검증할 수 없습니다. Pleos 시스템 이미지가 없다면 Android Studio의 SDK Update Sites에 운영체제에 맞는 주소를 추가하고 API 34 이미지를 설치합니다.

- Mac ARM64: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
- Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

### 네트워크·서버

- 앱은 `https://donghaeng-server.onrender.com`의 배포 서버에 연결합니다. 인터넷 연결이 필요하며 별도 서버 주소나 개발용 토큰을 입력할 필요가 없습니다.
- 서버 상태 확인: `https://donghaeng-server.onrender.com/actuator/health`
- 서버 재시작이나 휴면 복귀 직후에는 응답이 지연될 수 있습니다. 연결 실패 시 잠시 후 재시도합니다.
- 앱 프로세스가 종료되면 세션이 자동 복원되지 않아 여행방에 다시 참여해야 합니다.
- 서버를 별도로 실행하는 방법과 DB·LiveKit·사진 저장소 환경 변수는 [서버 README](server/README.md)에 있습니다. 앱의 기본 연결 대상은 배포 서버입니다.

### 위치·지도·마이크

- 위치 공유에는 Pleos 위치 권한과 앱 안에서의 별도 공유 동의가 필요합니다. 방 참여만으로 위치 전송을 시작하지 않습니다.
- 실지도 사용을 위한 소스 빌드에는 루트 `local.properties`의 `NAVER_MAP_CLIENT_ID`와 해당 앱 패키지에 맞는 지도 서비스 설정이 필요합니다. 키가 없으면 예시 지형과 **실지도 연결 전·위치 표시 없음** 안내가 나타납니다. 내려받은 APK의 설정은 로컬 파일 수정만으로 바뀌지 않습니다.
- 통화에는 마이크 권한이 필요합니다. macOS에서는 Android Studio의 마이크 접근을 허용하고, 에뮬레이터의 Extended Controls → Microphone → **Virtual microphone uses host audio input**을 켭니다. 입력 장치를 바꾸면 스위치를 껐다 켭니다.
- 설정·검색·사진 업로드 등은 정차 및 차량 UX 제한 해제 상태에서 확인합니다.

## 현재 구현 범위

| 기능 | 현재 상태 |
| --- | --- |
| 차량 UI | Compose 기반 홈·방·목적지·통화·앨범 화면, 차량 UX 제한 반영 |
| 여행방 | 실제 서버에 생성·초대 코드 참여·조회·퇴장, REST/WebSocket 연결 |
| 공동 목적지 | 제안·전체 참여자 합의, 내비 목적지 가져오기, 직접 등록·최근 기록·즐겨찾기 |
| 위치·지도 | Pleos 위치 수신과 동의 기반 송신, 지도 연동 코드 구현. 실지도·실좌표 통합 검증은 추가 필요 |
| 내비 | NaviHelper 단일 목적지 경로 요청. 기존 최종 목적지를 유지하는 경유지 추가는 후속 개발 |
| 통화 | 서버 토큰 발급과 LiveKit 연결·음소거·종료 연동. 최신 UI에서 실제 양방향 음성·백그라운드 지속은 추가 검증 필요 |
| 여행 앨범 | 정차 및 방 참여 후 사진 업로드·목록·확대·삭제 |
| 주변 탐색·친구·음악·휴식 추천 | 시연 전용 또는 미연동. 실제 주변 매칭·친구 API·음악 동기 재생은 후속 개발 |

## 검증 결과

2026-11-15 기준:

- 앱 단위 테스트 21개를 debug와 release에서 각각 통과했습니다.
- 서버 테스트 4개, Pleos 에뮬레이터 계측 테스트 5개를 통과했습니다.
- 계측 범위: 디자인 시연 화면 흐름, 제한 상태 입력 차단, 실제 서버 방 생성·WSS 연결·목적지 확정·통화 토큰 발급, 사진 업로드·조회·표시·삭제, 목적지 기록 암호화 저장, 런처 아이콘.
- debug/release 빌드 성공, `lintDebug` 오류 0개·경고 49개입니다.
- 배포 APK의 서명, 개발용 토큰 제외, Pleos 설치와 홈 화면 실행을 확인했습니다.
- 실제 양방향 음성·백그라운드 통화, 실지도 인증·표시, 실좌표 송신, 최신 UI에서 차량 내비 안내까지의 전체 흐름과 실차 동작은 별도 검증이 필요합니다. 제한 상태 전환 테스트는 계측에서 상태를 주입했으며 실제 D 기어 주행 시험은 아닙니다.

자동 테스트 재실행:

```bash
./gradlew test :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

계측 테스트는 정차 상태의 Pleos 에뮬레이터와 인터넷 연결이 필요하며, 배포 서버에 임시 여행방·사진을 생성하고 테스트 종료 시 정리합니다. 서버 테스트는 `server/` 디렉터리에서 `bash ./gradlew test`로 실행합니다.

## 저장소 구성

```text
app/                차량 UI 진입점, 서버 어댑터, 목적지 저장·앨범, 통화 서비스
core/model/         여행방·멤버·목적지 공통 모델
core/trip/          TripRepository 인터페이스와 테스트용 FakeTripRepository
core/call/          LiveKit 음성 통화
core/location/      Pleos FusedLocation 위치 수신
core/navi/          Pleos NaviHelper 내비 연동
core/map/           NAVER 지도와 일행 위치 표시
feature/room/       기존 방 생성·참여 화면
feature/trip/       기존 여행방 화면
server/            Spring Boot 서버·DB·실시간 상태·통화 토큰·사진 API
contract/api.md     앱·서버 API 계약
```

현재 앱 진입점은 `VehicleApp`이며 `ServerTripRepository`로 실제 서버에 연결합니다. 구현 과정과 추가 기술 설명은 [차량 프론트 기록](VEHICLE_UI.md)을 참고하세요. 해당 문서에는 초기 구현 기록도 포함되어 있으며, 제출 버전의 실행 방법과 검증 범위는 이 README를 기준으로 합니다.

## 관련 문서

- [공모전 제출 가이드](https://appdevchallenge.auto-hmg.io/guide/submission)
- [Pleos Connect SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/)
- [Pleos 샘플](https://github.com/Pleos-Playground)
