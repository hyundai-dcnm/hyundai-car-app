# 차량 프론트 구현 (2026-11-15)

## 11/15 사용자 피드백 반영

- 넓어진 차량 창에서도 1280×800 기준 레일·패널·글자·터치 영역의 비율이 함께 유지되도록 공통 배율을 적용했다. 가로세로를 따로 늘리지 않는다. 실제 차량 창 종횡비 자체는 플랫폼이 결정한다.
- 기본 집 모양 런처 아이콘을 동행의 두 갈래 도로 심볼로 변경했다. 원형 마스크와 Android 13 테마 아이콘을 제공한다.
- 실지도 키가 없을 때는 차량 마커/경로가 없는 Figma 예시 지형을 배경으로 사용하고 ‘예시 지도 · 실지도 연결 전 · 위치 표시 없음’을 고정 표시한다. 실지도 연결이 완료된 것으로 표시하지 않는다.
- 실제 모드 목적지는 차량 NaviSource 목적지 가져오기, 이름/좌표 직접 등록, 최근 기록 검색, 집/회사 등록을 지원한다. 외부 장소 검색은 미연동이다. 최근 6개/즐겨찾기는 Android Keystore AES-GCM으로 기기에 암호화 저장하며 지우기 버튼을 제공한다.
- 실제 앨범에 사진 선택→미리보기→확인 업로드·목록·썸네일·확대·삭제 확인을 연결했다. 정차/동행 참여가 필요하다. 이미지 원본은 최대 10MB, 긴 변 1,600px 이하 JPEG로 변환하면서 회전 방향을 반영하고 EXIF 위치 정보를 제거한다. 썸네일은 화면에 보이는 목록 위주로 불러오며 토큰은 Authorization 헤더로만 보낸다.
- 사용자 선택에 따라 프론트 범위를 유지했다. 서버/공통 계약/core/feature 엔진을 변경하지 않았다. 친구/주변/음악/추천은 기존 미연동·시연 범위를 유지한다.

이후 내용 중 최초 구현 시점의 한계는 위 후속 구현과 구분한다. 최신 검증은 상위 프로젝트 `docs/동행주행_프론트보완_결과.md`에 기록한다.

기준은 Figma `ZJNXfClr96V8VBi1dbaQgZ`의 최신 02(78:1327), 홈(78:1332), 주행(78:1451)이다. Kotlin/Compose 앱의 진입점을 `VehicleApp`으로 연결했다. 기존 `feature:room`, `feature:trip` 화면과 `core`의 지도·위치·내비·통화·공통 계약은 보존했다. 방/통화 UI와 서버를 연결하는 코드는 `app/.../vehicle`에 있다.

## 실행

- Java 21, Android SDK 35, 기존 Gradle 8.10.2/AGP 8.8.2/Kotlin 2.0.21 사용. minSdk 28 유지.
- `gradlew.bat :app:assembleDebug`. 한글 Windows 경로에서 빌드할 수 있도록 `android.overridePathCheck=true`를 명시했다.
- 이 PC에서는 JVM 단위 테스트가 한글 경로의 클래스를 찾지 못해 동일 저장소를 임시 `R:` 경로에 연결한 뒤 검사했다. 파일 이동/복사는 하지 않았다. 영문 경로의 checkout에서도 실행 가능하다.
- 서버: `https://donghaeng-server.onrender.com`. 외부 주소나 토큰 입력은 앱에 노출하지 않는다.
- 실제 지도는 기존 `core:map` 설정인 `local.properties`의 `NAVER_MAP_CLIENT_ID`가 필요하다. 검증 PC에 키가 없어 실지도 인증·표시 시험은 미수행이다. 앱에는 준비 중 안내를 표시한다. 임의 키를 만들거나 커밋하지 않았다.
- 정차하여 플랫폼 UX 제한이 해제되면 왼쪽 설정 → **디자인 시연 시작**. 상단에 시연 배지를 유지하며 방·친구·주변 찾기·통화·음악을 서버/SDK 동작과 분리해 확인할 수 있다. 시연 모드에서도 실제 차량 UX 제한은 유지된다.
- APK는 `app/build/outputs/apk/debug/app-debug.apk`. 개발/검증용 debug 서명이며 제출용 release 서명이 아니다.

## 화면과 데이터 경계

| 범위 | 구현/연결 |
|---|---|
| S01–S10 홈·방·초대·목적지·공유 | 실제 방 생성/참여/조회·제안/수락, 위치 동의 UI. 장소 검색은 연결 전이며 3개 예시 장소의 좌표를 명시적으로 선택한다. 친구 목록/수락은 시연 전용이다. |
| S11–S15 지도·내비·통화 | 기존 TripMap/LocationSource/NaviSource/CallClient 재사용. 서버 위치를 필터링해 지도에 전달. 내비 요청과 실제 반영 상태 구분. 통화는 토큰과 실제 CallState를 사용한다. 시연은 음성/위치를 보내지 않는다. |
| S16–S20 음악·공유 중지·퇴장 | 음악 요청/대기는 시연 전용이며 재생 없음. 실제 위치 공유와 내 통화·방 퇴장은 각각 분리한다. 서버 반영 실패 시 이 기기의 위치 전송은 우선 중단하고 재시도를 안내한다. |
| S21–S30 주변 찾기 | API가 없어 실제 모드는 OFF/준비 중. 시연에서만 ON·목적지 일치·반경 필터·양쪽 수락·취소·목적지 변경 시 OFF·차단·만료를 구현했다. 반경 100/500/1,000 m는 설계 예시다. |
| E01–E10 예외 | 실제 연결/권한/내비/위치 상태와 시연 예외 진입을 분리했다. 통신 실패를 주변 0명으로 표시하지 않는다. 제한 화면은 입력·검색·설정 화면을 닫는다. |
| 휴식 추천·여행 앨범 | 설정에서 여는 추가 화면 시연. 실제 장소 추천·사진 조회/업로드는 미연결이다. 서버 사진 API는 존재하며 별도 연결 작업이 필요하다. |

실제 서버 토큰은 메모리에만 보관한다. 앱 프로세스 종료 후 재참여가 필요하며 자동 세션 복구는 구현하지 않았다. 서버 공유 기본값이 true이므로 방 생성/참여 직후 PUT sharing=false를 확인한 뒤 화면에 방을 노출한다. 이 처리 실패 시 시험적으로 방을 유지하지 않고 해당 참가자의 퇴장을 시도한다. 위치 송신은 별도의 이 기기 동의가 있어야 하며 전경·권한·최신 위치·WSS 연결을 모두 확인한다.

WSS 재접속은 최대 5회(1/2/4/8/16초) 후 수동 재시도로 전환하며 재접속 snapshot을 사용한다. 토큰 만료/정상 방 종료는 자동 재접속하지 않는다. REST 쓰기 요청은 자동 재시도하지 않아 방 중복 생성 위험을 줄였다. health 확인만 최대 2회 한다. 위치 송신은 2초 간격, 상대 위치는 계약의 2분 유효기간 및 공유 여부로 필터링한다. SDK 위치 timestamp의 실제 시각 기준과 장시간 백그라운드 정책은 추가 확인이 필요하다.

## 검증 및 한계

- Pleos Connect 2.1.0 / Android API 34 / 2560×1440, 밀도 160. 차량 시스템 UI가 좌우 공간을 차지해 앱 창은 디자인 기준 1280×800과 다르며 native dp 배치·스크롤로 대응했다.
- 단위 테스트: 앱 7개(동의·공유 중지 실패·HTTP/WSS·주변 필터/수락·제한), 기존 core:trip 4개·core:navi 5개·feature:trip 5개 통과.
- Pleos 계측: 시연 홈→주변 탐색→상호 수락→방→목적지→공유 없이 주행→통화/음소거→음악 대기, 입력 중 제한/Unknown 전환, 실제 Android 서버 어댑터의 격리 방 생성·WSS snapshot·목적지 합의·통화 토큰·퇴장 확인.
- 별도 합성 방에서 REST 2대 참가·WSS snapshot·위치 공유 OFF·공동 목적지 합의·WSS 통화 토큰을 확인했고 두 시험 참가자는 퇴장했다. 토큰/실제 위치/음성은 결과 파일에 저장하지 않았다.
- `lintDebug` 오류 0. 기존 의존성 버전/target API 및 TOML 이동 등의 경고는 별도 남는다. 업그레이드는 이번 범위에 넣지 않았다.
- Windows 창 캡처는 `window crop is outside captured monitor` 및 `failed to activate captured window`로 실패했다. Android 계측/ADB 화면으로 대체했다.
- 실제 플랫폼 정차 UX 제한 해제 수신을 확인했다. Restricted/Unknown 전환 시험은 계측에서 UI 입력으로 주입한 상태이며 실제 D 기어 전환·실차 시험이 아니다.
- 실제 통화 오디오 송수신/백그라운드 서비스 지속, 위치 권한 팝업/실좌표 송신, 실지도 인증, 이번 UI에서 내비 경로 반영·실차 안전성은 미검증이다.
- Figma의 시연 속도/ETA/곡/지형은 실데이터가 아니다. 픽셀 단위 동일성을 보장하지 않으며 고정 예시 즐겨찾기 등 미확정 기능은 실제 기능으로 추가하지 않았다.

## 서버/팀 계약 요구

주변 탐색·목적지 장소 ID 정규화·요청/친구/차단·수락 유효기간·거리/위치 유효 정책, 음악 공급/요청/재생 상태 API가 필요하다. 기존 서버·DB·공통 API 계약·배포는 수정하지 않았다. 기존 통화/내비/지도 엔진도 수정하지 않았다. 현재 방 프로토콜의 WS 토큰 query 사용은 기존 계약이며, 공식 보안 가이드의 URL 내 민감정보 지양과 충돌하는 점을 후속 계약 개선 대상으로 기록한다. 이번 프론트에서 계약을 임의 변경하지 않았다.

## 확인한 공식 출처

2026-11-15 열람. 공모전 참가와 Pleos App Market 심사는 별도 절차이며 이 구현이 심사 통과를 보장하지 않는다.

- [공모전 개발 환경](https://appdevchallenge.auto-hmg.io/guide/development): API 28 이상, Gradle 8.0 이상, Kotlin/Java.
- [참가 규정](https://appdevchallenge.auto-hmg.io/guide/rules), [지원·제출](https://appdevchallenge.auto-hmg.io/guide/submission): 제출은 수행하지 않았다. 개발 APK와 제출용 release APK를 구분한다.
- [Pleos 앱 심사](https://document.pleos.ai/en/docs/pleos-only/distribute/app-submission-guide): 작동 검증·개인정보/위치/권한·제출 정보.
- [주의 분산 설계](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline), [제한 개발 가이드](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide): CarUxRestrictions 구독, 입력/검색/설정 제한, 큰 터치 영역·대비.
- [AAOS UX 상태 소비](https://source.android.com/docs/automotive/driver_distraction/consume): 직접 속도 0을 정차로 간주하지 않고 UX 제한 관리자를 사용한다.
- [Pleos 하드웨어 접근](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture), [보안](https://document.pleos.ai/en/docs/pleos-only/app-registration-guide/app-security-guide): 공식 API·최소 권한·TLS·비밀값/로그 관리.

## 시각 자산과 라이선스

`app/src/main/assets/automotive/*.svg`는 get_design_context가 제공한 최신 Figma 벡터의 원본 다운로드다. 앱은 임시 Figma URL 없이 로컬 자산을 AndroidSVG로 렌더링하며 root 종횡비를 유지한다. 지도/경로/커버/로고는 프로젝트의 Figma 생성 스크립트에서 만든 합성 그래픽이다. 정확한 지리 데이터나 음원 커버로 사용하지 않는다. 원본 캔버스의 일반 Material 3 버튼/아이콘 버튼은 Compose Material 3 컴포넌트로 대응했다.

- 프로젝트 제작 벡터: 원본 Figma와 상위 프로젝트 `outputs/figma/redesign-foundation.js`, `redesign-screens.js`의 출처 확인. 외부 사진/지도 타일/음원은 다운로드하지 않았다. 프로젝트 전체에 대한 새 배포 라이선스를 임의 부여하지 않았다.
- AndroidSVG 1.4: [Apache License 2.0](https://github.com/BigBadaboom/androidsvg/blob/master/LICENSE).
- OkHttp 4.12.0, Gson 2.11.0, AndroidX Compose/Material 3: Apache License 2.0. 기존 SDK/네이버 지도 서비스의 별도 이용조건과 키 등록은 유지된다.
- 폰트 파일은 새로 복제하지 않았고 Android 시스템 글꼴을 사용한다. Figma의 Noto Sans KR/Roboto와 기기별 메트릭 차이가 있을 수 있다.
