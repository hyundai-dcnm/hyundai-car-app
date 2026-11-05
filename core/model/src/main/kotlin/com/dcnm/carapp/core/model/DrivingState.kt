package com.dcnm.carapp.core.model

/**
 * 주행/정차 판정 결과. UI 분기와 음성 전용 모드 전환의 단일 기준.
 *
 * 판정 규칙(공식 정책 준용): 차속 <= 5km/h 이고 P단(또는 주차브레이크·오토홀드)이면 [PARKED], 그 외 [DRIVING].
 * CarUxRestrictions 콜백이 있으면 그것을 우선한다.
 */
enum class DrivingState { PARKED, DRIVING, UNKNOWN }
