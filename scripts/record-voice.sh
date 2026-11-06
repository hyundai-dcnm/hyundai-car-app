#!/usr/bin/env bash
# 음성 호출 시연: 순정 내비 화면이 앞에 있는 상태에서 "상황 기록"을 보낸다.
# 음성 비서·웨이크워드 연동은 검증 전이라(docs/02) 지금은 이 인텐트가 호출 진입점이다.
# 앱을 한 번 실행해 둬야 CaptureService가 떠 있다.
# 사용: scripts/record-voice.sh ["말한 문장"]   (문장을 빼면 STT를 연다)
set -euo pipefail
PKG="${PKG:-com.dcnm.carapp.debug}"
SAID="${1:-}"
args=(-a com.dcnm.carapp.action.RECORD_SYMPTOM -n "$PKG/com.dcnm.carapp.capture.CaptureService")
# adb shell은 인자를 다시 셸로 해석하므로 문장을 작은따옴표로 한 번 더 감싼다.
[[ -n "$SAID" ]] && args+=(--es utterance "'${SAID//\'/}'")
adb shell am start-foreground-service "${args[@]}"
echo "기록 요청 보냄. 약 5초 뒤 알림음과 상단 알림이 떠야 한다."
