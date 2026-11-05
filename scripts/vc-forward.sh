#!/usr/bin/env bash
# Pleos Connect 에뮬레이터 Virtual Console 포트 포워딩.
# 실행 후 브라우저에서 http://127.0.0.1:8088 접속.
set -euo pipefail
adb forward tcp:8088 tcp:8088
echo "Virtual Console: http://127.0.0.1:8088"
