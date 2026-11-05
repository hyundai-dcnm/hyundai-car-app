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
app/                  (예정) Pleos Connect 차량 앱 (Kotlin, Jetpack Compose)
web/                  (예정) 웹 리포트
apk/                  (예정) 팀명_앱이름_release.apk
```

## 개발 환경

- Android API 28+, Gradle 8+, Kotlin
- Pleos Connect SDK: Vehicle, NaviHelper, FusedLocation, TTS/STT
- Pleos Connect Emulator 2.1.0 (API 34, Cold boot 필수, AVD RAM 4GB)
  - Mac(arm64) 시스템 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
- 문서: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- 샘플: https://github.com/Pleos-Playground
- 공모전 가이드: https://appdevchallenge.auto-hmg.io/guide/overview
