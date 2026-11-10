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

## 개발 환경

- Pleos Connect SDK (Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`)
- Pleos Connect Emulator 2.1.0 (API 34). Cold boot 필수, AVD RAM 4GB
  - Mac(arm64) 시스템 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
  - Windows/Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`

## 문서

- 공모전 가이드: https://appdevchallenge.auto-hmg.io/guide/overview
- Pleos SDK: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- 샘플: https://github.com/Pleos-Playground
