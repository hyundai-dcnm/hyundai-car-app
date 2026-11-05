#!/usr/bin/env bash
# 에뮬레이터 주행 규제 상태 토글 (CarUxRestrictions 콜백 검증용).
# 사용: scripts/driving-state.sh driving | parked
set -euo pipefail
STATE="${1:-driving}"
adb shell cmd hmg_car_service --service drivingstate "$STATE"
echo "driving state -> $STATE"
