#!/usr/bin/env bash
# 에뮬레이터 주행 상태 토글 (CarUxRestrictions 콜백 검증용).
# Pleos 에뮬레이터는 user 빌드라 `cmd car_service inject-vhal-event`가 막혀 있어 Virtual Console HTTP API를 쓴다.
# Virtual Console은 브라우저 세션 하트비트가 있어야 명령을 받으므로 몇 초간 하트비트를 함께 보낸다.
# 사용: scripts/driving-state.sh driving [kmh] | parked
set -euo pipefail
STATE="${1:-driving}"
KMH="${2:-30}"
VC=http://127.0.0.1:8088

adb forward tcp:8088 tcp:8088 >/dev/null
( for _ in 1 2 3 4 5 6 7 8; do curl -s -m2 -X POST "$VC/browser/session?client=driving-state-sh" >/dev/null; sleep 1; done ) &
HB=$!
sleep 1.5

case "$STATE" in
  driving)
    curl -s -m5 -X POST "$VC/vehicle/gear?gear=Drive" >/dev/null
    curl -s -m5 -X POST "$VC/vehicle/controls?speed_kph=$KMH" >/dev/null ;;
  parked)
    curl -s -m5 -X POST "$VC/vehicle/controls?speed_kph=0" >/dev/null
    curl -s -m5 -X POST "$VC/vehicle/gear?gear=Park" >/dev/null ;;
  *) echo "사용: $0 driving [kmh] | parked" >&2; kill $HB; exit 1 ;;
esac

sleep 2
adb shell dumpsys car_service 2>/dev/null | grep -m1 "UXR: DO" || true
wait $HB
echo "driving state -> $STATE"
