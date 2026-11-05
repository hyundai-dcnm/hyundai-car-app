#!/usr/bin/env bash
# release APK 빌드 후 에뮬레이터에 설치. 결과물은 apk/ 에 복사 (공모전 제출 규격: 팀명_앱이름_release.apk).
set -euo pipefail
TEAM="${TEAM:-dcnm}"
APP="${APP:-carapp}"
./gradlew :app:assembleRelease
mkdir -p apk
cp app/build/outputs/apk/release/app-release*.apk "apk/${TEAM}_${APP}_release.apk"
adb install -r "apk/${TEAM}_${APP}_release.apk"
