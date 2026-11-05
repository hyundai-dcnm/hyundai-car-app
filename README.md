# hyundai-car-app

현대자동차그룹 차량용 앱 개발 공모전 ("Code your App into Pleos App Market") 팀 저장소.

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
app/                       차량 앱 (단일 Activity, Compose, 수동 DI)
  ui/driving               주행 중 화면: 대형 버튼 1개 + 상태 1줄, 음성 루프
  ui/parked                정차 중 화면: 사건 목록, 리포트 카드, QR
  ui/maintenance           정비 결과·교체 이력·다음 점검
  di/AppContainer          모듈 조립. debug 빌드만 MockSignalSource 추가
core/model                 순수 Kotlin 도메인 타입 (VehicleSignal, SymptomEvent, SignalSnapshot, Maintenance*)
core/vehicle               Pleos Vehicle SDK + AAOS 폴백 → 30초 링버퍼 → 주행/정차 판정 → 트리거 감지
core/voice                 Pleos TTS/STT 래퍼, 예/아니오 파서
core/navi                  NaviHelper/FusedLocation 래퍼, 번들 정비소 JSON
core/data                  Room (증상 사건, 정비 기록) + 리포지토리
scripts/                   Virtual Console 포워딩, 주행 규제 토글, release APK 설치
apk/                       (제출 시) 팀명_앱이름_release.apk
```

의존 방향: `app → core/* → core/model`. core 모듈끼리는 서로 모른다.

## 개발 환경

- Android Studio 최신, JDK 17, Gradle 8.10 (wrapper 포함)
- Android API 28+ (minSdk), compileSdk 35, Kotlin 2.0, Jetpack Compose
- Pleos Connect SDK (Maven `nexus-playground.pleos.ai`): Vehicle 2.3.2, NaviHelper 3.0.5, TextToSpeech 3.3.1, SpeechToText 3.3.1, FusedLocation 1.1.3
- Pleos Connect Emulator 2.1.0 (API 34). **Cold boot 필수**, AVD RAM 4GB
  - Mac(arm64) 시스템 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

## 실행

```bash
./gradlew :app:installDebug          # 에뮬레이터에 디버그 설치
scripts/vc-forward.sh                # Virtual Console → http://127.0.0.1:8088
scripts/driving-state.sh driving     # 주행 규제 ON (parked 로 해제)
./gradlew test                       # 단위 테스트 (core/model, core/vehicle)
TEAM=팀명 APP=앱이름 scripts/install-release.sh   # apk/팀명_앱이름_release.apk 생성·설치
```

## 설계 원칙

- 주행 중에는 음성과 대형 버튼만. 리포트·QR·키보드는 정차(P)에서만.
- 모든 신호에 출처(PLEOS_SDK / AAOS_CAR_API / SIMULATED)를 붙이고, 합성 신호는 화면에 SIMULATED 배지를 띄운다.
- 링버퍼는 실제 확보한 길이만 보고한다. 과거를 복원하거나 보간하지 않는다.
- 차량 권한은 읽기 전용만 선언한다.
- 주행 중 네트워크 호출 없음. 외부 전송은 정차 후 백그라운드.

## 문서

- 공모전 가이드: https://appdevchallenge.auto-hmg.io/guide/overview
- Pleos SDK: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- 샘플: https://github.com/Pleos-Playground
