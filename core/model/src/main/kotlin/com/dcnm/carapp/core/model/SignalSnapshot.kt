package com.dcnm.carapp.core.model

import kotlinx.serialization.Serializable

/**
 * 링버퍼에서 잘라낸 신호 구간. 리포트의 "사건 맥락" 섹션 원본.
 *
 * [coveredMillis]는 실제 확보한 길이다. 요청 길이보다 짧으면 리포트에 그대로 표시한다.
 */
@Serializable
data class SignalSnapshot(
    val anchorMillis: Long,
    val requestedBeforeMillis: Long,
    val requestedAfterMillis: Long,
    val coveredMillis: Long,
    val samples: List<SignalSample>,
)

/** 직렬화 가능한 평탄화 샘플. [VehicleSignal]을 저장용으로 변환한 형태. */
@Serializable
data class SignalSample(
    val epochMillis: Long,
    val type: String,
    val value: String,
    val unit: String,
    val source: SignalSource,
)
